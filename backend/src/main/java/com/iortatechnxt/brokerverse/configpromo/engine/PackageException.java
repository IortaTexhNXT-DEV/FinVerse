package com.iortatechnxt.brokerverse.configpromo.engine;

/**
 * A package that cannot be used: unreadable, incomplete, altered or not signed with the key of the
 * platform. The message is shown to the user.
 */
public class PackageException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param message message for the user
   */
  public PackageException(String message) {
    super(message);
  }

  /**
   * Creates the exception with its cause.
   *
   * @param message message for the user
   * @param cause cause
   */
  public PackageException(String message, Throwable cause) {
    super(message, cause);
  }
}
