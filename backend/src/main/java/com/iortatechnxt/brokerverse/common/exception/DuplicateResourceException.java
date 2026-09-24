package com.iortatechnxt.brokerverse.common.exception;

/** Raised when creating a resource whose business key already exists. Mapped to HTTP 409. */
public class DuplicateResourceException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param resource resource name
   * @param key duplicated business key
   */
  public DuplicateResourceException(String resource, Object key) {
    super(resource + " already exists: " + key);
  }
}
