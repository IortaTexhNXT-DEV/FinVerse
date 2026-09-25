package com.iortatechnxt.brokerverse.screening.config.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * One version of a screening configuration set (SNSRP-101-109; design 4.1). Maker-checker: a DRAFT
 * is edited by the maker, submitted (PENDING), then approved (ACTIVE from its effective date) or
 * rejected with a reason. An ACTIVE version is never edited; it becomes SUPERSEDED when a newer
 * version of the same type and scope takes effect. The maker never decides (SNSRP-109).
 */
@Entity
@Table(name = "scr_config_version")
public class ConfigVersion extends BaseEntity {

  /** Reason recorded when the maker discards a draft (FR-SS-010). */
  public static final String WITHDRAWN = "withdrawn by maker";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Enumerated(EnumType.STRING)
  @Column(name = "config_type", nullable = false, length = 30, updatable = false)
  private ConfigType configType;

  @Column(name = "scope", nullable = false, length = 30, updatable = false)
  private String scope;

  @Column(name = "version_no", nullable = false, updatable = false)
  private int versionNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ConfigStatus status = ConfigStatus.DRAFT;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "change_note", length = 1000)
  private String changeNote;

  @Column(name = "base_version_id")
  private Long baseVersionId;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_reason", length = 1000)
  private String decisionReason;

  @Column(name = "diff", columnDefinition = "text")
  private String diff;

  /** For JPA. */
  protected ConfigVersion() {}

  /**
   * Creates a draft.
   *
   * @param companyId company
   * @param configType type
   * @param scope template type for TEMPLATE, otherwise {@code null}
   * @param versionNo next number of the type and scope
   * @param effectiveFrom proposed effective date
   * @param baseVersionId the version the draft was copied from, may be {@code null}
   */
  public ConfigVersion(
      Long companyId,
      ConfigType configType,
      String scope,
      int versionNo,
      LocalDate effectiveFrom,
      Long baseVersionId) {
    this.companyId = companyId;
    this.configType = configType;
    this.scope = scope == null ? "" : scope;
    this.versionNo = versionNo;
    this.effectiveFrom = effectiveFrom;
    this.baseVersionId = baseVersionId;
  }

  /**
   * Refuses a change unless the version is a draft (an ACTIVE version is never edited).
   *
   * @throws BusinessRuleException SCR_CONFIG_NOT_DRAFT
   */
  public void requireDraft() {
    if (status != ConfigStatus.DRAFT) {
      throw new BusinessRuleException(
          "SCR_CONFIG_NOT_DRAFT",
          "Version "
              + versionNo
              + " is "
              + status
              + "; only a draft can be changed. Create a new draft instead");
    }
  }

  /**
   * Changes the header of a draft.
   *
   * @param newEffectiveFrom effective date
   * @param note change note
   */
  public void editHeader(LocalDate newEffectiveFrom, String note) {
    requireDraft();
    this.effectiveFrom = Objects.requireNonNull(newEffectiveFrom);
    this.changeNote = note;
  }

  /**
   * Submits the draft for approval with its difference against the version in force.
   *
   * @param maker submitting user
   * @param when time
   * @param comparedWith the version in force the difference was computed against, may be null
   * @param difference the difference (JSON)
   */
  public void submit(String maker, Instant when, Long comparedWith, String difference) {
    requireDraft();
    this.status = ConfigStatus.PENDING;
    this.submittedBy = maker;
    this.submittedAt = when;
    this.baseVersionId = comparedWith;
    this.diff = difference;
  }

  /**
   * Approves the version (SNSRP-109): it becomes ACTIVE from its effective date.
   *
   * @param checker approving user (never the maker)
   * @param when time
   * @param today business date; a past effective date moves to it
   */
  public void approve(String checker, Instant when, LocalDate today) {
    requirePending();
    requireNotMaker(checker);
    this.status = ConfigStatus.ACTIVE;
    this.decidedBy = checker;
    this.decidedAt = when;
    if (effectiveFrom.isBefore(today)) {
      this.effectiveFrom = today;
    }
  }

  /**
   * Rejects the version with a reason; the version in force stays.
   *
   * @param checker rejecting user (never the maker)
   * @param when time
   * @param reason mandatory reason
   */
  public void reject(String checker, Instant when, String reason) {
    requirePending();
    requireNotMaker(checker);
    decide(checker, when, reason);
  }

  /**
   * The maker discards the draft; it is kept as REJECTED "withdrawn by maker" (FR-SS-010).
   *
   * @param maker user
   * @param when time
   */
  public void withdraw(String maker, Instant when) {
    requireDraft();
    decide(maker, when, WITHDRAWN);
  }

  private void decide(String user, Instant when, String reason) {
    this.status = ConfigStatus.REJECTED;
    this.decidedBy = user;
    this.decidedAt = when;
    this.decisionReason = reason;
  }

  /** Marks an ACTIVE version as replaced by a newer version in force. */
  public void supersede() {
    if (status == ConfigStatus.ACTIVE) {
      this.status = ConfigStatus.SUPERSEDED;
    }
  }

  /**
   * Refuses a decision unless the version is PENDING and the checker is not its maker.
   *
   * @param checker deciding user
   */
  public void requireDecidableBy(String checker) {
    requirePending();
    requireNotMaker(checker);
  }

  private void requirePending() {
    if (status != ConfigStatus.PENDING) {
      throw new BusinessRuleException(
          "SCR_CONFIG_NOT_PENDING", "Version " + versionNo + " is not waiting for approval");
    }
  }

  private void requireNotMaker(String checker) {
    if (isMaker(checker)) {
      throw new BusinessRuleException(
          "SCR_CONFIG_MAKER_APPROVES",
          "A configuration change is approved by someone other than its maker");
    }
  }

  /**
   * Whether a user made the version (created or submitted it).
   *
   * @param user user
   * @return true for the maker
   */
  public boolean isMaker(String user) {
    return CurrentUser.sameUser(user, getCreatedBy()) || CurrentUser.sameUser(user, submittedBy);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public ConfigType getConfigType() {
    return configType;
  }

  /**
   * The scope: the template type for TEMPLATE versions.
   *
   * @return the scope, {@code null} for the other types
   */
  public String getScope() {
    return scope == null || scope.isEmpty() ? null : scope;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public ConfigStatus getStatus() {
    return status;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public String getChangeNote() {
    return changeNote;
  }

  public Long getBaseVersionId() {
    return baseVersionId;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public String getDecisionReason() {
    return decisionReason;
  }

  public String getDiff() {
    return diff;
  }
}
