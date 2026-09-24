package com.iortatechnxt.brokerverse.claims.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Immutable line of the claim movement ledger: an approved estimate change or an amount paid /
 * recovered, typed with the Reports Book code, at 100 % and company share, with the base-currency
 * equivalent at the rate of its date. Reports, the insurance kernel views and the listener
 * notifications are all derived from these lines.
 */
@Entity
@Table(name = "clm_movement")
public class MovementLine extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "claim_id")
  private Claim claim;

  @Column(name = "movement_date", nullable = false)
  private LocalDate movementDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private MovementKind kind;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private EstimateSide side;

  @Enumerated(EnumType.STRING)
  @Column(name = "cost_type", nullable = false, length = 10)
  private CostType costType;

  @Column(name = "estimate_type", nullable = false)
  private int estimateType;

  @Column(name = "amount_100", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount100;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "base_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal baseAmount;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false, length = 80)
  private String reference;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false, length = 15)
  private MovementSource sourceType;

  @Column(name = "source_id", nullable = false)
  private Long sourceId;

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  @Column(nullable = false, length = 300)
  private String narration;

  protected MovementLine() {}

  /**
   * Creates a line.
   *
   * @param v values
   */
  public MovementLine(MovementValues v) {
    Claim c = v.claim();
    this.companyId = c.getCompanyId();
    this.branchId = c.getBranchId();
    this.claim = c;
    this.currency = c.getCurrency();
    this.movementDate = v.date();
    this.kind = v.kind();
    this.side = v.line().side();
    this.costType = v.line().costType();
    this.amount100 = v.line().amount();
    this.estimateType = EstimateType.of(side, amount100.signum() < 0).code();
    this.amount = v.amount();
    this.baseAmount = v.baseAmount();
    this.reference = v.reference();
    this.sourceType = v.source();
    this.sourceId = v.sourceId();
    this.journalBatchNo = v.journalBatchNo();
    this.narration = v.narration();
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public Claim getClaim() {
    return claim;
  }

  public LocalDate getMovementDate() {
    return movementDate;
  }

  public MovementKind getKind() {
    return kind;
  }

  public EstimateSide getSide() {
    return side;
  }

  public CostType getCostType() {
    return costType;
  }

  public int getEstimateType() {
    return estimateType;
  }

  public BigDecimal getAmount100() {
    return amount100;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getBaseAmount() {
    return baseAmount;
  }

  public String getCurrency() {
    return currency;
  }

  public String getReference() {
    return reference;
  }

  public MovementSource getSourceType() {
    return sourceType;
  }

  public Long getSourceId() {
    return sourceId;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }

  public String getNarration() {
    return narration;
  }
}
