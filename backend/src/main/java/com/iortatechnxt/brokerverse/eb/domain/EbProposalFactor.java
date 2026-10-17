package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A capability factor of a proposal (list EB_CAPABILITY_FACTOR): value and rating 1 to 5. */
@Entity
@Table(name = "eb_proposal_factor")
public class EbProposalFactor extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "proposal_id", nullable = false, updatable = false)
  private EbProposal proposal;

  @Column(name = "factor_code", nullable = false, length = 40)
  private String factorCode;

  @Column(name = "factor_value", length = 500)
  private String factorValue;

  @Column private Integer rating;

  protected EbProposalFactor() {}

  EbProposalFactor(EbProposal proposal, Data data) {
    this.proposal = proposal;
    this.factorCode = data.factorCode();
    this.factorValue = data.value();
    this.rating = data.rating();
  }

  public EbProposal getProposal() {
    return proposal;
  }

  public String getFactorCode() {
    return factorCode;
  }

  public String getFactorValue() {
    return factorValue;
  }

  public Integer getRating() {
    return rating;
  }

  /**
   * Data of a factor.
   *
   * @param factorCode factor (list EB_CAPABILITY_FACTOR)
   * @param value what the insurer states, may be null
   * @param rating rating 1 to 5, may be null
   */
  public record Data(String factorCode, String value, Integer rating) {}
}
