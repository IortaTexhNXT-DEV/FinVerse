package com.iortatechnxt.finverse.reinsurance.domain;

/** Lifecycle of a facultative placement. */
public enum FacStatus {
  /** Requirement identified by the allocation; participants being negotiated. */
  PROVISIONAL,
  /** Participants recorded and submitted; waiting for a checker. */
  PENDING_APPROVAL,
  /** Approved: premium ceded to the participants and accounted for. */
  PLACED,
  /** Final: slip closed, no further changes. */
  CLOSED;

  /**
   * Whether the participants share premium and losses.
   *
   * @return true when placed or closed
   */
  public boolean isPlaced() {
    return this == PLACED || this == CLOSED;
  }
}
