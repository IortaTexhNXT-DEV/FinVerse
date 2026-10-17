package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * A Contact Center follow-up of a renewal (BRRN.026): channel, outcome, remarks and the next action
 * date.
 */
@Entity
@Table(name = "rnw_followup")
public class RenewalFollowup extends BaseEntity {

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Column(nullable = false, length = 20, updatable = false)
  private String channel;

  @Column(nullable = false, length = 40, updatable = false)
  private String outcome;

  @Column(nullable = false, length = 200, updatable = false)
  private String remarks;

  @Column(name = "next_action_date", updatable = false)
  private LocalDate nextActionDate;

  protected RenewalFollowup() {}

  /**
   * Records a follow-up.
   *
   * @param candidateId candidate
   * @param channel channel (list RNW_FOLLOWUP_CHANNEL)
   * @param outcome outcome (list RNW_FOLLOWUP_OUTCOME)
   * @param remarks remarks
   * @param nextActionDate next action, may be null
   */
  public RenewalFollowup(
      Long candidateId, String channel, String outcome, String remarks, LocalDate nextActionDate) {
    this.candidateId = candidateId;
    this.channel = channel;
    this.outcome = outcome;
    this.remarks = remarks;
    this.nextActionDate = nextActionDate;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public String getChannel() {
    return channel;
  }

  public String getOutcome() {
    return outcome;
  }

  public String getRemarks() {
    return remarks;
  }

  public LocalDate getNextActionDate() {
    return nextActionDate;
  }
}
