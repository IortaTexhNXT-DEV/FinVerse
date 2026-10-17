package com.iortatechnxt.brokerverse.remittance.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;

/**
 * A change of the Remittance Settings (FRS BRD-02 FRID.RMT.003): extraction schedule, shared
 * folders per remittance type and file naming, proposed by a maker and approved or rejected by the
 * Remittance Team Leader. The latest approved change of a company is the settings in force.
 */
@Entity
@Table(name = "rem_setting_change")
public class RemittanceSettingChange {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "schedule_active", nullable = false, updatable = false)
  private boolean scheduleActive;

  @Column(name = "extraction_time", nullable = false, length = 5, updatable = false)
  private String extractionTime;

  @Column(name = "working_days_only", nullable = false, updatable = false)
  private boolean workingDaysOnly;

  @Column(name = "folder_normal_php", nullable = false, length = 250, updatable = false)
  private String folderNormalPhp;

  @Column(name = "folder_normal_usd", nullable = false, length = 250, updatable = false)
  private String folderNormalUsd;

  @Column(name = "folder_with_incentives", nullable = false, length = 250, updatable = false)
  private String folderWithIncentives;

  @Column(name = "file_pattern", nullable = false, length = 120, updatable = false)
  private String filePattern;

  @Column(name = "text_prefix", nullable = false, length = 10, updatable = false)
  private String textPrefix;

  @Column(nullable = false, length = 250, updatable = false)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status;

  @Column(name = "requested_by", nullable = false, length = 50, updatable = false)
  private String requestedBy;

  @Column(name = "requested_at", nullable = false, updatable = false)
  private Instant requestedAt;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_comment", length = 250)
  private String decisionComment;

  /** For JPA. */
  protected RemittanceSettingChange() {}

  /**
   * A proposed change.
   *
   * @param companyId company
   * @param values proposed settings
   * @param reason reason of the change
   * @param by maker
   * @param at time
   */
  public RemittanceSettingChange(
      Long companyId, Values values, String reason, String by, Instant at) {
    this.companyId = companyId;
    this.scheduleActive = values.scheduleActive();
    this.extractionTime = values.extractionTime();
    this.workingDaysOnly = values.workingDaysOnly();
    this.folderNormalPhp = values.folderNormalPhp();
    this.folderNormalUsd = values.folderNormalUsd();
    this.folderWithIncentives = values.folderWithIncentives();
    this.filePattern = values.filePattern();
    this.textPrefix = values.textPrefix();
    this.reason = reason;
    this.status = Status.PENDING;
    this.requestedBy = by;
    this.requestedAt = at;
  }

  /**
   * Approves or rejects the change; the maker cannot decide it (four eyes).
   *
   * @param approve true to approve
   * @param by checker
   * @param comment comment, may be null
   * @param at time
   */
  public void decide(boolean approve, String by, String comment, Instant at) {
    if (status != Status.PENDING) {
      throw new BusinessRuleException(
          "REMIT_SETTING_DECIDED", "This change of the settings is already " + status);
    }
    if (Objects.equals(by, requestedBy)) {
      throw new BusinessRuleException(
          "REMIT_SETTING_SAME_USER", "The maker of a change cannot approve or reject it");
    }
    this.status = approve ? Status.APPROVED : Status.REJECTED;
    this.decidedBy = by;
    this.decidedAt = at;
    this.decisionComment = comment;
  }

  /** Marks an approved change replaced by a newer approved one. */
  public void superseded() {
    if (status == Status.APPROVED) {
      this.status = Status.SUPERSEDED;
    }
  }

  /**
   * The proposed settings.
   *
   * @return values
   */
  public Values values() {
    return new Values(
        scheduleActive,
        extractionTime,
        workingDaysOnly,
        folderNormalPhp,
        folderNormalUsd,
        folderWithIncentives,
        filePattern,
        textPrefix);
  }

  public Long getId() {
    return id;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getReason() {
    return reason;
  }

  public Status getStatus() {
    return status;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public String getDecisionComment() {
    return decisionComment;
  }

  /** Status of a change. */
  public enum Status {
    /** Waiting for the Remittance Team Leader. */
    PENDING,
    /** In force. */
    APPROVED,
    /** Refused. */
    REJECTED,
    /** Was in force, replaced by a newer approved change. */
    SUPERSEDED
  }

  /**
   * The Remittance Settings.
   *
   * @param scheduleActive whether the scheduled extraction runs
   * @param extractionTime time of the scheduled extraction, HH:mm Philippine time
   * @param workingDaysOnly whether the extraction skips weekends and holidays
   * @param folderNormalPhp shared folder of the Normal - Peso files
   * @param folderNormalUsd shared folder of the Normal - Dollar files
   * @param folderWithIncentives shared folder of the With Incentives files
   * @param filePattern name of the Excel extract, with {date}, {insurer}, {settlement}, {type}
   * @param textPrefix prefix of the text extract name (AM01 in the specimen)
   */
  public record Values(
      boolean scheduleActive,
      String extractionTime,
      boolean workingDaysOnly,
      String folderNormalPhp,
      String folderNormalUsd,
      String folderWithIncentives,
      String filePattern,
      String textPrefix) {}
}
