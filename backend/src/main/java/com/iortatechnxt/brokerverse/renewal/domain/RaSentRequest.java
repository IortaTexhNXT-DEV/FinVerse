package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * The correction of a rejected row of the Renewal Advices already sent before go-live (DMQ38;
 * FR-RN-016): prepared by a member of the Renewal processing team (maker, created by) and applied
 * only after a second member approves it.
 */
@Entity
@Table(name = "rnw_ra_sent_request")
public class RaSentRequest extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "legacy_ref", nullable = false, length = 60, updatable = false)
  private String legacyRef;

  @Column(name = "ra_date", nullable = false, updatable = false)
  private LocalDate raDate;

  @Column(name = "ra_ref", length = 60, updatable = false)
  private String raRef;

  @Column(length = 20, updatable = false)
  private String channel;

  @Column(length = 200, updatable = false)
  private String recipient;

  @Column(name = "job_no", length = 40, updatable = false)
  private String jobNo;

  @Column(name = "row_no", updatable = false)
  private Integer rowNo;

  @Column(nullable = false, length = 200, updatable = false)
  private String correction;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ApprovalStatus status = ApprovalStatus.PENDING;

  @Column(name = "candidate_id")
  private Long candidateId;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_remarks", length = 200)
  private String decisionRemarks;

  protected RaSentRequest() {}

  /**
   * Prepares a corrected row.
   *
   * @param companyId company
   * @param row legacy reference, RA date, reference, channel and recipient
   * @param source upload job and row corrected, may be null
   * @param correction what the maker corrected
   */
  public RaSentRequest(Long companyId, Row row, Source source, String correction) {
    this.companyId = companyId;
    this.legacyRef = row.legacyRef();
    this.raDate = row.raDate();
    this.raRef = row.raRef();
    this.channel = row.channel();
    this.recipient = row.recipient();
    this.jobNo = source == null ? null : source.jobNo();
    this.rowNo = source == null ? null : source.rowNo();
    this.correction = correction;
  }

  /**
   * Decides the correction (checker, never the maker).
   *
   * @param checker checker
   * @param approve approve or reject
   * @param remarks remarks
   * @param at time
   */
  public void decide(String checker, boolean approve, String remarks, Instant at) {
    if (status != ApprovalStatus.PENDING) {
      throw new BusinessRuleException("RNW_RA_SENT_DECIDED", "The correction is already " + status);
    }
    if (Objects.equals(checker, getCreatedBy())) {
      throw new BusinessRuleException(
          "RNW_RA_SENT_MAKER", "The correction must be approved by another team member");
    }
    this.status = approve ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED;
    this.decidedBy = checker;
    this.decidedAt = at;
    this.decisionRemarks = remarks;
  }

  /**
   * Records the renewal the approved correction was applied to.
   *
   * @param id candidate
   */
  public void appliedTo(Long id) {
    this.candidateId = id;
  }

  /**
   * The row data.
   *
   * @return row
   */
  public Row row() {
    return new Row(legacyRef, raDate, raRef, channel, recipient);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getLegacyRef() {
    return legacyRef;
  }

  public LocalDate getRaDate() {
    return raDate;
  }

  public String getRaRef() {
    return raRef;
  }

  public String getJobNo() {
    return jobNo;
  }

  public Integer getRowNo() {
    return rowNo;
  }

  public String getCorrection() {
    return correction;
  }

  public ApprovalStatus getStatus() {
    return status;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public String getDecisionRemarks() {
    return decisionRemarks;
  }

  /**
   * A row of the tracker of Renewal Advices already sent.
   *
   * @param legacyRef legacy policy reference
   * @param raDate date the RA was sent
   * @param raRef RA reference
   * @param channel channel
   * @param recipient recipient
   */
  public record Row(
      String legacyRef, LocalDate raDate, String raRef, String channel, String recipient) {}

  /**
   * The upload row a correction replaces.
   *
   * @param jobNo upload job
   * @param rowNo row
   */
  public record Source(String jobNo, Integer rowNo) {}
}
