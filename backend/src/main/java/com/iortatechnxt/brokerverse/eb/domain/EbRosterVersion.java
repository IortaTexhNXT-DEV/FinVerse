package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A version of the member roster of a programme and policy year (BRID-013, 014; FR-EB-054), loaded
 * from the master list through the bulk handler {@code EB_MASTERLIST} as STAGED; the AO reviews the
 * differences with the accepted version and accepts it (the earlier accepted version is
 * superseded and stays readable) or rejects it.
 */
@Entity
@Table(name = "eb_roster_version")
public class EbRosterVersion extends BaseEntity {

  /** Status of a roster version. */
  public enum Status {
    /** Loaded, waiting for the AO. */
    STAGED,
    /** The roster in force. */
    ACCEPTED,
    /** Refused by the AO. */
    REJECTED,
    /** Replaced by a later accepted version. */
    SUPERSEDED
  }

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "programme_id", nullable = false, updatable = false)
  private Long programmeId;

  @Column(name = "policy_year", nullable = false, updatable = false)
  private int policyYear;

  @Column(name = "version_no", nullable = false, updatable = false)
  private int versionNo;

  @Column(name = "source_ref", nullable = false, length = 40, updatable = false)
  private String sourceRef;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.STAGED;

  @Column(nullable = false)
  private int headcount;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "reject_reason", length = 500)
  private String rejectReason;

  protected EbRosterVersion() {}

  /**
   * Creates a staged version.
   *
   * @param programme programme
   * @param policyYear policy year
   * @param versionNo version, from 1
   * @param sourceRef upload number (or the member change applied)
   */
  public EbRosterVersion(EbProgramme programme, int policyYear, int versionNo, String sourceRef) {
    this.companyId = programme.getCompanyId();
    this.programmeId = programme.getId();
    this.policyYear = policyYear;
    this.versionNo = versionNo;
    this.sourceRef = sourceRef;
  }

  /** A member was added to the staged version. */
  public void counted() {
    headcount++;
  }

  /**
   * Accepts the staged version.
   *
   * @param by user
   * @param at time
   */
  public void accept(String by, Instant at) {
    requireStaged();
    this.status = Status.ACCEPTED;
    this.decidedBy = by;
    this.decidedAt = at;
  }

  /**
   * Rejects the staged version.
   *
   * @param reason why
   * @param by user
   * @param at time
   */
  public void reject(String reason, String by, Instant at) {
    requireStaged();
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException(
          "EB_ROSTER_REASON_REQUIRED", "Enter why the master list is rejected");
    }
    this.status = Status.REJECTED;
    this.rejectReason = reason.strip();
    this.decidedBy = by;
    this.decidedAt = at;
  }

  /** A later version was accepted. */
  public void supersede() {
    this.status = Status.SUPERSEDED;
  }

  /**
   * Sets the headcount of the accepted roster after a member change.
   *
   * @param count active members
   */
  public void recount(int count) {
    this.headcount = count;
  }

  private void requireStaged() {
    if (status != Status.STAGED) {
      throw new BusinessRuleException(
          "EB_ROSTER_NOT_STAGED", "Roster version " + versionNo + " is already decided");
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getProgrammeId() {
    return programmeId;
  }

  public int getPolicyYear() {
    return policyYear;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public Status getStatus() {
    return status;
  }

  public int getHeadcount() {
    return headcount;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public String getRejectReason() {
    return rejectReason;
  }
}
