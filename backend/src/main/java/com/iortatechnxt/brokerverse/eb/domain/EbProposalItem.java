package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** The answer of a proposal to a TOR item: what is offered and whether it deviates. */
@Entity
@Table(name = "eb_proposal_item")
public class EbProposalItem extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "proposal_id", nullable = false, updatable = false)
  private EbProposal proposal;

  @Column(name = "tor_item_id", nullable = false)
  private Long torItemId;

  @Column(name = "offered_value", nullable = false, length = 1000)
  private String offeredValue;

  @Column(nullable = false)
  private boolean deviation;

  @Column(length = 500)
  private String remark;

  protected EbProposalItem() {}

  EbProposalItem(EbProposal proposal, Data data) {
    this.proposal = proposal;
    this.torItemId = data.torItemId();
    this.offeredValue = data.offeredValue();
    this.deviation = data.deviation();
    this.remark = data.remark();
  }

  public EbProposal getProposal() {
    return proposal;
  }

  public Long getTorItemId() {
    return torItemId;
  }

  public String getOfferedValue() {
    return offeredValue;
  }

  public boolean isDeviation() {
    return deviation;
  }

  public String getRemark() {
    return remark;
  }

  /**
   * Data of an answer.
   *
   * @param torItemId TOR item
   * @param offeredValue what is offered
   * @param deviation whether it deviates from the requirement
   * @param remark remark, may be null
   */
  public record Data(Long torItemId, String offeredValue, boolean deviation, String remark) {}
}
