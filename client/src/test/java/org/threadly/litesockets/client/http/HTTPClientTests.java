package org.threadly.litesockets.client.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.threadly.concurrent.PriorityScheduler;
import org.threadly.concurrent.future.FutureCallback;
import org.threadly.concurrent.future.ListenableFuture;
import org.threadly.litesockets.Client;
import org.threadly.litesockets.Server.ClientAcceptor;
import org.threadly.litesockets.SocketExecuter;
import org.threadly.litesockets.TCPServer;
import org.threadly.litesockets.ThreadedSocketExecuter;
import org.threadly.litesockets.client.http.HTTPClient.HTTPResponseData;
import org.threadly.litesockets.protocols.http.request.HTTPRequest;
import org.threadly.litesockets.protocols.http.request.HTTPRequestBuilder;
import org.threadly.litesockets.protocols.http.shared.HTTPAddress;
import org.threadly.litesockets.protocols.http.shared.HTTPParsingException;
import org.threadly.test.concurrent.TestCondition;
import org.threadly.util.Clock;

public class HTTPClientTests {
  static String CONTENT = "TEST123";
  static String RESPONSE_TEMPLATE = "HTTP/1.1 200 OK\r\nContent-Length: {LENGTH}\r\nContent-Type: text/html\r\n\r\n{BODY}";
  static String RESPONSE1 = RESPONSE_TEMPLATE.replace("{BODY}", CONTENT).replace("{LENGTH}", Integer.toString(CONTENT.length()));
  static String RESPONSE_PARTIAL = RESPONSE_TEMPLATE.replace("{BODY}", CONTENT).replace("{LENGTH}", Integer.toString(CONTENT.length()*3));
  static String RESPONSE_ERROR_1 = "\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n";
  static String RESPONSE_HUGE;
  static String RESPONSE_HUGE_NOCL;
  static String RESPONSE_NOCL_NOBODY = RESPONSE_TEMPLATE.replace("Content-Length: {LENGTH}\r\n", "").replace("{BODY}", "");
  static String RESPONSE_NOCL_WITH = RESPONSE_NOCL_NOBODY+CONTENT;
  static {
    StringBuilder sb = new StringBuilder();
    while(sb.length() < (HTTPClient.MAX_HTTP_RESPONSE*2)) {
      sb.append(CONTENT);
    }
    RESPONSE_HUGE = RESPONSE_TEMPLATE.replace("{LENGTH}", Integer.toString(sb.length())).replace("{BODY}", sb.toString());
    RESPONSE_HUGE_NOCL = RESPONSE_TEMPLATE.replace("Content-Length: {LENGTH}\r\n", "").replace("{BODY}", sb.toString());
  }

  SocketExecuter SEI;
  PriorityScheduler PS;
  FakeHTTPServer fakeServer;

  @Before
  public void start() {
    PS = new PriorityScheduler(5);
    SEI = new ThreadedSocketExecuter(PS);
    SEI.start();
  }

  @After
  public void stop() {
    SEI.stop();
    PS.shutdownNow();
    if(fakeServer != null) {
      fakeServer.stop();
    }
  }

  @Test
  public void manyRequestsConcurrent() throws IOException {
    final int number = 500;
    final int port = TestUtils.findTCPPort();
    fakeServer = new FakeHTTPServer(port, RESPONSE1, false, false);
    final HTTPRequestBuilder hrb = new HTTPRequestBuilder(new URL("http://localhost:"+port));
    final HTTPClient httpClient = new HTTPClient();
    final AtomicInteger count = new AtomicInteger(0);

    PriorityScheduler CLIENT_PS = new PriorityScheduler(20);
    Runnable run = new Runnable() {
      @Override
      public void run() {
        HTTPRequest hr = hrb.build();
        //final long start = Clock.accurateForwardProgressingMillis();
        final ListenableFuture<HTTPResponseData>  lf = httpClient.requestAsync(new HTTPAddress("localhost", port, false), hr);
        lf.addCallback(new FutureCallback<HTTPResponseData>() {
          @Override
          public void handleResult(HTTPResponseData result) {
            //System.out.println("DELAY:"+(Clock.accurateForwardProgressingMillis()-start));
            assertEquals("TEST123", result.getBodyAsString());
            count.incrementAndGet();
          }

          @Override
          public void handleFailure(Throwable t) {
            System.out.println("***********************ERR*******************");
            try {
              System.out.println(lf.get().getBodyAsString());
            } catch (InterruptedException e) {
              e.printStackTrace();
            } catch (ExecutionException e) {
              e.printStackTrace();
            }
            System.out.println("***********************ERR*******************");
            fail();
          }});


      }};

      for(int i=0; i<number; i++) {
        CLIENT_PS.execute(run);
      }
      new TestCondition(){
        @Override
        public boolean get() {
          return count.get() == number;
        }
      }.blockTillTrue(10000);
      httpClient.stop();
  }

