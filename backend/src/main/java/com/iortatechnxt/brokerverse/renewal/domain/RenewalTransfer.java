package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;

/**
 * A transfer of a renewal to another Marketing unit (BRD 1.006, 1.007, 2.005): requested with
 * remarks, accepted or declined with remarks by a Team Leader of the receiving unit, or cancelled
 * by the sender. One open request per renewal.
 */
@Entity
@Table(name = "rnw_transfer")
public class RenewalTransfer extends BaseEntity {

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Column(name = "from_unit", length = 20, updatable = false)
  private String fromUnit;

  @Column(name = "to_unit", nullable = false, length = 20, updatable = false)
  private String toUnit;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_stage", nullable = false, length = 30, updatable = false)
  private RenewalStage fromStage;

  @Column(name = "reason_code", length = 40, updatable = false)
  private String reasonCode;

  @Column(nullable = false, length = 200, updatable = false)
  private String remarks;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TransferStatus status = TransferStatus.REQUESTED;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_remarks", length = 200)
  private String decisionRemarks;

  protected RenewalTransfer() {}

  /**
   * Requests a transfer.
   *
   * @param candidate candidate
   * @param toUnit receiving unit
   * @param reasonCode reason (list RNW_TRANSFER_REASON), may be null
   * @param remarks remarks
   */
  public RenewalTransfer(
      RenewalCandidate candidate, String toUnit, String reasonCode, String remarks) {
    this.candidateId = candidate.getId();
    this.fromUnit = candidate.getOwnerUnit();
    this.fromStage = candidate.getStage();
    this.toUnit = toUnit;
    this.reasonCode = reasonCode;
    this.remarks = remarks;
  }

  /**
   * Decides or cancels the request.
   *
   * @param outcome ACCEPTED, DECLINED or CANCELLED
   * @param user user
   * @param remarks remarks
   * @param at time
   */
  public void decide(TransferStatus outcome, String user, String remarks, Instant at) {
    if (status != TransferStatus.REQUESTED) {
      throw new BusinessRuleException(
          "RNW_TRANSFER_DECIDED",
          "The transfer is already " + status.name().toLowerCase(Locale.ROOT));
    }
    this.status = outcome;
    this.decidedBy = user;
    this.decidedAt = at;
    this.decisionRemarks = remarks;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public String getFromUnit() {
    return fromUnit;
  }

  public String getToUnit() {
    return toUnit;
  }

  public RenewalStage getFromStage() {
    return fromStage;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public String getRemarks() {
    return remarks;
  }

  public TransferStatus getStatus() {
    return status;
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
}
