package com.iortatechnxt.brokerverse.claims.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.party.domain.Party;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Settlement of a claim to a payee (claimant, garage, surveyor), partial or final, at 100 %: net =
 * assessed loss − deductible − excess. On approval the net consumes the outstanding reserve of its
 * cost type, is posted (company share) and becomes a payable open item of the payee.
 */
@Entity
@Table(name = "clm_settlement")
public class Settlement extends ClaimDocument {

  @Column(name = "settlement_no", nullable = false, length = 40)
  private String settlementNo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "payee_party_id")
  private Party payee;

  @Enumerated(EnumType.STRING)
  @Column(name = "cost_type", nullable = false, length = 10)
  private CostType costType;

  @Enumerated(EnumType.STRING)
  @Column(name = "settlement_type", nullable = false, length = 10)
  private SettlementType settlementType;

  @Column(name = "assessed_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal assessedAmount;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal deductible;

  @Column(name = "excess_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal excessAmount;

  @Column(name = "net_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal netAmount;

  @Column(name = "our_amount", precision = 19, scale = 2)
  private BigDecimal ourAmount;

  @Column(name = "payable_amount", precision = 19, scale = 2)
  private BigDecimal payableAmount;

  @Column(name = "coinsurer_amount", precision = 19, scale = 2)
  private BigDecimal coinsurerAmount;

  @Column(name = "exchange_rate", precision = 19, scale = 8)
  private BigDecimal exchangeRate;

  @Column(nullable = false, length = 300)
  private String narration;

  @Column(name = "coinsurance_batch_no", length = 40)
  private String coinsuranceBatchNo;

  protected Settlement() {}

  /**
   * Creates a settlement pending approval.
   *
   * @param claim claim
   * @param settlementNo allocated number
   * @param terms payee, cost type, type and amounts
   * @param approval maker-checker state
   */
  public Settlement(
      Claim claim, String settlementNo, SettlementTerms terms, ClaimApproval approval) {
    super(claim, approval);
    BigDecimal deductions = Money.nz(terms.deductible()).add(Money.nz(terms.excess()));
    BigDecimal net = terms.assessedAmount().subtract(deductions);
    if (net.signum() <= 0) {
      throw new BusinessRuleException(
          "SETTLEMENT_NOT_POSITIVE",
          "Deductible and excess (" + deductions + ") absorb the assessed amount");
    }
    this.settlementNo = settlementNo;
    this.payee = terms.payee();
    this.costType = terms.costType();
    this.settlementType = terms.settlementType();
    this.assessedAmount = terms.assessedAmount();
    this.deductible = Money.nz(terms.deductible());
    this.excessAmount = Money.nz(terms.excess());
    this.netAmount = net;
    this.narration = terms.narration();
  }

  /**
   * Records the accounting split made at approval.
   *
   * @param split company share, amount payable to the payee and coinsurers' share
   * @param rate exchange rate to base currency
   * @param coinsuranceBatch journal of the coinsurers' share, null when not leading
   */
  public void recordSplit(ShareSplit split, BigDecimal rate, String coinsuranceBatch) {
    this.ourAmount = split.ours();
    this.payableAmount = split.payable();
    this.coinsurerAmount = split.coinsurers();
    this.exchangeRate = rate;
    this.coinsuranceBatchNo = coinsuranceBatch;
  }

  @Override
  public String label() {
    return "Settlement " + settlementNo;
  }

  public String getSettlementNo() {
    return settlementNo;
  }

  public Party getPayee() {
    return payee;
  }

  public CostType getCostType() {
    return costType;
  }

  public SettlementType getSettlementType() {
    return settlementType;
  }

  public BigDecimal getAssessedAmount() {
    return assessedAmount;
  }

  public BigDecimal getDeductible() {
    return deductible;
  }

  public BigDecimal getExcessAmount() {
    return excessAmount;
  }

  public BigDecimal getNetAmount() {
    return netAmount;
  }

  public BigDecimal getOurAmount() {
    return ourAmount;
  }

  public BigDecimal getPayableAmount() {
    return payableAmount;
  }

  public BigDecimal getCoinsurerAmount() {
    return coinsurerAmount;
  }

  public BigDecimal getExchangeRate() {
    return exchangeRate;
  }

  public String getNarration() {
    return narration;
  }

  public String getCoinsuranceBatchNo() {
    return coinsuranceBatchNo;
  }
}
