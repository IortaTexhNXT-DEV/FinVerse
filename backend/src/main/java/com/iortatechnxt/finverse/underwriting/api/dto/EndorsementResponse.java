package com.iortatechnxt.finverse.underwriting.api.dto;

import com.iortatechnxt.finverse.underwriting.domain.Endorsement;
import com.iortatechnxt.finverse.underwriting.domain.EndorsementType;
import java.time.LocalDate;

/**
 * Endorsement view.
 *
 * @param id id
 * @param policyId policy
 * @param policyNo policy number
 * @param endorsementNo number within the policy
 * @param documentNo printable number
 * @param type type
 * @param issueDate issue date
 * @param effectiveDate effective date
 * @param newPeriodFrom renewed period start
 * @param newPeriodTo renewed period end
 * @param uwYear underwriting year of the period the endorsement belongs to
 * @param description description
 * @param currency policy currency
 * @param document workflow and accounting references
 * @param premium premium change
 */
public record EndorsementResponse(
    Long id,
    Long policyId,
    String policyNo,
    int endorsementNo,
    String documentNo,
    EndorsementType type,
    LocalDate issueDate,
    LocalDate effectiveDate,
    LocalDate newPeriodFrom,
    LocalDate newPeriodTo,
    int uwYear,
    String description,
    String currency,
    DocumentStatusResponse document,
    PremiumResponse premium) {

  /**
   * Maps an endorsement (policy loaded).
   *
   * @param e endorsement
   * @return response
   */
  public static EndorsementResponse from(Endorsement e) {
    return new EndorsementResponse(
        e.getId(),
        e.getPolicy().getId(),
        e.getPolicy().getPolicyNo(),
        e.getEndorsementNo(),
        e.documentNo(),
        e.getEndorsementType(),
        e.getIssueDate(),
        e.getEffectiveDate(),
        e.getNewPeriodFrom(),
        e.getNewPeriodTo(),
        e.getUwYear(),
        e.getDescription(),
        e.getPolicy().getCurrency(),
        DocumentStatusResponse.of(e.getWorkflow(), e.getRefs(), e.getCreatedBy(), e.getCreatedAt()),
        PremiumResponse.from(e.getPremium()));
  }
}
