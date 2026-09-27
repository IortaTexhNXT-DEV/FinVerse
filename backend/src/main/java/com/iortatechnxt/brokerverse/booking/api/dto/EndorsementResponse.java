package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.domain.BookingEndorsement;
import com.iortatechnxt.brokerverse.booking.domain.CancellationKind;
import com.iortatechnxt.brokerverse.booking.domain.EndorsementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * An endorsement or cancellation.
 *
 * @param id id
 * @param endorsementNo endorsement number
 * @param arn account
 * @param accountId account id
 * @param type type
 * @param cancellationKind cancellation kind
 * @param periodBasis period basis
 * @param effectiveDate effective date
 * @param policyYear policy year
 * @param sumInsuredChange sum insured change
 * @param ratePercent rate
 * @param description description
 * @param reasonCode reason
 * @param invoiceNo invoice booked
 * @param createdBy user
 * @param createdAt time
 */
public record EndorsementResponse(
    Long id,
    String endorsementNo,
    String arn,
    Long accountId,
    EndorsementType type,
    CancellationKind cancellationKind,
    String periodBasis,
    LocalDate effectiveDate,
    int policyYear,
    BigDecimal sumInsuredChange,
    BigDecimal ratePercent,
    String description,
    String reasonCode,
    String invoiceNo,
    String createdBy,
    Instant createdAt) {

  /**
   * Maps an endorsement.
   *
   * @param e endorsement
   * @return response
   */
  public static EndorsementResponse from(BookingEndorsement e) {
    return new EndorsementResponse(
        e.getId(),
        e.getEndorsementNo(),
        e.getArn(),
        e.getAccountId(),
        e.getType(),
        e.getCancellationKind(),
        e.getPeriodBasis(),
        e.getEffectiveDate(),
        e.getPolicyYear(),
        e.getSumInsuredChange(),
        e.getRatePercent(),
        e.getDescription(),
        e.getReasonCode(),
        e.getInvoiceNo(),
        e.getCreatedBy(),
        e.getCreatedAt());
  }
}
