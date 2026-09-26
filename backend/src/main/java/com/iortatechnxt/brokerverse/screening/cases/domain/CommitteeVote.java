package com.iortatechnxt.brokerverse.screening.cases.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * An AML Committee member's decision on a case round (SNSRP-704; FR-SS-064): APPROVE_STR, NO_STR or
 * COMMITTEE_RETURN with remarks. One vote per member and round; insert-only (a vote cannot be
 * changed, FR-SS-064 R1).
 */
@Entity
@Table(name = "scr_committee_vote")
public class CommitteeVote extends BaseEntity {

  @Column(name = "case_id", nullable = false, updatable = false)
  private Long caseId;

  @Column(name = "round_no", nullable = false, updatable = false)
  private int roundNo;

  @Column(name = "member", nullable = false, length = 50, updatable = false)
  private String member;

  @Column(name = "decision", nullable = false, length = 40, updatable = false)
  private String decision;

  @Column(name = "remarks", nullable = false, length = 4000, updatable = false)
  private String remarks;

  @Column(name = "voted_at", nullable = false, updatable = false)
  private Instant votedAt;

  /** For JPA. */
  protected CommitteeVote() {}

  /**
   * Records a vote.
   *
   * @param c the case (its committee round)
   * @param member the member
   * @param decision the decision code
   * @param remarks the remarks
   * @param at when
   */
  public CommitteeVote(
      ScreeningCase c, String member, String decision, String remarks, Instant at) {
    this.caseId = c.getId();
    this.roundNo = c.getCommitteeRound();
    this.member = member;
    this.decision = decision;
    this.remarks = remarks;
    this.votedAt = at;
  }

  public Long getCaseId() {
    return caseId;
  }

  public int getRoundNo() {
    return roundNo;
  }

  public String getMember() {
    return member;
  }

  public String getDecision() {
    return decision;
  }

  public String getRemarks() {
    return remarks;
  }

  public Instant getVotedAt() {
    return votedAt;
  }
}
