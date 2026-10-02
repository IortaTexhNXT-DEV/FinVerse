package com.iortatechnxt.brokerverse.screening.risk.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.TreeSet;

/**
 * One change of a client's risk profile made by screening (SNSRP-302, 304; FR-SS-033, 035): the
 * category, the rating before and after, the tags added and ended, the source (RULE with the rule,
 * the match and the run; MANUAL with the justification and the evidence) and the time. Insert-only
 * history ({@code scr_client_risk_profile}); the client master itself is changed through {@code
 * crm.service.ClientRiskService}.
 */
@Entity
@Table(name = "scr_client_risk_profile")
public class RiskProfileEntry extends BaseEntity {

  private static final int MAX_TEXT = 2000;
  private static final int MAX_LIST = 200;
  private static final int MAX_EVIDENCE = 500;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "client_id", nullable = false, updatable = false)
  private Long clientId;

  @Column(name = "client_code", nullable = false, length = 30, updatable = false)
  private String clientCode;

  @Column(name = "category_code", length = 30, updatable = false)
  private String categoryCode;

  @Column(name = "previous_rating", length = 30, updatable = false)
  private String previousRating;

  @Column(name = "kyc_risk_rating", length = 30, updatable = false)
  private String kycRiskRating;

  @Column(name = "tags_added", nullable = false, length = MAX_LIST, updatable = false)
  private String tagsAdded;

  @Column(name = "tags_removed", nullable = false, length = MAX_LIST, updatable = false)
  private String tagsRemoved;

  @Column(name = "active_tags", nullable = false, length = MAX_LIST, updatable = false)
  private String activeTags;

  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false, length = 10, updatable = false)
  private RiskSource source;

  @Column(name = "risk_version_id", updatable = false)
  private Long riskVersionId;

  @Column(name = "rule_id", updatable = false)
  private Long ruleId;

  @Column(name = "match_id", updatable = false)
  private Long matchId;

  @Column(name = "run_id", updatable = false)
  private Long runId;

  @Column(name = "justification", length = MAX_TEXT, updatable = false)
  private String justification;

  @Column(name = "evidence", length = MAX_EVIDENCE, updatable = false)
  private String evidence;

  @Column(name = "evidence_case_id", updatable = false)
  private Long evidenceCaseId;

  @Column(name = "kyc_review_due", updatable = false)
  private LocalDate kycReviewDue;

  @Column(name = "effective_at", nullable = false, updatable = false)
  private Instant effectiveAt;

  /** For JPA. */
  protected RiskProfileEntry() {}

  /**
   * Records a change.
   *
   * @param client the client and its profile after the change
   * @param cause source, category, rule, match, run and justification
   * @param effectiveAt when the change took effect
   */
  public RiskProfileEntry(ClientChange client, Cause cause, Instant effectiveAt) {
    this.companyId = client.companyId();
    this.clientId = client.clientId();
    this.clientCode = client.clientCode();
    this.previousRating = client.previousRating();
    this.kycRiskRating = client.riskRating();
    this.tagsAdded = join(client.tagsAdded());
    this.tagsRemoved = join(client.tagsRemoved());
    this.activeTags = join(client.activeTags());
    this.kycReviewDue = client.kycReviewDue();
    this.source = cause.source();
    this.categoryCode = cause.categoryCode();
    this.riskVersionId = cause.riskVersionId();
    this.ruleId = cause.ruleId();
    this.matchId = cause.matchId();
    this.runId = cause.runId();
    this.justification = cap(cause.justification(), MAX_TEXT);
    this.evidence = cap(cause.evidence(), MAX_EVIDENCE);
    this.evidenceCaseId = cause.evidenceCaseId();
    this.effectiveAt = effectiveAt;
  }

  private static String join(Collection<String> values) {
    return cap(String.join(",", new TreeSet<>(values)), MAX_LIST);
  }

  private static String cap(String value, int max) {
    return value == null || value.length() <= max ? value : value.substring(0, max);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getClientId() {
    return clientId;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getCategoryCode() {
    return categoryCode;
  }

  public String getPreviousRating() {
    return previousRating;
  }

  public String getKycRiskRating() {
    return kycRiskRating;
  }

  public String getTagsAdded() {
    return tagsAdded;
  }

  public String getTagsRemoved() {
    return tagsRemoved;
  }

  public String getActiveTags() {
    return activeTags;
  }

  public RiskSource getSource() {
    return source;
  }

  public Long getRiskVersionId() {
    return riskVersionId;
  }

  public Long getRuleId() {
    return ruleId;
  }

  public Long getMatchId() {
    return matchId;
  }

  public Long getRunId() {
    return runId;
  }

  public String getJustification() {
    return justification;
  }

  public String getEvidence() {
    return evidence;
  }

  public Long getEvidenceCaseId() {
    return evidenceCaseId;
  }

  public LocalDate getKycReviewDue() {
    return kycReviewDue;
  }

  public Instant getEffectiveAt() {
    return effectiveAt;
  }

  /**
   * The client's profile after the change.
   *
   * @param companyId company
   * @param clientId client
   * @param clientCode client code
   * @param previousRating rating before
   * @param riskRating rating after
   * @param tagsAdded tags added
   * @param tagsRemoved tags ended
   * @param activeTags active tags after the change
   * @param kycReviewDue next KYC review date after the change
   */
  public record ClientChange(
      Long companyId,
      Long clientId,
      String clientCode,
      String previousRating,
      String riskRating,
      Collection<String> tagsAdded,
      Collection<String> tagsRemoved,
      Collection<String> activeTags,
      LocalDate kycReviewDue) {}

  /**
   * Why the profile changed.
   *
   * @param source RULE or MANUAL
   * @param categoryCode risk category, may be {@code null} for a manual change
   * @param riskVersionId RISK_RULES version (RULE)
   * @param ruleId rule (RULE)
   * @param matchId match the rule held on or the manual change cleared
   * @param runId screening run (RULE)
   * @param justification justification (MANUAL) or rule description (RULE)
   * @param evidence evidence attachment ids, comma separated (MANUAL)
   * @param evidenceCaseId the case holding the evidence (MANUAL), may be {@code null}
   */
  public record Cause(
      RiskSource source,
      String categoryCode,
      Long riskVersionId,
      Long ruleId,
      Long matchId,
      Long runId,
      String justification,
      String evidence,
      Long evidenceCaseId) {}
}
