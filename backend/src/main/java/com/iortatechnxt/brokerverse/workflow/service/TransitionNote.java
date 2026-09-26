package com.iortatechnxt.brokerverse.workflow.service;

/**
 * Reason and comment given with an action.
 *
 * @param reasonCode reason code (list of values of the transition), may be null
 * @param comment free text, may be null
 */
public record TransitionNote(String reasonCode, String comment) {

  /** No reason, no comment. */
  public static final TransitionNote NONE = new TransitionNote(null, null);

  /**
   * A comment only.
   *
   * @param comment comment
   * @return note
   */
  public static TransitionNote comment(String comment) {
    return new TransitionNote(null, comment);
  }
}
