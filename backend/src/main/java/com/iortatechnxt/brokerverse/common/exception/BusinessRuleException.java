package com.iortatechnxt.brokerverse.common.exception;

/**
 * Raised when a request violates a business rule (e.g. unbalanced journal, closed period).
 *
 * <p>Mapped to HTTP 422 with a stable machine readable {@code code}.
 */
public class BusinessRuleException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String code;

  /**
   * Creates the exception.
   *
   * @param code stable error code, UPPER_SNAKE_CASE
   * @param message human readable explanation
   */
  public BusinessRuleException(String code, String message) {
    super(message);
    this.code = code;
  }

  /**
   * Creates the exception with its technical cause (kept for the log, never shown to the user).
   *
   * @param code stable machine readable code (UPPER_SNAKE_CASE)
   * @param message human readable explanation
   * @param cause underlying exception
   */
  public BusinessRuleException(String code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
