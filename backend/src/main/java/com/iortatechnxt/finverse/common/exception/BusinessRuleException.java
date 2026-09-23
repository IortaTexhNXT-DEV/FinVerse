package com.iortatechnxt.finverse.common.exception;

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

  public String getCode() {
    return code;
  }
}
