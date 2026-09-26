package com.iortatechnxt.brokerverse.common.storage;

/** A failure of the object store (not found, refused, unreachable). */
public class FileStoreException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param message description
   */
  public FileStoreException(String message) {
    super(message);
  }

  /**
   * Creates the exception with its cause.
   *
   * @param message description
   * @param cause cause
   */
  public FileStoreException(String message, Throwable cause) {
    super(message, cause);
  }
}
