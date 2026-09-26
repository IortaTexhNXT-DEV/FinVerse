package com.iortatechnxt.brokerverse.messaging.service;

import java.io.Serial;

/** A delivery attempt failed; the message says why (shown in the send log). */
public class MailDeliveryException extends Exception {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param message reason
   * @param cause cause, may be null
   */
  public MailDeliveryException(String message, Throwable cause) {
    super(message, cause);
  }
}
