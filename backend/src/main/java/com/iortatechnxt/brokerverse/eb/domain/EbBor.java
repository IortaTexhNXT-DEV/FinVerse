package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A version of the client's signed Broker on Record for a cycle (BRID-008; FR-EB-031): the file,
 * the validator's checklist (signed by an authorised signatory, not blank, client name matches;
 * e-signature verification is a parked seam, EBQ06), the validity dates and the decision. Versions
 * are never changed after the decision; a rejected BOR is replaced by version n+1.
 */
@Entity
@Table(name = "eb_bor")
public class EbBor extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "programme_id", nullable = false, updatable = false)
  private Long programmeId;

  @Column(name = "cycle_id", nullable = false, updatable = false)
  private Long cycleId;

  @Column(name = "version_no", nullable = false, updatable = false)
  private int versionNo;

  @Column(name = "attachment_id", nullable = false, updatable = false)
  private Long attachmentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EbBorStatus status = EbBorStatus.UPLOADED;

  @Column(name = "signed_by_signatory")
  private Boolean signedBySignatory;

  @Column(name = "not_blank")
  private Boolean notBlank;

  @Column(name = "client_name_matches")
  private Boolean clientNameMatches;

  @Column(name = "valid_from")
  private LocalDate validFrom;

  @Column(name = "valid_to")
  private LocalDate validTo;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "reject_reason", length = 500)
  private String rejectReason;

  protected EbBor() {}

  /**
   * Registers an uploaded BOR version.
   *
   * @param cycle cycle
   * @param versionNo version, from 1
   * @param attachmentId the stored file
   */
  public EbBor(EbCycle cycle, int versionNo, Long attachmentId) {
    this.companyId = cycle.getCompanyId();
    this.programmeId = cycle.getProgrammeId();
    this.cycleId = cycle.getId();
    this.versionNo = versionNo;
    this.attachmentId = attachmentId;
  }

  /**
   * Validates the BOR: the checklist must be complete and the validity period valid.
   *
   * @param checklist validator's answers and validity
   * @param by validator
   * @param at time
   */
  public void validate(Checklist checklist, String by, Instant at) {
    requireUploaded();
    if (!checklist.complete()) {
      throw new BusinessRuleException(
          "EB_BOR_CHECKLIST_INCOMPLETE", "Complete the BOR checklist before validating");
    }
    if (checklist.validFrom() == null || checklist.validTo() == null) {
      throw new BusinessRuleException(
          "EB_BOR_VALIDITY_REQUIRED", "Enter the validity dates of the BOR");
    }
    if (!checklist.validTo().isAfter(checklist.validFrom())) {
      throw new BusinessRuleException(
          "EB_BOR_VALIDITY_INVALID", "The BOR must be valid to a date after it is valid from");
    }
    this.signedBySignatory = true;
    this.notBlank = true;
    this.clientNameMatches = true;
    this.validFrom = checklist.validFrom();
    this.validTo = checklist.validTo();
    decide(EbBorStatus.VALIDATED, by, at);
  }

  /**
   * Rejects the BOR with a reason; the AO uploads a corrected version.
   *
   * @param reason why
   * @param by validator
   * @param at time
   */
  public void reject(String reason, String by, Instant at) {
    requireUploaded();
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException(
          "EB_BOR_REASON_REQUIRED", "Enter the reason for rejecting the BOR");
    }
    this.rejectReason = reason.strip();
    decide(EbBorStatus.REJECTED, by, at);
  }

  /** A later version was validated. */
  public void supersede() {
    if (status == EbBorStatus.VALIDATED) {
      this.status = EbBorStatus.SUPERSEDED;
    }
  }

  private void decide(EbBorStatus result, String by, Instant at) {
    this.status = result;
    this.decidedBy = by;
    this.decidedAt = at;
  }

  private void requireUploaded() {
    if (status != EbBorStatus.UPLOADED) {
      throw new BusinessRuleException(
          "EB_BOR_ALREADY_DECIDED", "Version " + versionNo + " of the BOR is already decided");
    }
  }

  /**
   * Whether this version is the active BOR on a date.
   *
   * @param date date
   * @return true when validated and valid on the date
   */
  public boolean activeOn(LocalDate date) {
    return status == EbBorStatus.VALIDATED && !date.isBefore(validFrom) && !date.isAfter(validTo);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getProgrammeId() {
    return programmeId;
  }

  public Long getCycleId() {
    return cycleId;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public EbBorStatus getStatus() {
    return status;
  }

  public Boolean getSignedBySignatory() {
    return signedBySignatory;
  }

  public Boolean getNotBlank() {
    return notBlank;
  }

  public Boolean getClientNameMatches() {
    return clientNameMatches;
  }

  public LocalDate getValidFrom() {
    return validFrom;
  }

  public LocalDate getValidTo() {
    return validTo;
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

  /**
   * The validator's checklist (FR-EB-031 fields).
   *
   * @param signedBySignatory signed by an authorised signatory
   * @param notBlank not blank
   * @param clientNameMatches the client name matches
   * @param validFrom valid from
   * @param validTo valid to, after valid from
   */
  public record Checklist(
      boolean signedBySignatory,
      boolean notBlank,
      boolean clientNameMatches,
      LocalDate validFrom,
      LocalDate validTo) {

    /**
     * Whether every check is confirmed.
     *
     * @return true when all three are ticked
     */
    public boolean complete() {
      return signedBySignatory && notBlank && clientNameMatches;
    }
  }
}
