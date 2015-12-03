package org.threadly.litesockets.protocols.http.shared;

public enum HTTPResponseCode {
  Continue(100, "Continue"), SwitchingProtocols(100, "Switching Protocols"), 
  OK(200, "OK"), Created(201, "Created"), Accepted(202, "Accepted"), NonAuthoritativeInformation(203, "Non-Authoritative Information"), NoContent(204, "No Content"), ResetContent(205, "Reset Content"), PartialContent(206, "Partial Content"), 
  MultipleChoices(300, "Multiple Choice"), MovedPermanently(301, "Moved Permanently"), Found(302, "Found"), SeeOther(303, "See Other"), NotModified(304, "Not Modified"), Unused(305, "(Unused)"), TemporaryRedirect(306, "Temporary Redirect"),
  BadRequest(400, "Bad Request"), Unauthorized(401, "Unauthorized"), PaymentRequired(402, "Payment Required"), Forbidden(403, "Forbidden"), NotFound(404, "Not Found"), MethodNotAllowed(405, "Method Not Allowed"), NotAcceptable(406, "Not Acceptable"), ProxyAuthenticationRequired(407, "Proxy Authentication Required"), RequestTimeout(408, "Request Timeout"), Conflict(409, "Conflict"), Gone(410, "Gone"), LengthRequired(411, "Length Required"), PreconditionFailed(412, "Precondition Failed"), RequestEntityTooLarge(413, "Request Entity Too Large"), RequestURITooLong(414, "Request-URI Too Long"), UnsupportedMediaType(415, "Unsupported Media Type"), RequestedRangeNotSatisfiable(416, "Requested Range Not Satisfiable"), ExpectationFailed(417, "Expectation Failed"),
  InternalServerError(500, "Internal Server Error"), NotImplemented(501, "Not Implemented"), BadGateway(502, "Bad Gateway"), ServiceUnavailable(503, "Service Unavailable"), GatewayTimeout(504, "Gateway Timeout"), HTTPVersionNotSupported(505, "HTTP Version Not Supported");
  
  private int val;
  private String text;
  private HTTPResponseCode(int val, String text) {
    this.val = val;
    this.text = text;
  }
  
  public int getId() {
    return val;
  }
  
  @Override
  public String toString() {
    return text;
  }
  
  public static HTTPResponseCode findResponseCode(int val) {
    for(HTTPResponseCode hrc: HTTPResponseCode.values()) { 
      if(hrc.getId() == val) {
        return hrc;
      }
    }
    throw new IllegalArgumentException("Could not find ResponseCode: " + val);
  }
}
