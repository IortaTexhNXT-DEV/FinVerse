package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A policy related to an IAAF (BRIDSP-05): previous term, same borrower or other. */
@Entity
@Table(name = "sbm_iaaf_link")
public class SbmIaafLink extends BaseEntity {

  @Column(name = "iaaf_id", nullable = false, updatable = false)
  private Long iaafId;

  @Column(name = "related_policy_id", nullable = false, updatable = false)
  private Long relatedPolicyId;

  @Column(nullable = false, updatable = false, length = 20)
  private String relation;

  protected SbmIaafLink() {}

  /**
   * A link.
   *
   * @param iaafId IAAF
   * @param relatedPolicyId related policy
   * @param relation PREVIOUS_TERM, SAME_BORROWER or OTHER
   */
  public SbmIaafLink(Long iaafId, Long relatedPolicyId, String relation) {
    this.iaafId = iaafId;
    this.relatedPolicyId = relatedPolicyId;
    this.relation = relation;
  }

  public Long getIaafId() {
    return iaafId;
  }

  public Long getRelatedPolicyId() {
    return relatedPolicyId;
  }

  public String getRelation() {
    return relation;
  }
}
