package com.iortatechnxt.brokerverse.productmaint.api.dto;

import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestMilestones;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestScope;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestType;
import com.iortatechnxt.brokerverse.productmaint.service.PackageQueryService;
import com.iortatechnxt.brokerverse.productmaint.service.PackageQueryService.CaseFacts;
import com.iortatechnxt.brokerverse.productmaint.service.PackageRequestService.Prefill;
import com.iortatechnxt.brokerverse.productmaint.service.RequestDraft;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request and response bodies of the package request endpoints (BRPM.008-011). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class RequestDtos {

  private RequestDtos() {}

  /**
   * The Package Request Form as entered.
   *
   * @param companyId company (creation only)
   * @param type request type
   * @param scope scope
   * @param title package / programme name
   * @param clientId client of a client-specific package
   * @param lineCode product line
   * @param coverTypeCode cover type
   * @param productCode target product
   * @param baseVersionNo base version, null for the current one
   * @param marketSegments market segments
   * @param reason reason
   * @param reasonNote comment on the reason
   * @param negotiationRequired insurers approached (RENEW / UPDATE / REACTIVATE)
   * @param terms requested terms and target insurers
   */
  public record FormBody(
      Long companyId,
      RequestType type,
      RequestScope scope,
      @NotBlank @Size(max = 200) String title,
      Long clientId,
      @NotBlank @Size(max = 30) String lineCode,
      @Size(max = 30) String coverTypeCode,
      @Size(max = 20) String productCode,
      Integer baseVersionNo,
      List<String> marketSegments,
      @NotBlank @Size(max = 40) String reason,
      @Size(max = 500) String reasonNote,
      Boolean negotiationRequired,
      PackageTerms terms) {

    /**
     * The service draft.
     *
     * @return draft
     */
    public RequestDraft toDraft() {
      return new RequestDraft(
          type,
          scope,
          title,
          clientId,
          lineCode,
          coverTypeCode,
          productCode,
          baseVersionNo,
          marketSegments,
          reason,
          reasonNote,
          negotiationRequired,
          terms);
    }
  }

  /**
   * Filters of the package request list.
   *
   * @param companyId company
   * @param text request number, title, client or product fragment
   * @param stage stages
   * @param type types
   * @param scope scope
   * @param mine only the current user's requests
   * @param productCode target product
   * @param expiringWithin package end date within n days
   */
  public record SearchParams(
      Long companyId,
      String text,
      List<RequestStage> stage,
      List<RequestType> type,
      RequestScope scope,
      Boolean mine,
      String productCode,
      Integer expiringWithin) {

    /**
     * As service criteria.
     *
     * @param maker the current user when only their requests are wanted, else null
     * @return criteria
     */
    public PackageQueryService.Search toSearch(String maker) {
      return new PackageQueryService.Search(
          companyId, text, stage, type, scope, maker, productCode, expiringWithin);
    }
  }

  /**
   * A recommendation of the TSU Team Lead.
   *
   * @param text recommendation
   */
  public record RecommendBody(@NotBlank @Size(max = 2000) String text) {}

  /**
   * A row of the package request list.
   *
   * @param id id
   * @param requestNo request number
   * @param requestType type
   * @param scope scope
   * @param title package / programme name
   * @param clientName client of a client-specific package
   * @param lineCode line
   * @param productCode target product
   * @param resultingVersionNo version set up
   * @param packageEndDate package end date
   * @param status stage
   * @param stageEnteredAt stage since
   * @param dueAt SLA due time
   * @param assignee assignee
   * @param createdBy maker
   * @param createdAt created
   */
  public record ListItem(
      Long id,
      String requestNo,
      RequestType requestType,
      RequestScope scope,
      String title,
      String clientName,
      String lineCode,
      String productCode,
      Integer resultingVersionNo,
      LocalDate packageEndDate,
      RequestStage status,
      Instant stageEnteredAt,
      Instant dueAt,
      String assignee,
      String createdBy,
      Instant createdAt) {

    /**
     * Maps a request with its work case facts.
     *
     * @param p request
     * @param c work case facts, null when unknown
     * @return row
     */
    public static ListItem from(PackageRequest p, CaseFacts c) {
      return new ListItem(
          p.getId(),
          p.getRequestNo(),
          p.getRequestType(),
          p.getScope(),
          p.getTitle(),
          p.getClientName(),
          p.getLineCode(),
          p.getTargetProductCode(),
          p.getResultingVersionNo(),
          p.getPackageEndDate(),
          p.getStatus(),
          c == null ? null : c.stageEnteredAt(),
          c == null ? null : c.dueAt(),
          c == null ? null : c.assignee(),
          p.getCreatedBy(),
          p.getCreatedAt());
    }
  }

  /**
   * One package request.
   *
   * @param id id
   * @param companyId company
   * @param requestNo request number
   * @param requestType type
   * @param scope scope
   * @param title package / programme name
   * @param clientId client id
   * @param clientCode client code
   * @param clientName client name
   * @param lineCode line
   * @param coverTypeCode cover type
   * @param productCode target product
   * @param baseVersionNo base version
   * @param marketSegments market segments
   * @param reason reason
   * @param reasonNote comment on the reason
   * @param negotiationRequired whether insurers are approached
   * @param recommendation TSU Team Lead recommendation
   * @param requestedTerms requested terms
   * @param proposedTerms proposed terms, null before terms final
   * @param chosenInsurers chosen insurers
   * @param packageEndDate package end date
   * @param schemeRate scheme rate
   * @param status stage
   * @param milestones who did what and when
   * @param resultingVersionNo version set up
   * @param releasedAt release time
   * @param createdBy maker
   * @param createdAt created
   */
  public record RequestResponse(
      Long id,
      Long companyId,
      String requestNo,
      RequestType requestType,
      RequestScope scope,
      String title,
      Long clientId,
      String clientCode,
      String clientName,
      String lineCode,
      String coverTypeCode,
      String productCode,
      Integer baseVersionNo,
      List<String> marketSegments,
      String reason,
      String reasonNote,
      boolean negotiationRequired,
      String recommendation,
      PackageTerms requestedTerms,
      PackageTerms proposedTerms,
      List<String> chosenInsurers,
      LocalDate packageEndDate,
      BigDecimal schemeRate,
      RequestStage status,
      Milestones milestones,
      Integer resultingVersionNo,
      Instant releasedAt,
      String createdBy,
      Instant createdAt) {

    /**
     * Maps a request.
     *
     * @param p request
     * @param requested requested terms
     * @param proposed proposed terms, null when none
     * @return response
     */
    public static RequestResponse from(
        PackageRequest p, PackageTerms requested, PackageTerms proposed) {
      return new RequestResponse(
          p.getId(),
          p.getCompanyId(),
          p.getRequestNo(),
          p.getRequestType(),
          p.getScope(),
          p.getTitle(),
          p.getClientId(),
          p.getClientCode(),
          p.getClientName(),
          p.getLineCode(),
          p.getCoverTypeCode(),
          p.getTargetProductCode(),
          p.getBaseVersionNo(),
          p.getMarketSegmentList(),
          p.getReason(),
          p.getReasonNote(),
          p.isNegotiationRequired(),
          p.getRecommendation(),
          requested,
          proposed,
          p.getChosenInsurerList(),
          p.getPackageEndDate(),
          p.getSchemeRate(),
          p.getStatus(),
          Milestones.from(p.getMilestones()),
          p.getResultingVersionNo(),
          p.getReleasedAt(),
          p.getCreatedBy(),
          p.getCreatedAt());
    }
  }

  /**
   * Who moved the request through its steps and when.
   *
   * @param submittedBy submitter
   * @param submittedAt submitted
   * @param approvedBy Marketing approver
   * @param approvedAt approved
   * @param recommendedBy TSU Team Lead
   * @param recommendedAt recommended
   * @param tsuApprovedBy TSU Head
   * @param tsuApprovedAt TSU approved
   * @param termsFinalBy terms final by
   * @param termsFinalAt terms final
   * @param requirementsBy requirements submitted by
   * @param requirementsAt requirements submitted
   * @param setupBy MBS user
   * @param setupAt set up
   */
  public record Milestones(
      String submittedBy,
      Instant submittedAt,
      String approvedBy,
      Instant approvedAt,
      String recommendedBy,
      Instant recommendedAt,
      String tsuApprovedBy,
      Instant tsuApprovedAt,
      String termsFinalBy,
      Instant termsFinalAt,
      String requirementsBy,
      Instant requirementsAt,
      String setupBy,
      Instant setupAt) {

    /**
     * Maps the milestones.
     *
     * @param m milestones
     * @return view
     */
    public static Milestones from(RequestMilestones m) {
      return new Milestones(
          m.getSubmittedBy(),
          m.getSubmittedAt(),
          m.getApprovedBy(),
          m.getApprovedAt(),
          m.getRecommendedBy(),
          m.getRecommendedAt(),
          m.getTsuApprovedBy(),
          m.getTsuApprovedAt(),
          m.getTermsFinalBy(),
          m.getTermsFinalAt(),
          m.getRequirementsBy(),
          m.getRequirementsAt(),
          m.getSetupBy(),
          m.getSetupAt());
    }
  }

  /**
   * Terms pre-filled from a package version.
   *
   * @param productCode product
   * @param versionNo version the terms come from
   * @param versionStatus its status
   * @param terms terms
   */
  public record PrefillResponse(
      String productCode, int versionNo, String versionStatus, PackageTerms terms) {

    /**
     * Maps a pre-fill.
     *
     * @param p pre-fill
     * @return response
     */
    public static PrefillResponse from(Prefill p) {
      return new PrefillResponse(p.productCode(), p.versionNo(), p.versionStatus(), p.terms());
    }
  }

  /**
   * A generic comment.
   *
   * @param comment comment
   */
  public record CommentBody(@Size(max = 1000) String comment) {

    /**
     * The comment, null when blank.
     *
     * @return comment
     */
    public String text() {
      return comment == null || comment.isBlank() ? null : comment.strip();
    }
  }

  /**
   * A reason code and comment.
   *
   * @param reasonCode reason (list of the transition)
   * @param comment comment
   */
  public record ReasonBody(
      @NotNull @Size(max = 40) String reasonCode, @Size(max = 1000) String comment) {}
}
