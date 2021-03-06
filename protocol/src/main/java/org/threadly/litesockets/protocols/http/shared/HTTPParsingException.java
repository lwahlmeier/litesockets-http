package org.threadly.litesockets.protocols.http.shared;

/**
 * Whenever there is a problem processing HTTP protocol this exception is thrown.
 * 
 * @author lwahlmeier
 *
 */
public class HTTPParsingException extends Exception {
  private static final long serialVersionUID = 6877333877624172529L;

  public HTTPParsingException() {
  }

  public HTTPParsingException(String s) {
    super(s);
  }
  
  public HTTPParsingException(Throwable t) {
    super(t);
  }
  
  public HTTPParsingException(String s, Throwable t) {
    super(s, t);
  }
}
