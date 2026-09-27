package com.iortatechnxt.brokerverse.adjustment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * What an endorsement request asks for (ADJID.002/004, Annex V): the type, the reason, the
 * insurer's endorsement reference, the effective date and the inputs of the recompute.
 *
 * @param endorsementType list {@code ENDORSEMENT_TYPE} (FIN_ / NF_ / INT_)
 * @param requestType list {@code ENDORSEMENT_REQUEST_TYPE}, null for none
 * @param reasonCode list {@code CANCELLATION_REASON}, null for none
 * @param endorsementRef insurer's endorsement reference, may be null
 * @param effectiveDate effective date of the endorsement or cancellation
 * @param refundBasis pro-rata or short-period pricing of the remaining term
 * @param sumInsuredChange change of the total sum insured (TSI change), signed
 * @param ratePercent premium rate in percent for the TSI change, null for the product rate
 * @param newPeriodFrom new inception (period changes), may be null
 * @param newPeriodTo new expiry (period changes), may be null
 * @param description description of the change
 * @param instructions additional or other instructions (endorsement slip)
 */
@Embeddable
public record RequestTerms(
    @Column(name = "endorsement_type", nullable = false, length = 40) String endorsementType,
    @Column(name = "request_type", length = 40) String requestType,
    @Column(name = "reason_code", length = 40) String reasonCode,
    @Column(name = "endorsement_ref", length = 60) String endorsementRef,
    @Column(name = "effective_date", nullable = false) LocalDate effectiveDate,
    @Enumerated(EnumType.STRING) @Column(name = "refund_basis", nullable = false, length = 20)
        RefundBasis refundBasis,
    @Column(name = "sum_insured_change", precision = 19, scale = 2) BigDecimal sumInsuredChange,
    @Column(name = "rate_percent", precision = 19, scale = 8) BigDecimal ratePercent,
    @Column(name = "new_period_from") LocalDate newPeriodFrom,
    @Column(name = "new_period_to") LocalDate newPeriodTo,
    @Column(nullable = false, length = 1000) String description,
    @Column(length = 1000) String instructions) {

  /** Pro-rata unless given. */
  public RequestTerms {
    refundBasis = refundBasis == null ? RefundBasis.PRO_RATA : refundBasis;
  }
}
