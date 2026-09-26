package com.iortatechnxt.brokerverse.adjustment.api.dto;

import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.service.RequestAging;
import java.time.Instant;
import java.time.LocalDate;

/**
 * An endorsement request in a work list (ADJID.021 aging from submission to completion).
 *
 * @param id id
 * @param requestNo request number
 * @param stage stage
 * @param requestClass class
 * @param endorsementType endorsement type
 * @param requestType request type
 * @param invoiceNo invoice
 * @param arn ARN
 * @param assuredName assured
 * @param insurerCode insurer
 * @param currency currency
 * @param effectiveDate effective date
 * @param negative whether it reduces the invoice
 * @param quotationRequired TSI increase above the package limit
 * @param duplicateOverride whether a duplicate was overridden
 * @param batchNo posting batch
 * @param createdBy requester
 * @param createdAt raised at
 * @param agingDays days from submission (or creation) to completion (or today)
 */
public record RequestSummaryResponse(
    Long id,
    String requestNo,
    String stage,
    String requestClass,
    String endorsementType,
    String requestType,
    String invoiceNo,
    String arn,
    String assuredName,
    String insurerCode,
    String currency,
    LocalDate effectiveDate,
    boolean negative,
    boolean quotationRequired,
    boolean duplicateOverride,
    String batchNo,
    String createdBy,
    Instant createdAt,
    long agingDays) {

  /**
   * Maps a request.
   *
   * @param r request
   * @param now current time (aging)
   * @return summary
   */
  public static RequestSummaryResponse from(EndorsementRequest r, Instant now) {
    return new RequestSummaryResponse(
        r.getId(),
        r.getRequestNo(),
        r.getStage().name(),
        r.getRequestClass().name(),
        r.getTerms().endorsementType(),
        r.getTerms().requestType(),
        r.getSubject().invoiceNo(),
        r.getSubject().arn(),
        r.getSubject().assuredName(),
        r.getSubject().insurerCode(),
        r.getSubject().currency(),
        r.getTerms().effectiveDate(),
        r.isNegative(),
        r.isQuotationRequired(),
        r.getDuplicateOverride() != null,
        r.outcome().batchNo(),
        r.getCreatedBy(),
        r.getCreatedAt(),
        RequestAging.days(r, now));
  }
}
