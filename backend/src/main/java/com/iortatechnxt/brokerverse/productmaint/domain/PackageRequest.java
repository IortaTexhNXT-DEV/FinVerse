package com.iortatechnxt.brokerverse.productmaint.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * A package request (BRPM.008-017; design section 4.4): the Package Request Form with its client or
 * programme, line, cover type, target product, reason and requested terms; the approvals of
 * Marketing, the TSU Team Lead and the TSU Head; the proposed terms after negotiation; and the
 * catalog version the MBS set-up produced. The status mirrors the PM_PACKAGE_REQUEST work case.
 * Terms are JSON ({@link PackageTerms}) written by the service's codec.
 */
@Entity
@Table(name = "pm_request")
public class PackageRequest extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "request_no", nullable = false, length = 30, updatable = false)
  private String requestNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "request_type", nullable = false, length = 20, updatable = false)
  private RequestType requestType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RequestScope scope;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(name = "client_id")
  private Long clientId;

  @Column(name = "client_code", length = 30)
  private String clientCode;

  @Column(name = "client_name", length = 250)
  private String clientName;

  @Column(name = "line_code", nullable = false, length = 30)
  private String lineCode;

  @Column(name = "cover_type_code", length = 30)
  private String coverTypeCode;

  @Column(name = "target_product_code", length = 20)
  private String targetProductCode;

  @Column(name = "base_version_no")
  private Integer baseVersionNo;

  @Column(name = "market_segments", length = 200)
  private String marketSegments;

  @Column(nullable = false, length = 40)
  private String reason;

  @Column(name = "reason_note", length = 500)
  private String reasonNote;

  @Column(name = "negotiation_required", nullable = false)
  private boolean negotiationRequired = true;

  @Column(name = "requested_terms", nullable = false, columnDefinition = "text")
  private String requestedTerms;

  @Column(length = 2000)
  private String recommendation;

  @Column(name = "proposed_terms", columnDefinition = "text")
  private String proposedTerms;

  @Column(name = "chosen_insurers", length = 300)
  private String chosenInsurers;

  @Column(name = "package_end_date")
  private LocalDate packageEndDate;

  @Column(name = "scheme_rate", precision = 19, scale = 8)
  private BigDecimal schemeRate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private RequestStage status = RequestStage.DRAFT;

  @Embedded private RequestMilestones milestones = new RequestMilestones();

  @Column(name = "resulting_version_no")
  private Integer resultingVersionNo;

  @Column(name = "released_at")
  private Instant releasedAt;

  protected PackageRequest() {}

  /**
   * Creates a request (not yet saved).
   *
   * @param companyId company
   * @param requestNo request number (PKR-yyyy-n)
   * @param requestType type
   */
  public PackageRequest(Long companyId, String requestNo, RequestType requestType) {
    this.companyId = companyId;
    this.requestNo = requestNo;
    this.requestType = requestType;
    this.negotiationRequired = requestType.negotiatesByDefault();
  }

  /**
   * Sets the form fields (BRPM.008).
   *
   * @param form header of the form
   * @param termsJson requested terms as JSON
   */
  public void describe(RequestForm form, String termsJson) {
    this.scope = form.scope();
    this.title = form.title();
    this.clientId = form.client() == null ? null : form.client().id();
    this.clientCode = form.client() == null ? null : form.client().code();
    this.clientName = form.client() == null ? null : form.client().name();
    this.lineCode = form.lineCode();
    this.coverTypeCode = form.coverTypeCode();
    this.targetProductCode = form.productCode();
    this.baseVersionNo = form.baseVersionNo();
    this.marketSegments =
        form.marketSegments().isEmpty() ? null : String.join(",", form.marketSegments());
    this.reason = form.reason();
    this.reasonNote = form.reasonNote();
    this.negotiationRequired = form.negotiationRequired();
    this.requestedTerms = termsJson;
  }

  /**
   * Mirrors key facts of the terms for lists and reports.
   *
   * @param endDate package end date
   * @param rate scheme rate
   */
  public void summarise(LocalDate endDate, BigDecimal rate) {
    this.packageEndDate = endDate;
    this.schemeRate = rate;
  }

  /**
   * Records the TSU Team Lead recommendation text (BRPM.009); the milestone is kept apart.
   *
   * @param text recommendation
   */
  public void recommend(String text) {
    this.recommendation = text;
  }

  /**
   * Sets the proposed terms and chosen insurers (terms final, requirements).
   *
   * @param termsJson proposed terms as JSON
   * @param insurers chosen insurer codes
   */
  public void propose(String termsJson, List<String> insurers) {
    this.proposedTerms = termsJson;
    this.chosenInsurers = insurers.isEmpty() ? null : String.join(",", insurers);
  }

  /**
   * Records the MBS set-up and the resulting catalog version.
   *
   * @param productCode product the version belongs to
   * @param versionNo DRAFT version number
   */
  public void markSetUp(String productCode, int versionNo) {
    this.targetProductCode = productCode;
    this.resultingVersionNo = versionNo;
  }

  /**
   * Records the release of the resulting version (catalog event).
   *
   * @param versionNo released version
   * @param when time
   */
  public void markReleased(int versionNo, Instant when) {
    this.resultingVersionNo = versionNo;
    this.releasedAt = when;
  }

  /**
   * Mirrors the work case stage.
   *
   * @param stage stage
   */
  public void markStatus(RequestStage stage) {
    this.status = stage;
  }

  /**
   * The market segments.
   *
   * @return codes
   */
  public List<String> getMarketSegmentList() {
    return marketSegments == null || marketSegments.isBlank()
        ? List.of()
        : Arrays.stream(marketSegments.split(",")).map(String::strip).toList();
  }

  /**
   * The chosen insurers.
   *
   * @return codes
   */
  public List<String> getChosenInsurerList() {
    return chosenInsurers == null || chosenInsurers.isBlank()
        ? List.of()
        : Arrays.stream(chosenInsurers.split(",")).map(String::strip).toList();
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRequestNo() {
    return requestNo;
  }

  public RequestType getRequestType() {
    return requestType;
  }

  public RequestScope getScope() {
    return scope;
  }

  public String getTitle() {
    return title;
  }

  public Long getClientId() {
    return clientId;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getClientName() {
    return clientName;
  }

  public String getLineCode() {
    return lineCode;
  }

  public String getCoverTypeCode() {
    return coverTypeCode;
  }

  public String getTargetProductCode() {
    return targetProductCode;
  }

  public Integer getBaseVersionNo() {
    return baseVersionNo;
  }

  public String getReason() {
    return reason;
  }

  public String getReasonNote() {
    return reasonNote;
  }

  public boolean isNegotiationRequired() {
    return negotiationRequired;
  }

  public String getRequestedTerms() {
    return requestedTerms;
  }

  public String getRecommendation() {
    return recommendation;
  }

  public String getProposedTerms() {
    return proposedTerms;
  }

  public LocalDate getPackageEndDate() {
    return packageEndDate;
  }

  public BigDecimal getSchemeRate() {
    return schemeRate;
  }

  public RequestStage getStatus() {
    return status;
  }

  /**
   * Who moved the request through its steps (Hibernate leaves an all-empty embeddable null).
   *
   * @return milestones
   */
  public RequestMilestones getMilestones() {
    if (milestones == null) {
      milestones = new RequestMilestones();
    }
    return milestones;
  }

  public Integer getResultingVersionNo() {
    return resultingVersionNo;
  }

  public Instant getReleasedAt() {
    return releasedAt;
  }

  /**
   * Header of the Package Request Form.
   *
   * @param scope generic or client-specific
   * @param title package / programme name
   * @param client client of a client-specific package, null for a generic programme
   * @param lineCode product line
   * @param coverTypeCode cover type or subtype
   * @param productCode target product (null for NEW)
   * @param baseVersionNo version the request starts from
   * @param marketSegments market segments
   * @param reason reason (list PKG_REQUEST_REASON)
   * @param reasonNote comment on the reason
   * @param negotiationRequired whether insurers are approached
   */
  public record RequestForm(
      RequestScope scope,
      String title,
      ClientRef client,
      String lineCode,
      String coverTypeCode,
      String productCode,
      Integer baseVersionNo,
      List<String> marketSegments,
      String reason,
      String reasonNote,
      boolean negotiationRequired) {

    /** Defensive copy. */
    public RequestForm {
      marketSegments = marketSegments == null ? List.of() : List.copyOf(marketSegments);
    }
  }

  /**
   * The client of a client-specific package (CRM look-up).
   *
   * @param id client id
   * @param code client or prospect code
   * @param name display name
   */
  public record ClientRef(Long id, String code, String name) {}
}
