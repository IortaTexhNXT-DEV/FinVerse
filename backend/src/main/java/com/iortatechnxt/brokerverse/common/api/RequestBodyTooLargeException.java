package com.iortatechnxt.brokerverse.common.api;

import java.io.IOException;

/**
 * Raised while a request body is read beyond the configured size limit; answered with 413 and
 * {@value #CODE} by the {@link GlobalExceptionHandler}.
 */
public class RequestBodyTooLargeException extends IOException {

  /** Error code of the refusal. */
  public static final String CODE = "REQUEST_TOO_LARGE";

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param maxBytes the limit
   */
  public RequestBodyTooLargeException(long maxBytes) {
    super("The request is larger than " + maxBytes + " bytes");
  }
}
