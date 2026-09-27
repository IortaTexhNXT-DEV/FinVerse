package com.iortatechnxt.brokerverse.brokerclaims.domain;

import java.time.Instant;

/**
 * Spring application event published by the status engine (wave CL1-B) after the status of a claim
 * changed and its history row was written, inside the same transaction (BRCLM.011/035;
 * CLAIMS_BROKING_DESIGN 8.1 and 14). It is also published for the first status set at recording
 * ({@code fromStatus} and {@code fromPhase} null), for a closure by a closing settlement type and
 * for a reopen. Listeners in other waves (CL1-A: the claims special remittance feed and the premium
 * notifications) use {@code @EventListener} or {@code @TransactionalEventListener} and read the
 * claim only through its getters.
 *
 * @param claimId claim id
 * @param companyId company of the claim
 * @param claimNo claim number
 * @param fromStatus previous status ({@code BCL_CLAIM_STATUS}), null for the first status
 * @param toStatus new status ({@code BCL_CLAIM_STATUS}); unchanged on a closure by settlement type
 * @param fromPhase previous phase, null for the first status
 * @param toPhase new phase
 * @param changedBy user who made the change (SYSTEM for jobs)
 * @param changedAt time of the change
 */
public record ClaimStatusChanged(
    Long claimId,
    Long companyId,
    String claimNo,
    String fromStatus,
    String toStatus,
    ClaimPhase fromPhase,
    ClaimPhase toPhase,
    String changedBy,
    Instant changedAt) {

  /**
   * Whether the phase changed (and with it the stage of workflow {@code BCL_CLAIM}).
   *
   * @return true when the phase differs from the previous one or the claim had none
   */
  public boolean phaseChanged() {
    return fromPhase != toPhase;
  }

  /**
   * Whether the claim entered a phase with this change.
   *
   * @param phase phase
   * @return true when the new phase is {@code phase} and the previous one was different
   */
  public boolean entered(ClaimPhase phase) {
    return toPhase == phase && fromPhase != phase;
  }

  /**
   * Whether the claim entered a status with this change (e.g. {@code BDOI_PREMIUM_REMITTANCE}).
   *
   * @param statusCode status code
   * @return true when the new status is {@code statusCode} and the previous one was different
   */
  public boolean enteredStatus(String statusCode) {
    return statusCode.equals(toStatus) && !statusCode.equals(fromStatus);
  }
}
