package com.iortatechnxt.brokerverse.screening.matching.api.dto;

import com.iortatechnxt.brokerverse.screening.matching.service.FalsePositive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

/**
 * Body of "Mark False Positive" (FR-SS-032, 035).
 *
 * @param justification justification (mandatory; checked by the service with the FRS message)
 * @param evidenceAttachmentIds evidence documents; empty = the documents attached to the match
 * @param riskRating corrected rating, {@code null} to leave the client's profile
 * @param addTags tags to add
 * @param removeTags tags to end
 * @param caseId case holding the evidence
 */
public record FalsePositiveRequest(
    @Size(max = 2000) String justification,
    List<Long> evidenceAttachmentIds,
    String riskRating,
    Set<String> addTags,
    Set<String> removeTags,
    Long caseId) {

  /**
   * The service request.
   *
   * @return request
   */
  public FalsePositive toRequest() {
    return new FalsePositive(
        justification, evidenceAttachmentIds, riskRating, addTags, removeTags, caseId);
  }
}
