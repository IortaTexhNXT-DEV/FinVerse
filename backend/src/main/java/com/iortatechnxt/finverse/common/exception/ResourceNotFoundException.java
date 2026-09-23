package com.iortatechnxt.finverse.common.exception;

/** Raised when a requested resource does not exist. Mapped to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param resource resource name, e.g. "GL account"
   * @param key identifier that was looked up
   */
  public ResourceNotFoundException(String resource, Object key) {
    super(resource + " not found: " + key);
  }
}
