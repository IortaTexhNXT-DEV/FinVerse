package com.iortatechnxt.brokerverse.placement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Details of a hold cover request made from a renewal (Renewal BRD BRRN.042; FR-RN-086): the
 * duration chosen, the expiring policy number, the remarks and the requester.
 *
 * @param durationDays duration in days (30 or 60), null for a placement request
 * @param expiringPolicyNo expiring policy number, may be null
 * @param remarks remarks of the requester, may be null
 * @param requestedBy user who requested it, may be null
 */
@Embeddable
public record HoldCoverRequestDetails(
    @Column(name = "duration_days") Integer durationDays,
    @Column(name = "expiring_policy_no", length = 60) String expiringPolicyNo,
    @Column(name = "remarks", length = 200) String remarks,
    @Column(name = "requested_by", length = 50) String requestedBy) {

  /** No details (a hold cover requested from placement). */
  public static final HoldCoverRequestDetails NONE =
      new HoldCoverRequestDetails(null, null, null, null);
}
