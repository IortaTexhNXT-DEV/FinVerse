package com.iortatechnxt.brokerverse.migration.cutover.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A criterion of a decommissioning checklist (DATA_MIGRATION_DESIGN section 17.6; FR-DM-123): per
 * legacy system (milestone SYSTEM) or for the legacy context in BIBS (milestone CONTEXT), with its
 * evidence, status and signature.
 */
@Entity
@Table(name = "mig_decommission_item")
public class DecommissionItem extends BaseEntity {

  /** Status of a criterion. */
  public enum Status {
    OPEN,
    MET,
    SIGNED,
    NOT_APPLICABLE
  }

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "system_code", nullable = false, length = 10, updatable = false)
  private String systemCode;

  @Column(nullable = false, length = 10, updatable = false)
  private String milestone;

  @Column(nullable = false, length = 40, updatable = false)
  private String criterion;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 500)
  private String description;

  @Column(length = 1000)
  private String evidence;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private Status status = Status.OPEN;

  @Column(name = "signed_by", length = 50)
  private String signedBy;

  @Column(name = "signed_at")
  private Instant signedAt;

  protected DecommissionItem() {}

  /**
   * A new criterion.
   *
   * @param companyId company
   * @param systemCode legacy system (or BIBS for the context milestone)
   * @param milestone SYSTEM or CONTEXT
   * @param template criterion code, name and description
   */
  public DecommissionItem(Long companyId, String systemCode, String milestone, Template template) {
    this.companyId = companyId;
    this.systemCode = systemCode;
    this.milestone = milestone;
    this.criterion = template.criterion();
    this.name = template.name();
    this.description = template.description();
  }

  /**
   * Records evidence and status.
   *
   * @param newStatus status
   * @param text evidence
   * @param user user
   * @param when time
   */
  public void update(Status newStatus, String text, String user, Instant when) {
    this.status = newStatus;
    this.evidence = text;
    if (newStatus == Status.SIGNED) {
      this.signedBy = user;
      this.signedAt = when;
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getSystemCode() {
    return systemCode;
  }

  public String getMilestone() {
    return milestone;
  }

  public String getCriterion() {
    return criterion;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getEvidence() {
    return evidence;
  }

  public Status getStatus() {
    return status;
  }

  public String getSignedBy() {
    return signedBy;
  }

  public Instant getSignedAt() {
    return signedAt;
  }

  /**
   * A criterion of the checklist template.
   *
   * @param criterion code
   * @param name short name
   * @param description description
   */
  public record Template(String criterion, String name, String description) {}
}
