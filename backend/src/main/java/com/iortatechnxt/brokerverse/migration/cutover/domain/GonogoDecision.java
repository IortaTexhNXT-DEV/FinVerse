package com.iortatechnxt.brokerverse.migration.cutover.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A go / no-go decision of a plan (gate G7) with the criteria met at the time. */
@Entity
@Table(name = "mig_gonogo_decision")
public class GonogoDecision {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "plan_id", nullable = false, updatable = false)
  private Long planId;

  @Column(nullable = false, length = 6, updatable = false)
  private String decision;

  @Column(length = 2000, updatable = false)
  private String comment;

  @Column(name = "criteria_met", nullable = false, updatable = false)
  private int criteriaMet;

  @Column(name = "criteria_total", nullable = false, updatable = false)
  private int criteriaTotal;

  @Column(name = "decided_by", nullable = false, length = 50, updatable = false)
  private String decidedBy;

  @Column(name = "decided_at", nullable = false, updatable = false)
  private Instant decidedAt;

  protected GonogoDecision() {}

  /**
   * A decision.
   *
   * @param planId plan
   * @param go GO or NO_GO
   * @param comment comment
   * @param met criteria met
   * @param total criteria
   * @param user board member
   * @param when time
   */
  public GonogoDecision(
      Long planId, boolean go, String comment, int met, int total, String user, Instant when) {
    this.planId = planId;
    this.decision = go ? "GO" : "NO_GO";
    this.comment = comment;
    this.criteriaMet = met;
    this.criteriaTotal = total;
    this.decidedBy = user;
    this.decidedAt = when;
  }

  public Long getId() {
    return id;
  }

  public Long getPlanId() {
    return planId;
  }

  public String getDecision() {
    return decision;
  }

  public String getComment() {
    return comment;
  }

  public int getCriteriaMet() {
    return criteriaMet;
  }

  public int getCriteriaTotal() {
    return criteriaTotal;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }
}