  @Test
  public void manyRequestsConcurrentJavaExecutor() throws IOException, InterruptedException {
    final int number = 500;
    final int port = TestUtils.findTCPPort();
    final ThreadedSocketExecuter TSE = new ThreadedSocketExecuter(PS);
    TSE.start();
    fakeServer = new FakeHTTPServer(port, RESPONSE1, false, false);
    final HTTPRequestBuilder hrb = new HTTPRequestBuilder(new URL("http://localhost:"+port));
    final HTTPClient httpClient = new HTTPClient(HTTPClient.DEFAULT_CONCURRENT, HTTPClient.MAX_HTTP_RESPONSE, TSE);
    final AtomicInteger count = new AtomicInteger(0);

    PriorityScheduler CLIENT_PS = new PriorityScheduler(200);
    Runnable run = new Runnable() {
      @Override
      public void run() {
        HTTPRequest hr = hrb.build();
        //final long start = Clock.accurateForwardProgressingMillis();
        final ListenableFuture<HTTPResponseData>  lf = httpClient.requestAsync(new HTTPAddress("localhost", port, false), hr);
        lf.addCallback(new FutureCallback<HTTPResponseData>() {
          @Override
          public void handleResult(HTTPResponseData result) {
            //System.out.println("DELAY:"+(Clock.accurateForwardProgressingMillis()-start));
            assertEquals("TEST123", result.getBodyAsString());
            count.incrementAndGet();
          }

          @Override
          public void handleFailure(Throwable t) {
            System.out.println("***********************ERR*******************");
            try {
              System.out.println(lf.get().getBodyAsString());
            }  catch (InterruptedException e) {
              e.printStackTrace();
            } catch (ExecutionException e) {
              e.printStackTrace();
            }
            System.out.println("***********************ERR*******************");
            fail();
          }});


      }};

      for(int i=0; i<number; i++) {
        CLIENT_PS.execute(run);
      }
      Thread.sleep(1000);
      System.out.println(count.get());
      new TestCondition(){
        @Override
        public boolean get() {
          return count.get() == number;
        }
      }.blockTillTrue(10000);
      httpClient.stop();
      TSE.stop();
  }


  @Test
  public void manyRequestsConcurrentOnPool() throws IOException {
    final int number = 500;
    final int port = TestUtils.findTCPPort();
    fakeServer = new FakeHTTPServer(port, RESPONSE1, false, false);
    final HTTPRequestBuilder hrb = new HTTPRequestBuilder(new URL("http://localhost:"+port));
    final ThreadedSocketExecuter TSE = new ThreadedSocketExecuter(PS);
    TSE.start();
    final HTTPClient httpClient = new HTTPClient(200, HTTPClient.MAX_HTTP_RESPONSE, TSE);
    final AtomicInteger count = new AtomicInteger(0);

    PriorityScheduler CLIENT_PS = new PriorityScheduler(200);
    Runnable run = new Runnable() {
      @Override
      public void run() {
        HTTPRequest hr = hrb.build();
        //final long start = Clock.accurateForwardProgressingMillis();

        final ListenableFuture<HTTPResponseData>  lf = httpClient.requestAsync(new HTTPAddress("localhost", port, false), hr);
        lf.addCallback(new FutureCallback<HTTPResponseData>() {
          @Override
          public void handleResult(HTTPResponseData result) {
            //System.out.println("DELAY:"+(Clock.accurateForwardProgressingMillis()-start));
            assertEquals("TEST123", result.getBodyAsString());
            count.incrementAndGet();
          }

          @Override
          public void handleFailure(Throwable t) {
            System.out.println("***********************ERR*******************");
            try {
              System.out.println(lf.get().getBodyAsString());
            }  catch (InterruptedException e) {
              e.printStackTrace();
            } catch (ExecutionException e) {
              e.printStackTrace();
            }
            System.out.println("***********************ERR*******************");
            fail();
          }});


      }};

      for(int i=0; i<number; i++) {
        CLIENT_PS.execute(run);
      }
      new TestCondition(){
        @Override
        public boolean get() {
          return count.get() == number;
        }
      }.blockTillTrue(10000);
      httpClient.stop();
      TSE.stop();
  }

