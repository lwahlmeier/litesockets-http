package org.threadly.litesockets.client.http;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.threadly.concurrent.PriorityScheduler;
import org.threadly.litesockets.Client;
import org.threadly.litesockets.Server.ClientAcceptor;
import org.threadly.litesockets.ThreadedSocketExecuter;
import org.threadly.litesockets.TCPClient;
import org.threadly.litesockets.TCPServer;
import org.threadly.litesockets.utils.SSLUtils.FullTrustManager;
import org.threadly.litesockets.utils.MergedByteBuffers;
import org.threadly.litesockets.utils.TransactionalByteBuffers;

public class FakeHTTPServer implements ClientAcceptor, Client.Reader{
  TrustManager[] myTMs = new TrustManager [] {new FullTrustManager() };
  KeyStore KS;
  KeyManagerFactory kmf;
  SSLContext sslCtx;


  PriorityScheduler PS;
  ThreadedSocketExecuter SEB;
  int port;
  TCPServer server;
  TransactionalByteBuffers sendBack = new TransactionalByteBuffers();
  boolean closeOnSend;
  boolean doSSL;

  public FakeHTTPServer(int port, String sendBack, boolean doSSL, boolean closeOnSend) throws IOException {
    this.doSSL = doSSL;
    this.port = port;
    PS = new PriorityScheduler(5);
    SEB = new ThreadedSocketExecuter(PS);
    SEB.start();
    server = SEB.createTCPServer("localhost", port);
    if(doSSL) {
      doSSLCrap();
      server.setSSLContext(sslCtx);
      server.setDoHandshake(true);
    } 
    server.setClientAcceptor(this);
    server.start();
    this.sendBack.add(ByteBuffer.wrap(sendBack.getBytes()));
    this.closeOnSend = closeOnSend;
  }

  private void doSSLCrap() {
    try {
      KS = KeyStore.getInstance(KeyStore.getDefaultType());
      String filename = ClassLoader.getSystemClassLoader().getResource("keystore.jks").getFile();
      FileInputStream ksf = new FileInputStream(filename);
      KS.load(ksf, "password".toCharArray());
      kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(KS, "password".toCharArray());
      sslCtx = SSLContext.getInstance("TLS");
      sslCtx.init(kmf.getKeyManagers(), myTMs, null);
    } catch(Exception e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public void accept(Client c) {
    TCPClient tc = (TCPClient)c;
    System.out.println(c);
    tc.setReader(this);
  }

  @Override
  public void onRead(Client client) {
    MergedByteBuffers mbb = new MergedByteBuffers();
    mbb.add(client.getRead());
    if(mbb.indexOf("\r\n\r\n") >= 0) {
      //System.out.println(mbb.getAsString(mbb.remaining()));
      sendBack.begin();
      while(sendBack.remaining() > 0 && !client.isClosed()) {
        client.write(sendBack.pull(Math.min(500, sendBack.remaining())));
      }
      sendBack.rollback();
      if(closeOnSend) {
        while(client.getWriteBufferSize() > 0) {
          try {
            Thread.sleep(10);
          } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
        client.close();
      }
    }
  }

  public void stop() {
    server.close();
    SEB.stopIfRunning();
    PS.shutdownNow();
  }

}
