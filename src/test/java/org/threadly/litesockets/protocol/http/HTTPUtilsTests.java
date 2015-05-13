package org.threadly.litesockets.protocol.http;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.threadly.litesockets.protocol.http.HTTPConstants.REQUEST_TYPE;
import org.threadly.litesockets.protocol.http.structures.HTTPUtils;
import org.threadly.litesockets.protocol.http.structures.HTTPHeaders;
import org.threadly.litesockets.protocol.http.structures.HTTPRequestHeader;

public class HTTPUtilsTests {
  
  @Test
  public void queryTest1() {
    String query1 = "?test=test1&343=334&q&5&4&blah=wewew";
    Map<String, String> map = HTTPUtils.parseQueryString(query1);
    String tmp = HTTPUtils.queryToString(map);
    Map<String, String> map2 =  HTTPUtils.parseQueryString(tmp);
    String tmp2 = HTTPUtils.queryToString(map2);
    assertEquals(map, map2);
  }
  
  @Test
  public void queryTest2() {
    String query1 = "test=test1&343=334&q&5&4&blah=wewew";
    Map<String, String> map = HTTPUtils.parseQueryString(query1);
    String tmp = HTTPUtils.queryToString(map);
    Map<String, String> map2 =  HTTPUtils.parseQueryString(tmp);
    String tmp2 = HTTPUtils.queryToString(map2);
    assertEquals(map, map2);
  }

  @Test
  public void queryTest3() {
    String query1 = "";
    Map<String, String> map = HTTPUtils.parseQueryString(query1);
    map.put("blah", null);
    String tmp = HTTPUtils.queryToString(map);
    Map<String, String> map2 =  HTTPUtils.parseQueryString(tmp);
    String tmp2 = HTTPUtils.queryToString(map2);
    Map<String, String> map3 =  HTTPUtils.parseQueryString(tmp2);
    
    System.out.println(tmp2);
    System.out.println(map+":"+map.size());
    assertEquals(tmp, tmp2);
    assertEquals(map3, map2);
  }
  
  @Test
  public void queryTest4() {
    String query1 = "";
    Map<String, String> map = HTTPUtils.parseQueryString(query1);
    String tmp = HTTPUtils.queryToString(map);
    Map<String, String> map2 =  HTTPUtils.parseQueryString(tmp);
    String tmp2 = HTTPUtils.queryToString(map2);
    Map<String, String> map3 =  HTTPUtils.parseQueryString(tmp2);
    
    
    assertEquals(tmp, tmp2);
    assertEquals(map3, map2);
  }
  
  @Test
  public void HTTPHeadersTest1() {
    HTTPHeaders hh1 = new HTTPHeaders(HTTPConstants.DEFAULT_HEADERS_MAP);
    HTTPHeaders hh2 = new HTTPHeaders(hh1.toString());
    System.out.println(hh1);
    System.out.println(hh2);
    //System.out.println(hh2.equals(hh1));
    assertEquals(hh1, hh2);
  }
  
  @Test
  public void HTTPHeadersTest2() {
    Map<String, String> map = new HashMap<String, String>(HTTPConstants.DEFAULT_HEADERS_MAP);
    map.put("X-Blah", "value");
    HTTPHeaders hh1 = new HTTPHeaders(HTTPConstants.DEFAULT_HEADERS_MAP);
    HTTPHeaders hh2 = new HTTPHeaders(map);
    System.out.println(hh1);
    System.out.println(hh2);
    //System.out.println(hh2.equals(hh1));
    assertFalse(hh1.equals(hh2));
    assertFalse(hh2.equals(hh1));
  }
  
  @Test
  public void HTTPHeadersTest3() {
    Map<String, String> map = new HashMap<String, String>(HTTPConstants.DEFAULT_HEADERS_MAP);
    map.put("X-Blah", "value");
    map.remove("accept");
    HTTPHeaders hh1 = new HTTPHeaders(HTTPConstants.DEFAULT_HEADERS_MAP);
    HTTPHeaders hh2 = new HTTPHeaders(map);
    System.out.println(hh1);
    System.out.println(hh2);
    //System.out.println(hh2.equals(hh1));
    assertFalse(hh1.equals(hh2));
    assertFalse(hh2.equals(hh1));
    assertTrue(hh2.equals(hh2));
    assertFalse(hh2.equals(map));
  }
  
  @Test
  public void HTTPHeadersTest4() {
    LinkedHashMap<String, String> map = new LinkedHashMap<String, String>(HTTPConstants.DEFAULT_HEADERS_MAP);
    
    LinkedHashMap<String, String> map2 = new LinkedHashMap<String, String>();
    LinkedList<Entry<String, String>> list = new LinkedList<Entry<String, String>>(map.entrySet());
    while(list.size() > 0) {
      Entry<String, String> l = list.removeLast();
      map2.put(l.getKey(), l.getValue());
    }
    HTTPHeaders hh1 = new HTTPHeaders(map);
    HTTPHeaders hh2 = new HTTPHeaders(map2);
    System.out.println(hh1);
    System.out.println(hh2);
    //System.out.println(hh2.equals(hh1));
    assertTrue(hh1.equals(hh2));
    assertTrue(hh2.equals(hh1));
    assertTrue(hh2.equals(hh2));
    assertFalse(hh2.equals(map));
    assertFalse(hh2.hashCode() == hh1.hashCode());
    assertFalse(hh2.isChunked());
    assertEquals("*/*", hh2.getHeader(HTTPConstants.HTTP_KEY_ACCEPT));
    assertEquals(-1, hh2.getContentLength());
  }
  @Test
  public void HTTPHeadersTest5() {
    Map<String, String> map = new HashMap<String, String>(HTTPConstants.DEFAULT_HEADERS_MAP);
    map.put(HTTPConstants.HTTP_KEY_CONTENT_LENGTH, "34342");
    HTTPHeaders hh1 = new HTTPHeaders(map);
    assertEquals(34342, hh1.getContentLength());
  }
  