  @Test
  public void blockingRequest() throws IOException, HTTPParsingException {
    int port = TestUtils.findTCPPort();
    fakeServer = new FakeHTTPServer(port, RESPONSE1, false, true);
    final HTTPRequestBuilder hrb = new HTTPRequestBuilder(new URL("http://localhost:"+port));
    final HTTPClient httpClient = new HTTPClient();
    assertEquals("TEST123", httpClient.request(new HTTPAddress("localhost", port, false), hrb.build()).getBodyAsString());
  }

  @Test
  public void noContentLengthNoBody() throws IOException, HTTPParsingException {
    int port = TestUtils.findTCPPort();
    fakeServer = new FakeHTTPServer(port, RESPONSE_NOCL_NOBODY, false, true);
    final HTTPRequestBuilder hrb = new HTTPRequestBuilder(new URL("http://localhost:"+port));
    final HTTPClient httpClient = new HTTPClient();
    HTTPResponseData hrs = httpClient.request(new HTTPAddress("localhost", port, false), hrb.build());
    assertEquals("", hrs.getBodyAsString());
  }

  @Test
  public void noContentLengthWithBody() throws IOException, HTTPParsingException {
    int port = TestUtils.findTCPPort();
    fakeServer = new FakeHTTPServer(port, RESPONSE_NOCL_WITH, false, true);
    final HTTPRequestBuilder hrb = new HTTPRequestBuilder(new URL("http://localhost:"+port));
    final HTTPClient httpClient = new HTTPClient();
    HTTPResponseData hrs = httpClient.request(new HTTPAddress("localhost", port, false), hrb.build());
    assertEquals("TEST123", hrs.getBodyAsString());
  }

  @Test
  public void closeBeforeLength() throws IOException, HTTPParsingException {
    int port = TestUtils.findTCPPort();
    fakeServer = new FakeHTTPServer(port, RESPONSE_PARTIAL, false, true);
    final HTTPRequestBuilder hrb = new HTTPRequestBuilder(new URL("http://localhost:"+port));
    final HTTPClient httpClient = new HTTPClient();
    try{
      httpClient.request(new HTTPAddress("localhost", port, false), hrb.build());
      fail();
    } catch(HTTPParsingException e) {
      assertEquals("Did not get complete body!", e.getMessage());
    }
  }

  @Test
  public void expireRequest() throws IOException, HTTPParsingException {
    int port = TestUtils.findTCPPort();
    fakeServer = new FakeHTTPServer(port, "", false, false);
    final HTTPRequestBuilder hrb = new HTTPRequestBuilder(new URL("http://localhost:"+port));
    final HTTPClient httpClient = new HTTPClient();
    long start = Clock.accurateForwardProgressingMillis();
    try {
      httpClient.request(new HTTPAddress("localhost", port, false), hrb.build(), HTTPClient.EMPTY_BUFFER, TimeUnit.MILLISECONDS, 300);
      fail();
    } catch(HTTPParsingException hp) {

    }
    assertTrue((Clock.accurateForwardProgressingMillis() - start) > 300);
  }

