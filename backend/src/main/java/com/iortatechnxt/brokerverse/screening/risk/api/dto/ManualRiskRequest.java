package com.iortatechnxt.brokerverse.screening.risk.api.dto;

import com.iortatechnxt.brokerverse.screening.risk.service.ManualRiskChange;
import java.util.List;
import java.util.Set;

/**
 * Body of "Update Risk Tag" (SNSRP-304; FR-SS-035). The mandatory fields are checked by the service
 * with the FRS messages.
 *
 * @param riskRating new rating ({@code KYC_RISK_RATING})
 * @param addTags tags to add
 * @param removeTags tags to end
 * @param justification justification (up to 2000 characters)
 * @param evidenceAttachmentIds evidence documents (at least one)
 * @param matchId match the change clears, may be {@code null}
 * @param caseId case holding the evidence, may be {@code null}
 * @param reference case or match number for the client audit, may be {@code null}
 */
public record ManualRiskRequest(
    String riskRating,
    Set<String> addTags,
    Set<String> removeTags,
    String justification,
    List<Long> evidenceAttachmentIds,
    Long matchId,
    Long caseId,
    String reference) {

  /**
   * The service request.
   *
   * @return change
   */
  public ManualRiskChange toChange() {
    return new ManualRiskChange(
        riskRating,
        addTags,
        removeTags,
        justification,
        evidenceAttachmentIds,
        matchId,
        caseId,
        reference);
  }
}
