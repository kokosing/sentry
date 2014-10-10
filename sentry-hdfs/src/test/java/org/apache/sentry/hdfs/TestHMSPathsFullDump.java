/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sentry.hdfs;

import junit.framework.Assert;

import org.apache.sentry.hdfs.service.thrift.TPathsDump;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TCompactProtocol;
import org.junit.Test;

import com.google.common.collect.Lists;

public class TestHMSPathsFullDump {

  @Test
  public void testDumpAndInitialize() {
    HMSPaths hmsPaths = new HMSPaths(new String[] {"/user/hive/warehouse"});
    hmsPaths._addAuthzObject("db1", Lists.newArrayList("/user/hive/warehouse/db1"));
    hmsPaths._addAuthzObject("db1.tbl11", Lists.newArrayList("/user/hive/warehouse/db1/tbl11"));
    hmsPaths._addPathsToAuthzObject("db1.tbl11", Lists.newArrayList(
        "/user/hive/warehouse/db1/tbl11/part111",
        "/user/hive/warehouse/db1/tbl11/part112",
        "/user/hive/warehouse/db1/tbl11/p1=1/p2=x"));
    
    Assert.assertEquals("db1", hmsPaths.findAuthzObject(new String[]{"user", "hive", "warehouse", "db1"}, false));
    Assert.assertEquals("db1.tbl11", hmsPaths.findAuthzObject(new String[]{"user", "hive", "warehouse", "db1", "tbl11"}, false));
    Assert.assertEquals("db1.tbl11", hmsPaths.findAuthzObject(new String[]{"user", "hive", "warehouse", "db1", "tbl11", "part111"}, false));
    Assert.assertEquals("db1.tbl11", hmsPaths.findAuthzObject(new String[]{"user", "hive", "warehouse", "db1", "tbl11", "part112"}, false));

    Assert.assertEquals("db1.tbl11", hmsPaths.findAuthzObject(new String[]{"user", "hive", "warehouse", "db1", "tbl11", "p1=1", "p2=x"}, false));
    Assert.assertEquals("db1.tbl11", hmsPaths.findAuthzObject(new String[]{"user", "hive", "warehouse", "db1", "tbl11", "p1=1"}, true));

    HMSPathsSerDe serDe = hmsPaths.getPathsDump();
    TPathsDump pathsDump = serDe.createPathsDump();
    HMSPaths hmsPaths2 = serDe.initializeFromDump(pathsDump);

    Assert.assertEquals("db1", hmsPaths2.findAuthzObject(new String[]{"user", "hive", "warehouse", "db1"}, false));
    Assert.assertEquals("db1.tbl11", hmsPaths2.findAuthzObject(new String[]{"user", "hive", "warehouse", "db1", "tbl11"}, false));
    Assert.assertEquals("db1.tbl11", hmsPaths2.findAuthzObject(new String[]{"user", "hive", "warehouse", "db1", "tbl11", "part111"}, false));
    Assert.assertEquals("db1.tbl11", hmsPaths2.findAuthzObject(new String[]{"user", "hive", "warehouse", "db1", "tbl11", "part112"}, false));
  }

  @Test
  public void testThrftSerialization() throws TException {
    HMSPaths hmsPaths = new HMSPaths(new String[] {"/"});
    String prefix = "/user/hive/warehouse/";
    for (int dbNum = 0; dbNum < 1; dbNum++) {
      String dbName = "db" + dbNum;
      hmsPaths._addAuthzObject(dbName, Lists.newArrayList(prefix + dbName));
      for (int tblNum = 0; tblNum < 1000000; tblNum++) {
        String tblName = "tbl" + tblNum;
        hmsPaths._addAuthzObject(dbName + "." + tblName, Lists.newArrayList(prefix + dbName + "/" + tblName));
        for (int partNum = 0; partNum < 1; partNum++) {
          String partName = "part" + partNum;
          hmsPaths
              ._addPathsToAuthzObject(
                  dbName + "." + tblName,
                  Lists.newArrayList(prefix + dbName + "/" + tblName + "/"
                      + partName));
        }
      }
    }
    HMSPathsSerDe serDe = hmsPaths.getPathsDump();
    long t1 = System.currentTimeMillis();
    TPathsDump pathsDump = serDe.createPathsDump();
    byte[] ser = new TSerializer(new TCompactProtocol.Factory()).serialize(pathsDump);
    long serTime = System.currentTimeMillis() - t1;
    System.out.println("Serialization Time: " + serTime + ", " + ser.length);

    t1 = System.currentTimeMillis();
    TPathsDump tPathsDump = new TPathsDump();
    new TDeserializer(new TCompactProtocol.Factory()).deserialize(tPathsDump, ser);
    HMSPaths fromDump = serDe.initializeFromDump(tPathsDump);
    System.out.println("Deserialization Time: " + (System.currentTimeMillis() - t1));
    Assert.assertEquals("db9.tbl999", fromDump.findAuthzObject(new String[]{"user", "hive", "warehouse", "db0", "tbl999"}, false));
    Assert.assertEquals("db9.tbl999", fromDump.findAuthzObject(new String[]{"user", "hive", "warehouse", "db0", "tbl999", "part5"}, false));
  }

}
