package org.threadly.litesockets.client.http;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import org.threadly.concurrent.PriorityScheduler;
import org.threadly.litesockets.ThreadedSocketExecuter;
import org.threadly.litesockets.protocols.http.request.HTTPRequest;
import org.threadly.litesockets.protocols.http.response.HTTPResponse;
import org.threadly.litesockets.protocols.http.response.HTTPResponseBuilder;
import org.threadly.litesockets.protocols.http.shared.HTTPConstants;
import org.threadly.litesockets.protocols.http.shared.RequestType;
import org.threadly.litesockets.server.http.HTTPServer;
import org.threadly.litesockets.server.http.HTTPServer.BodyFuture;
import org.threadly.litesockets.server.http.HTTPServer.ResponseWriter;

public class SimpleHttpServer {

  public static void main(String[] args) throws IOException, InterruptedException {
    PriorityScheduler ps = new PriorityScheduler(10);
    ThreadedSocketExecuter TSE = new ThreadedSocketExecuter(ps);
    TSE.start();
    int port = 5544;
    HTTPServer server = new HTTPServer(TSE, "localhost", port, null);
    server.setHandler(new HTTPStaticFileHandler("/tmp/http"));
    server.start();
    Thread.sleep(1000000);
  }
  public static class HTTPStaticFileHandler implements HTTPServer.Handler {
    public static final HTTPResponse NOTFOUND = new HTTPResponseBuilder().setResponseHeader(HTTPConstants.NOT_FOUND_RESPONSE_HEADER).build();
    public static final HTTPResponse SERVERERROR = new HTTPResponseBuilder().setResponseHeader(HTTPConstants.SERVER_ERROR_RESPONSE_HEADER).build();
    public final String rootPath;
    
    
    public HTTPStaticFileHandler(String rootPath) {
      this.rootPath = rootPath;
      //System.out.println(new File(this.rootPath).isDirectory());
    }
    
    @Override
    public HTTPServer getServer() {
      return null;
    }

    @Override
    public void handle(HTTPRequest httpRequest, BodyFuture bodyListener, ResponseWriter responseWriter) {
      if(httpRequest.getHTTPRequestHeaders().getRequestType().equals(RequestType.GET.toString())) {
        String path = httpRequest.getHTTPRequestHeaders().getRequestPath();
        if(!path.startsWith("/")) {
          path = "/"+path;
        }
        String fullPath = this.rootPath+path;
        File f = new File(fullPath);
        if(!f.exists() || !f.isFile())  {
          System.out.println("Not found:"+path);
          responseWriter.sendHTTPResponse(NOTFOUND);
          responseWriter.done();
        } else {
          try {
            //System.out.println("found:"+path);
            RandomAccessFile raf = new RandomAccessFile(f, "r");
            byte[] ba = new byte[(int) raf.length()];
            raf.read(ba);
            raf.close();
            HTTPResponseBuilder hrb = new HTTPResponseBuilder().setHeader(HTTPConstants.HTTP_KEY_CONTENT_LENGTH, Integer.toString(ba.length));
            hrb.setHTTPVersion(httpRequest.getHTTPRequestHeaders().getHttpVersion());
            responseWriter.sendHTTPResponse(hrb.build());
            responseWriter.writeBody(ByteBuffer.wrap(ba));
            responseWriter.done();
          } catch (IOException e) {
            System.out.println("ERROR:"+path);
            responseWriter.sendHTTPResponse(SERVERERROR);
            responseWriter.done();          }
        }
      }
    }
    
  }
}