  @Test
  public void sslRequest() throws IOException, HTTPParsingException {
    int port = TestUtils.findTCPPort();
    fakeServer = new FakeHTTPServer(port, RESPONSE1, true, false);
    final HTTPRequestBuilder hrb = new HTTPRequestBuilder(new URL("https://localhost:"+port));
    final HTTPClient httpClient = new HTTPClient();
    assertEquals("TEST123", httpClient.request(new HTTPAddress("localhost", port, true), hrb.build()).getBodyAsString());
  }

  @Test
  public void toLargeRequest() throws IOException, HTTPParsingException {
    int port = TestUtils.findTCPPort();
    fakeServer = new FakeHTTPServer(port, RESPONSE_HUGE, false, false);
    final HTTPRequestBuilder hrb = new HTTPRequestBuilder(new URL("http://localhost:"+port));
    final HTTPClient httpClient = new HTTPClient();
    try {
      httpClient.request(new HTTPAddress("localhost", port, false), hrb.build());
      fail();
    } catch(HTTPParsingException e) {
      assertEquals("Response Body to large!", e.getMessage());
    }
  }

  @Test
  public void toLargeRequestNoContentLength() throws IOException, HTTPParsingException {
    int port = TestUtils.findTCPPort();
    fakeServer = new FakeHTTPServer(port, RESPONSE_HUGE, false, true);
    final HTTPRequestBuilder hrb = new HTTPRequestBuilder(new URL("http://localhost:"+port));
    final HTTPClient httpClient = new HTTPClient();
    try {
      httpClient.request(new HTTPAddress("localhost", port, false), hrb.build());
      fail();
    } catch(HTTPParsingException e) {
      assertEquals("Response Body to large!", e.getMessage());
    }
  }

  @Test
  public void blockingRequestERROR() throws IOException, InterruptedException, ExecutionException, TimeoutException {
    int port = TestUtils.findTCPPort();
    fakeServer = new FakeHTTPServer(port, RESPONSE_ERROR_1, false, false);
    final HTTPRequestBuilder hrb = new HTTPRequestBuilder(new URL("http://localhost:"+port));
    final HTTPClient httpClient = new HTTPClient();
    final ListenableFuture<HTTPResponseData> lf = httpClient.requestAsync(new HTTPAddress("localhost", port, false), hrb.build());
    final AtomicBoolean failed = new AtomicBoolean(false);
    lf.addCallback(new FutureCallback<HTTPResponseData>() {

      @Override
      public void handleResult(HTTPResponseData result) {
        fail();
      }

      @Override
      public void handleFailure(Throwable t) {
        failed.set(false);
      }});

    try {
      lf.get(5000, TimeUnit.MILLISECONDS);
      fail();
    } catch(ExecutionException e) {

    }
    assertFalse(failed.get());

  }

  @Test
  public void timeOut() throws IOException, InterruptedException, ExecutionException, TimeoutException {
    int port = TestUtils.findTCPPort();
    TCPServer server =  SEI.createTCPServer("localhost", port);
    server.setClientAcceptor(new ClientAcceptor() {
      @Override
      public void accept(Client c) {
        System.out.println("new Client!");
      }});
    server.start();
    final HTTPRequestBuilder hrb = new HTTPRequestBuilder(new URL("http://localhost:"+port));
    final HTTPClient httpClient = new HTTPClient();

    HTTPRequest hr = hrb.build();

    long start = System.currentTimeMillis();
    try{
      httpClient.request(new HTTPAddress("localhost", port, false), hr, HTTPClient.EMPTY_BUFFER, TimeUnit.MILLISECONDS, 100);
    } catch(Exception e) {
      System.out.println(System.currentTimeMillis() - start);
      assertTrue(System.currentTimeMillis() - start < 300);
      return;
    }
    fail();
  }
  
  
  @Test
  public void urlRequest() throws HTTPParsingException, IOException {
    int port = TestUtils.findTCPPort();
    fakeServer = new FakeHTTPServer(port, RESPONSE1, false, false);
    final HTTPClient httpClient = new HTTPClient();
    HTTPResponseData hrd = httpClient.request(new URL("http://localhost:"+port));
    assertEquals(CONTENT, hrd.getBodyAsString());
  }
}
