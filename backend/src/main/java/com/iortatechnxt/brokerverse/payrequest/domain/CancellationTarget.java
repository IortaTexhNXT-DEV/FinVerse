package com.iortatechnxt.brokerverse.payrequest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * The disbursed check a check-cancellation request is about (MKT 1.19.0).
 *
 * @param requestNo refund or cash-advance request that was paid
 * @param dvNo its disbursement voucher
 * @param checkNo check number
 * @param reasonCode reason (LOV {@code DISB_CANCEL_REASON})
 */
@Embeddable
public record CancellationTarget(
    @Column(name = "target_request_no", length = 30) String requestNo,
    @Column(name = "target_dv_no", length = 40) String dvNo,
    @Column(name = "check_no", length = 40) String checkNo,
    @Column(name = "cancel_reason", length = 40) String reasonCode) {

  /** No target (refunds and cash advances). */
  public static final CancellationTarget NONE = new CancellationTarget(null, null, null, null);
}
