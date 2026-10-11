package com.iortatechnxt.brokerverse.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;

/**
 * The incentive indicator of a booked invoice and its latest evaluation (FR-NB-118, FR-RN-087):
 * Pending until the invoice is fully paid, then Eligible or Not eligible.
 *
 * @param status indicator
 * @param evaluatedAt time of the latest evaluation, null before any
 * @param reason reason of the latest evaluation
 */
@Embeddable
public record InvoiceIncentive(
    @Enumerated(EnumType.STRING) @Column(name = "incentive_status", nullable = false, length = 20)
        IncentiveStatus status,
    @Column(name = "incentive_evaluated_at") Instant evaluatedAt,
    @Column(name = "incentive_reason", length = 300) String reason) {

  /** Not evaluated yet: pending. */
  public static final InvoiceIncentive PENDING =
      new InvoiceIncentive(IncentiveStatus.PENDING, null, null);

  /** Pending when not given. */
  public InvoiceIncentive {
    status = status == null ? IncentiveStatus.PENDING : status;
  }
}
