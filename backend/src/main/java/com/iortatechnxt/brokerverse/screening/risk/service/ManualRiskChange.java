package com.iortatechnxt.brokerverse.screening.risk.service;

import java.util.List;
import java.util.Set;

/**
 * A manual update of a client's risk profile by an investigator (SNSRP-304; FR-SS-035 "Update Risk
 * Tag" dialog).
 *
 * @param riskRating the new rating ({@code KYC_RISK_RATING}), mandatory
 * @param addTags tags to add ({@code CLIENT_TAG})
 * @param removeTags tags to end
 * @param justification the justification, mandatory (up to 2000 characters)
 * @param evidenceAttachmentIds the evidence documents (attachment ids), at least one
 * @param matchId the match the change clears, may be {@code null}
 * @param caseId the screening case holding the evidence, may be {@code null}
 * @param reference the case or match number written to the client audit, may be {@code null}
 */
public record ManualRiskChange(
    String riskRating,
    Set<String> addTags,
    Set<String> removeTags,
    String justification,
    List<Long> evidenceAttachmentIds,
    Long matchId,
    Long caseId,
    String reference) {

  /** Defensive copies; null collections are empty. */
  public ManualRiskChange {
    addTags = addTags == null ? Set.of() : Set.copyOf(addTags);
    removeTags = removeTags == null ? Set.of() : Set.copyOf(removeTags);
    evidenceAttachmentIds =
        evidenceAttachmentIds == null ? List.of() : List.copyOf(evidenceAttachmentIds);
  }
}
