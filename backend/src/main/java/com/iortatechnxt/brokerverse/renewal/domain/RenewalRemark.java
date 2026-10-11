package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A remark on a renewal (BRD 2.004.6-8): at most 200 characters, stamped with the user, time and
 * stage; never edited.
 */
@Entity
@Table(name = "rnw_remark")
public class RenewalRemark extends BaseEntity {

  /** Longest remark (BRD 2.004.6). */
  public static final int MAX_LENGTH = 200;

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30, updatable = false)
  private RenewalStage stage;

  @Column(name = "remark_text", nullable = false, length = MAX_LENGTH, updatable = false)
  private String text;

  protected RenewalRemark() {}

  /**
   * Records a remark.
   *
   * @param candidateId candidate
   * @param stage stage of the candidate when the remark was made
   * @param text remark
   */
  public RenewalRemark(Long candidateId, RenewalStage stage, String text) {
    this.candidateId = candidateId;
    this.stage = stage;
    this.text = text;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public RenewalStage getStage() {
    return stage;
  }

  public String getText() {
    return text;
  }
}
