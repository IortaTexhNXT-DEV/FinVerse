package com.iortatechnxt.brokerverse.configpromo.engine;

/** An import that cannot be applied; nothing of it is kept. The message is shown to the user. */
public class ApplyException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param message message for the user
   */
  public ApplyException(String message) {
    super(message);
  }

  /**
   * Creates the exception with its cause.
   *
   * @param message message for the user
   * @param cause cause
   */
  public ApplyException(String message, Throwable cause) {
    super(message, cause);
  }
}
