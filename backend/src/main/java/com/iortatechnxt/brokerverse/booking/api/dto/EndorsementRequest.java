package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.domain.CancellationKind;
import com.iortatechnxt.brokerverse.booking.domain.EndorsementType;
import com.iortatechnxt.brokerverse.booking.service.EndorsementPosting;
import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An endorsement or cancellation entered on a booked account (BRNB.076/081/094).
 *
 * @param arn account
 * @param type endorsement type
 * @param cancellationKind cancellation kind (cancellations only)
 * @param effectiveDate effective date
 * @param basis PRO_RATA or SHORT_PERIOD
 * @param sumInsuredChange sum insured change (financial endorsements)
 * @param ratePercent premium rate, null for the product default
 * @param premium premium components computed by the caller, null to calculate
 * @param description description
 * @param reasonCode reason (list CANCELLATION_REASON for cancellations)
 * @param bookingDate booking date, null for today
 */
public record EndorsementRequest(
    @NotBlank @Size(max = 30) String arn,
    @NotNull EndorsementType type,
    CancellationKind cancellationKind,
    @NotNull LocalDate effectiveDate,
    PeriodBasis basis,
    BigDecimal sumInsuredChange,
    BigDecimal ratePercent,
    PremiumDto premium,
    @NotBlank @Size(max = 1000) String description,
    @Size(max = 40) String reasonCode,
    LocalDate bookingDate) {

  /**
   * The posting.
   *
   * @return posting
   */
  public EndorsementPosting toPosting() {
    return new EndorsementPosting(
        arn,
        type,
        cancellationKind,
        effectiveDate,
        basis,
        sumInsuredChange,
        ratePercent,
        premium == null ? null : premium.toComponents(),
        null,
        description,
        reasonCode,
        bookingDate,
        null);
  }
}
