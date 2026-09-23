package com.iortatechnxt.finverse.claims.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of a claim.
 *
 * <pre>
 * REGISTERED --(initial reserve approved)--&gt; OPEN --(partial settlement)--&gt; PARTIALLY_SETTLED
 * OPEN / PARTIALLY_SETTLED / REOPENED --(final settlement or close)--&gt; CLOSED --(reopen)--&gt; REOPENED
 * REGISTERED / OPEN / REOPENED --(repudiated / withdrawn, nothing paid)--&gt; REJECTED / WITHDRAWN
 * </pre>
 */
public enum ClaimStatus {
  /** Notified (FNOL); no approved reserve yet. */
  REGISTERED,
  /** Initial reserve approved; claim under handling. */
  OPEN,
  /** At least one partial settlement approved. */
  PARTIALLY_SETTLED,
  /** Final settlement approved or claim closed; outstanding reserve released. */
  CLOSED,
  /** Closed claim reopened for further handling. */
  REOPENED,
  /** Repudiated by the insurer (with reason). */
  REJECTED,
  /** Withdrawn by the claimant (with reason). */
  WITHDRAWN;

  private static final Set<ClaimStatus> ACTIVE =
      EnumSet.of(REGISTERED, OPEN, PARTIALLY_SETTLED, REOPENED);

  /**
   * Whether the claim is still being handled (reserves and settlements may be entered).
   *
   * @return true for registered, open, partially settled and reopened claims
   */
  public boolean isActive() {
    return ACTIVE.contains(this);
  }

  /**
   * Whether the claim has ended (closed, rejected or withdrawn).
   *
   * @return true when no longer active
   */
  public boolean isFinished() {
    return !isActive();
  }
}
