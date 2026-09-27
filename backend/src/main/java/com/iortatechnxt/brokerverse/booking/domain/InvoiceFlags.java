package com.iortatechnxt.brokerverse.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.List;

/**
 * Flags of a booked invoice read by Operations (cashiering, remittance, commission).
 *
 * @param directPayment premium paid directly to the insurer: no client receivable, only commission
 *     receivable (BRNB.114)
 * @param cwt2Percent the client withholds 2 % creditable tax on premium (CSHID.027)
 * @param incentiveEligible matched at least one incentive criterion (BRNB.107, PMADD07)
 * @param businessType New Business or Renewal (BRNB.097)
 * @param incentiveCriteria codes of the catalog incentive criteria matched, comma-separated, null
 *     when none (PMADD07)
 */
@Embeddable
public record InvoiceFlags(
    @Column(name = "direct_payment", nullable = false) boolean directPayment,
    @Column(name = "cwt2_percent", nullable = false) boolean cwt2Percent,
    @Column(name = "incentive_eligible", nullable = false) boolean incentiveEligible,
    @Enumerated(EnumType.STRING) @Column(name = "business_type", nullable = false, length = 20)
        BusinessType businessType,
    @Column(name = "incentive_criteria", length = 500) String incentiveCriteria) {

  /** New Business when not given. */
  public InvoiceFlags {
    businessType = businessType == null ? BusinessType.NEW_BUSINESS : businessType;
  }

  /**
   * Flags without incentive criteria codes.
   *
   * @param directPayment direct payment
   * @param cwt2Percent CWT 2 %
   * @param incentiveEligible incentive eligible
   * @param businessType business type
   */
  public InvoiceFlags(
      boolean directPayment,
      boolean cwt2Percent,
      boolean incentiveEligible,
      BusinessType businessType) {
    this(directPayment, cwt2Percent, incentiveEligible, businessType, null);
  }

  /**
   * Flags with the matched incentive criteria (PMADD07): eligible when at least one matched.
   *
   * @param directPayment direct payment
   * @param cwt2Percent CWT 2 %
   * @param businessType business type
   * @param criteria matched criteria codes
   * @return flags
   */
  public static InvoiceFlags withCriteria(
      boolean directPayment,
      boolean cwt2Percent,
      BusinessType businessType,
      List<String> criteria) {
    return new InvoiceFlags(
        directPayment,
        cwt2Percent,
        !criteria.isEmpty(),
        businessType,
        criteria.isEmpty() ? null : String.join(",", criteria));
  }

  /**
   * The matched incentive criteria codes.
   *
   * @return codes, empty when none
   */
  public List<String> incentiveCriteriaCodes() {
    return incentiveCriteria == null || incentiveCriteria.isBlank()
        ? List.of()
        : List.of(incentiveCriteria.split(","));
  }
}
