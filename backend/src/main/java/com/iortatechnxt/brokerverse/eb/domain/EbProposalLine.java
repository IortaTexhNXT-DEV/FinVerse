package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** The premium of one plan of a benefit line in a proposal. */
@Entity
@Table(name = "eb_proposal_line")
public class EbProposalLine extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "proposal_id", nullable = false, updatable = false)
  private EbProposal proposal;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "benefit_line", nullable = false, length = 30)
  private String benefitLine;

  @Column(name = "plan_code", nullable = false, length = 30)
  private String planCode;

  @Column(name = "plan_name", length = 200)
  private String planName;

  @Column private Integer members;

  @Column(name = "premium_rate", precision = 19, scale = 4)
  private BigDecimal premiumRate;

  @Column(name = "annual_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal annualPremium;

  @Column(name = "sum_insured", precision = 19, scale = 2)
  private BigDecimal sumInsured;

  protected EbProposalLine() {}

  EbProposalLine(EbProposal proposal, int sortOrder, Data data) {
    this.proposal = proposal;
    this.sortOrder = sortOrder;
    this.benefitLine = data.benefitLine();
    this.planCode = data.planCode();
    this.planName = data.planName();
    this.members = data.members();
    this.premiumRate = data.premiumRate();
    this.annualPremium = data.annualPremium();
    this.sumInsured = data.sumInsured();
  }

  public EbProposal getProposal() {
    return proposal;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public String getBenefitLine() {
    return benefitLine;
  }

  public String getPlanCode() {
    return planCode;
  }

  public String getPlanName() {
    return planName;
  }

  public Integer getMembers() {
    return members;
  }

  public BigDecimal getPremiumRate() {
    return premiumRate;
  }

  public BigDecimal getAnnualPremium() {
    return annualPremium;
  }

  public BigDecimal getSumInsured() {
    return sumInsured;
  }

  /**
   * Data of a plan.
   *
   * @param benefitLine benefit line
   * @param planCode plan
   * @param planName plan description, may be null
   * @param members members covered, may be null
   * @param premiumRate premium per member, may be null
   * @param annualPremium annual premium of the plan
   * @param sumInsured TSI of the plan (GLI / GPA), may be null
   */
  public record Data(
      String benefitLine,
      String planCode,
      String planName,
      Integer members,
      BigDecimal premiumRate,
      BigDecimal annualPremium,
      BigDecimal sumInsured) {}
}
