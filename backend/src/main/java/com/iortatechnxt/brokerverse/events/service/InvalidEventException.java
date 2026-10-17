package com.iortatechnxt.brokerverse.events.service;

/**
 * A consumed record is not a valid integration event (missing envelope fields, unknown payload
 * shape). Not retried: the record goes to its dead-letter topic at once.
 */
public class InvalidEventException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param message what is wrong
   */
  public InvalidEventException(String message) {
    super(message);
  }
}
