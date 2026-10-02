package com.iortatechnxt.brokerverse.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One status change (insert only): from, to, action, reason, comment, who and when. */
@Entity
@Table(name = "wf_case_history")
public class WorkCaseHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "case_id", nullable = false, updatable = false)
  private Long caseId;

  @Column(name = "from_stage", length = 40, updatable = false)
  private String fromStage;

  @Column(name = "to_stage", nullable = false, length = 40, updatable = false)
  private String toStage;

  @Column(nullable = false, length = 40, updatable = false)
  private String action;

  @Column(name = "reason_code", length = 40, updatable = false)
  private String reasonCode;

  @Column(length = 1000, updatable = false)
  private String comment;

  @Column(nullable = false, length = 50, updatable = false)
  private String actor;

  @Column(nullable = false, updatable = false)
  private boolean automatic;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  protected WorkCaseHistory() {}

  /**
   * Records a change.
   *
   * @param caseId case
   * @param change what changed
   * @param actor user
   * @param automatic true for system actions
   * @param occurredAt time
   */
  public WorkCaseHistory(
      Long caseId, StageChange change, String actor, boolean automatic, Instant occurredAt) {
    this.caseId = caseId;
    this.fromStage = change.fromStage();
    this.toStage = change.toStage();
    this.action = change.action();
    this.reasonCode = change.reasonCode();
    this.comment = change.comment();
    this.actor = actor;
    this.automatic = automatic;
    this.occurredAt = occurredAt;
  }

  public Long getId() {
    return id;
  }

  public Long getCaseId() {
    return caseId;
  }

  public String getFromStage() {
    return fromStage;
  }

  public String getToStage() {
    return toStage;
  }

  public String getAction() {
    return action;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public String getComment() {
    return comment;
  }

  public String getActor() {
    return actor;
  }

  public boolean isAutomatic() {
    return automatic;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
