package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.booking.domain.CancellationKind;
import com.iortatechnxt.brokerverse.booking.domain.CommissionTerms;
import com.iortatechnxt.brokerverse.booking.domain.EndorsementType;
import com.iortatechnxt.brokerverse.booking.domain.PremiumComponents;
import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An endorsement or cancellation to post on a booked account (BRNB.076/081/094) - input of the
 * {@link EndorsementPostingService#post} contract used by Operations Adjustment (ADJID.001/011).
 *
 * <p>A financial endorsement gives either the sum insured change (rated for the remaining term with
 * the catalog calculator, pro-rata or short-period) or the premium components and commission
 * computed by the caller (e.g. recomputed per insurer). A cancellation gives its kind; a partial
 * one its basis.
 *
 * @param arn account (BOOKED)
 * @param type positive, negative, non-financial or cancellation
 * @param cancellationKind FLAT, FLAT_RETAIN_DST or PARTIAL (cancellations only)
 * @param effectiveDate effective date, within a booked policy year
 * @param basis PRO_RATA or SHORT_PERIOD for calculated and partial amounts (default PRO_RATA)
 * @param sumInsuredChange change of the sum insured (calculated financial endorsements)
 * @param ratePercent premium rate in percent, null for the product default
 * @param premium premium components given by the caller (signed), null to calculate
 * @param commission commission given by the caller (signed), null to derive it
 * @param description description of the change
 * @param reasonCode reason (list CANCELLATION_REASON for cancellations)
 * @param bookingDate booking date of the resulting invoice, null for today
 * @param sourceReference caller's idempotency key (e.g. adjustment request), may be null
 */
public record EndorsementPosting(
    String arn,
    EndorsementType type,
    CancellationKind cancellationKind,
    LocalDate effectiveDate,
    PeriodBasis basis,
    BigDecimal sumInsuredChange,
    BigDecimal ratePercent,
    PremiumComponents premium,
    CommissionTerms commission,
    String description,
    String reasonCode,
    LocalDate bookingDate,
    String sourceReference) {

  /**
   * The period basis, pro-rata when not given.
   *
   * @return basis
   */
  public PeriodBasis basisOrDefault() {
    return basis == null || basis == PeriodBasis.ANNUAL ? PeriodBasis.PRO_RATA : basis;
  }
}
