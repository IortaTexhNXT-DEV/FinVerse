package com.iortatechnxt.brokerverse.common.exception;

/**
 * Raised when a request names a company or branch outside the data scope of the signed-in user.
 * Mapped to HTTP 403 with the code {@value #CODE}.
 */
public class DataScopeDeniedException extends RuntimeException {

  /** Stable error code of the refusal. */
  public static final String CODE = "DATA_SCOPE_DENIED";

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param message business message shown to the user
   */
  public DataScopeDeniedException(String message) {
    super(message);
  }

  /**
   * Refusal of a company.
   *
   * @return exception
   */
  public static DataScopeDeniedException company() {
    return new DataScopeDeniedException(
        "You do not have access to the data of this company. Ask your administrator to extend"
            + " your data access.");
  }

  /**
   * Refusal of a branch.
   *
   * @return exception
   */
  public static DataScopeDeniedException branch() {
    return new DataScopeDeniedException(
        "You do not have access to the data of this branch. Ask your administrator to extend"
            + " your data access.");
  }
}
