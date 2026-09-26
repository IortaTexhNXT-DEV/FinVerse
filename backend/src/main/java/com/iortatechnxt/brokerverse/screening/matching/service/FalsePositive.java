package com.iortatechnxt.brokerverse.screening.matching.service;

import java.util.List;
import java.util.Set;

/**
 * "Mark False Positive" on a match (SNSRP-304; FR-SS-032, 035): the justification and the evidence,
 * and optionally the corrected risk rating and tags of the client.
 *
 * @param justification the justification, mandatory
 * @param evidenceAttachmentIds the evidence documents; empty = the documents attached to the match
 * @param riskRating a new rating ({@code KYC_RISK_RATING}), {@code null} to leave the profile
 * @param addTags tags to add with the new rating
 * @param removeTags tags to end with the new rating (e.g. WATCHLIST_REVIEW)
 * @param caseId the screening case holding the evidence, may be {@code null}
 */
public record FalsePositive(
    String justification,
    List<Long> evidenceAttachmentIds,
    String riskRating,
    Set<String> addTags,
    Set<String> removeTags,
    Long caseId) {

  /** Defensive copies; null collections are empty. */
  public FalsePositive {
    evidenceAttachmentIds =
        evidenceAttachmentIds == null ? List.of() : List.copyOf(evidenceAttachmentIds);
    addTags = addTags == null ? Set.of() : Set.copyOf(addTags);
    removeTags = removeTags == null ? Set.of() : Set.copyOf(removeTags);
  }

  /**
   * Whether the client's risk profile is changed as well.
   *
   * @return true when a rating or tags are given
   */
  public boolean changesProfile() {
    return riskRating != null && !riskRating.isBlank()
        || !addTags.isEmpty()
        || !removeTags.isEmpty();
  }
}
