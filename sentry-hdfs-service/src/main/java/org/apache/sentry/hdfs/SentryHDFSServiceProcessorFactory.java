package org.apache.sentry.hdfs;

import java.net.Socket;

import org.apache.hadoop.conf.Configuration;
import org.apache.sentry.hdfs.service.thrift.SentryHDFSService;
import org.apache.sentry.hdfs.service.thrift.SentryHDFSService.Iface;
import org.apache.sentry.provider.db.log.util.CommandUtil;
import org.apache.sentry.service.thrift.ProcessorFactory;
import org.apache.thrift.TException;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSaslClientTransport;
import org.apache.thrift.transport.TSaslServerTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SentryHDFSServiceProcessorFactory extends ProcessorFactory{

  private static final Logger LOGGER = LoggerFactory.getLogger(SentryHDFSServiceProcessorFactory.class);

  static class ProcessorWrapper extends SentryHDFSService.Processor<SentryHDFSService.Iface> {

    public ProcessorWrapper(Iface iface) {
      super(iface);
    }
    @Override
    public boolean process(TProtocol in, TProtocol out) throws TException {
      setIpAddress(in);
      setImpersonator(in);
      return super.process(in, out);
    }

    private void setImpersonator(final TProtocol in) {
      TTransport transport = in.getTransport();
      if (transport instanceof TSaslServerTransport) {
        String impersonator = ((TSaslServerTransport) transport).getSaslServer().getAuthorizationID();
        CommandUtil.setImpersonator(impersonator);
      }
    }

    private void setIpAddress(final TProtocol in) {
      TTransport transport = in.getTransport();
      TSocket tSocket = getUnderlyingSocketFromTransport(transport);
      if (tSocket != null) {
        setIpAddress(tSocket.getSocket());
      } else {
        LOGGER.warn("Unknown Transport, cannot determine ipAddress");
      }
    }

    private void setIpAddress(Socket socket) {
      CommandUtil.setIpAddress(socket.getInetAddress().toString());
    }

    private TSocket getUnderlyingSocketFromTransport(TTransport transport) {
      if (transport != null) {
        if (transport instanceof TSaslServerTransport) {
          transport = ((TSaslServerTransport) transport).getUnderlyingTransport();
        } else if (transport instanceof TSaslClientTransport) {
          transport = ((TSaslClientTransport) transport).getUnderlyingTransport();
        } else if (transport instanceof TSocket) {
          return (TSocket) transport;
        }
      }
      return null;
    }
  }

  public SentryHDFSServiceProcessorFactory(Configuration conf) {
    super(conf);
  }


  public boolean register(TMultiplexedProcessor multiplexedProcessor) throws Exception {
    SentryHDFSServiceProcessor sentryServiceHandler =
        new SentryHDFSServiceProcessor();
    TProcessor processor = new ProcessorWrapper(sentryServiceHandler);
    multiplexedProcessor.registerProcessor(
        SentryHDFSServiceProcessor.SENTRY_HDFS_SERVICE_NAME, processor);
    return true;
  }
}