  @Test
  public void HTTPHeadersTest6() {
    Map<String, String> map = new HashMap<String, String>(HTTPConstants.DEFAULT_HEADERS_MAP);
    map.put(HTTPConstants.HTTP_KEY_CONTENT_LENGTH, "BAD!");
    HTTPHeaders hh1 = new HTTPHeaders(map);
    assertEquals(-1, hh1.getContentLength());
  }
  
  @Test
  public void HTTPRequestHeaderTest1() {
    String req = "GET / HTTP/1.1";
    String req2 = "GET /TEST? HTTP/1.1";
    HTTPRequestHeader hrh1 = new HTTPRequestHeader(req);
    HTTPRequestHeader hrh2 = new HTTPRequestHeader(hrh1.toString());
    HTTPRequestHeader hrh3 = new HTTPRequestHeader(req2);
    assertEquals(REQUEST_TYPE.GET.toString(), hrh1.getRequestType());
    assertEquals(HTTPConstants.HTTP_VERSION_1_1, hrh1.getHttpVersion());
    assertEquals("/", hrh1.getRequestPath());
    assertEquals(req, hrh1.toString());
    assertEquals(new HashMap<String, String>(), hrh1.getRequestQuery());
    assertEquals(hrh2, hrh1);
    assertFalse(hrh3.equals(hrh1));
    assertFalse(hrh3.equals(req));
    assertFalse(hrh3.hashCode() == hrh2.hashCode());
  }
  
  @Test
  public void HTTPRequestHeaderTest2() {
    String req = "POST / HTTP/1.0";
    HTTPRequestHeader hrh1 = new HTTPRequestHeader(req);
    assertEquals(HTTPConstants.HTTP_VERSION_1_0, hrh1.getHttpVersion());
    assertEquals(REQUEST_TYPE.POST.toString(), hrh1.getRequestType());
  }
  
  @Test(expected=IllegalStateException.class)
  public void HTTPRequestHeaderBad1() {
    String req = "GET / HTTP/1.4";
    HTTPRequestHeader hrh1 = new HTTPRequestHeader(req);
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void HTTPRequestHeaderBad2() {
    String req = "GET / HTTP/1.4 NLDS DSAD SD Aa";
    HTTPRequestHeader hrh1 = new HTTPRequestHeader(req);
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void HTTPRequestHeaderBad3() {
    String req = "GET ";
    HTTPRequestHeader hrh1 = new HTTPRequestHeader(req);
  }
  
  @Test
  public void HTTPRequestHeaderTest3() {
    HTTPRequestHeader hrh1 = new HTTPRequestHeader(REQUEST_TYPE.DELETE, "/ds/sds/ds/", new HashMap<String, String>(), HTTPConstants.HTTP_VERSION_1_1);
    assertEquals(REQUEST_TYPE.DELETE.toString(), hrh1.getRequestType());
    assertEquals(HTTPConstants.HTTP_VERSION_1_1, hrh1.getHttpVersion());
    assertEquals("/ds/sds/ds/", hrh1.getRequestPath());
    assertEquals("DELETE /ds/sds/ds/ HTTP/1.1", hrh1.toString());
    assertEquals(new HashMap<String, String>(), hrh1.getRequestQuery());
  }
  
  @Test
  public void HTTPRequestHeaderTest4() {
    HTTPRequestHeader hrh1 = new HTTPRequestHeader(REQUEST_TYPE.DELETE, "/ds/sds/ds/", null, HTTPConstants.HTTP_VERSION_1_1);
    assertEquals(REQUEST_TYPE.DELETE.toString(), hrh1.getRequestType());
    assertEquals(HTTPConstants.HTTP_VERSION_1_1, hrh1.getHttpVersion());
    assertEquals("/ds/sds/ds/", hrh1.getRequestPath());
    assertEquals("DELETE /ds/sds/ds/ HTTP/1.1", hrh1.toString());
    assertEquals(new HashMap<String, String>(), hrh1.getRequestQuery());
  }
  @Test
  public void HTTPRequestHeaderTest5() {
    HashMap<String, String> map = new HashMap<String, String>();
    map.put("test", "te");
    HTTPRequestHeader hrh1 = new HTTPRequestHeader(REQUEST_TYPE.DELETE, "/ds/sds/ds/", map, HTTPConstants.HTTP_VERSION_1_0);
    assertEquals(REQUEST_TYPE.DELETE.toString(), hrh1.getRequestType());
    assertEquals(HTTPConstants.HTTP_VERSION_1_0, hrh1.getHttpVersion());
    assertEquals("/ds/sds/ds/", hrh1.getRequestPath());
    assertEquals("DELETE /ds/sds/ds/?test=te HTTP/1.0", hrh1.toString());
    assertEquals(map, hrh1.getRequestQuery());
  }
  
  @Test(expected=IllegalStateException.class)
  public void HTTPRequestHeaderTest6() {
    HTTPRequestHeader hrh1 = new HTTPRequestHeader(REQUEST_TYPE.DELETE, "/ds/sds/ds/", new HashMap<String, String>(), "HTTP/1.2");
  }
}
