package com.iortatechnxt.brokerverse.nbadmin.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One approver of an access request, in order (BRD 1.002.1.1.3 "select approver", BRD 3.002.x
 * "approver/s"): user requests have one, group-profile requests one or more, each deciding in turn.
 */
@Entity
@Table(name = "nba_access_request_approver")
public class AccessRequestApprover extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "request_id", nullable = false, updatable = false)
  private AccessRequest request;

  @Column(nullable = false)
  private int sequence;

  @Column(nullable = false, length = 50)
  private String approver;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private AccessApproverDecision decision = AccessApproverDecision.PENDING;

  @Column(length = 1000)
  private String remarks;

  @Column(name = "decided_at")
  private Instant decidedAt;

  protected AccessRequestApprover() {}

  AccessRequestApprover(AccessRequest request, int sequence, String approver) {
    this.request = request;
    this.sequence = sequence;
    this.approver = approver;
  }

  void decide(AccessApproverDecision outcome, String comment, Instant when) {
    this.decision = outcome;
    this.remarks = comment;
    this.decidedAt = when;
  }

  public int getSequence() {
    return sequence;
  }

  public String getApprover() {
    return approver;
  }

  public AccessApproverDecision getDecision() {
    return decision;
  }

  public String getRemarks() {
    return remarks;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }
}
