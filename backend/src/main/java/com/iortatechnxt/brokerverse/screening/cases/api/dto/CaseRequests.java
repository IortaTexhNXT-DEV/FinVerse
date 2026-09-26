package com.iortatechnxt.brokerverse.screening.cases.api.dto;

import com.iortatechnxt.brokerverse.screening.cases.service.CaseValidator.Decision;
import com.iortatechnxt.brokerverse.screening.matching.service.FalsePositive;
import com.iortatechnxt.brokerverse.screening.risk.service.ManualRiskChange;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The request bodies of the case actions (FR-SS-043, 050-052, 061-064, 035). Mandatory fields are
 * checked by the services with the FRS messages.
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class CaseRequests {

  private static final int TEXT = 4000;

  private CaseRequests() {}

  /**
   * Submit with a disposition (FR-SS-051).
   *
   * @param disposition INVESTIGATION disposition
   * @param recommendation recommendation
   * @param strRequired STR flag
   */
  public record Submit(
      @Size(max = 40) String disposition,
      @Size(max = TEXT) String recommendation,
      boolean strRequired) {

    /**
     * The service decision.
     *
     * @return decision
     */
    public Decision decision() {
      return new Decision(disposition, recommendation, strRequired);
    }
  }

  /**
   * Resubmit a returned case (FR-SS-062).
   *
   * @param response response to the return
   * @param disposition corrected disposition, blank to keep
   * @param recommendation corrected recommendation, blank to keep
   * @param strRequired STR flag
   */
  public record Resubmit(
      @Size(max = TEXT) String response,
      @Size(max = 40) String disposition,
      @Size(max = TEXT) String recommendation,
      boolean strRequired) {

    /**
     * The service decision.
     *
     * @return decision
     */
    public Decision decision() {
      return new Decision(disposition, recommendation, strRequired);
    }
  }

  /**
   * A decision step: unit head decision, Compliance outcome, re-open (FR-SS-061, 063, 040).
   *
   * @param disposition the disposition of the stage
   * @param reasonCode the return reason
   * @param remarks rationale, remarks or comment
   */
  public record Step(
      @Size(max = 40) String disposition,
      @Size(max = 40) String reasonCode,
      @Size(max = TEXT) String remarks) {}

  /**
   * A committee vote (FR-SS-064).
   *
   * @param decision APPROVE_STR, NO_STR or COMMITTEE_RETURN
   * @param remarks remarks
   */
  public record Vote(@Size(max = 40) String decision, @Size(max = TEXT) String remarks) {}

  /**
   * Re-assignment (FR-SS-043).
   *
   * @param assignee the new assignee
   * @param reasonCode reason
   * @param comment comment
   */
  public record Reassign(
      @Size(max = 50) String assignee,
      @Size(max = 40) String reasonCode,
      @Size(max = 1000) String comment) {}

  /**
   * The answers of a review draft (FR-SS-050).
   *
   * @param values raw values by field code
   */
  public record Review(Map<String, String> values) {

    /** Null is empty. */
    public Review {
      values = values == null ? Map.of() : values;
    }
  }

  /**
   * Confirm a match (FR-SS-032).
   *
   * @param remarks remarks
   */
  public record Confirm(@Size(max = 2000) String remarks) {}

  /**
   * Clear a match of the case (FR-SS-035).
   *
   * @param justification justification
   * @param evidenceAttachmentIds evidence, empty for the case documents
   * @param riskRating corrected rating
   * @param addTags tags to add
   * @param removeTags tags to end
   */
  public record ClearMatch(
      @Size(max = 2000) String justification,
      List<Long> evidenceAttachmentIds,
      String riskRating,
      Set<String> addTags,
      Set<String> removeTags) {

    /**
     * The service request.
     *
     * @param caseId the case
     * @return request
     */
    public FalsePositive toRequest(Long caseId) {
      return new FalsePositive(
          justification, evidenceAttachmentIds, riskRating, addTags, removeTags, caseId);
    }
  }

  /**
   * Update Risk Tag on the case (FR-SS-035).
   *
   * @param riskRating new rating
   * @param addTags tags to add
   * @param removeTags tags to end
   * @param justification justification
   * @param evidenceAttachmentIds evidence, empty for the case documents
   * @param matchId the match cleared, may be null
   */
  public record RiskTag(
      String riskRating,
      Set<String> addTags,
      Set<String> removeTags,
      @Size(max = 2000) String justification,
      List<Long> evidenceAttachmentIds,
      Long matchId) {

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
          null,
          null);
    }
  }
}
