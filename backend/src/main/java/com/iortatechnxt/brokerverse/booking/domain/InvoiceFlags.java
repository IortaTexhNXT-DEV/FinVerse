package com.iortatechnxt.brokerverse.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Flags of a booked invoice read by Operations (cashiering, remittance, commission).
 *
 * @param directPayment premium paid directly to the insurer: no client receivable, only commission
 *     receivable (BRNB.114)
 * @param cwt2Percent the client withholds 2 % creditable tax on premium (CSHID.027)
 * @param incentiveEligible matched an incentive rule (BRNB.107)
 * @param businessType New Business or Renewal (BRNB.097)
 */
@Embeddable
public record InvoiceFlags(
    @Column(name = "direct_payment", nullable = false) boolean directPayment,
    @Column(name = "cwt2_percent", nullable = false) boolean cwt2Percent,
    @Column(name = "incentive_eligible", nullable = false) boolean incentiveEligible,
    @Enumerated(EnumType.STRING) @Column(name = "business_type", nullable = false, length = 20)
        BusinessType businessType) {

  /** New Business when not given. */
  public InvoiceFlags {
    businessType = businessType == null ? BusinessType.NEW_BUSINESS : businessType;
  }
}
