package com.iortatechnxt.finverse.reinsurance.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.insurance.ClaimMovement;
import com.iortatechnxt.finverse.insurance.ClaimMovementType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A claim movement received from the claims module and the reinsurers' shares computed for it. One
 * row per movement reference (idempotency key). The net retained amount is the company's share of a
 * payment (positive) or recovery (negative) after proportional reinsurance, the base of the excess
 * of loss recovery.
 */
@Entity
@Table(name = "ri_claim_movement")
public class RiClaimMovement extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false, updatable = false)
  private Long branchId;

  @Column(nullable = false, updatable = false, length = 100)
  private String reference;

  @Column(name = "claim_id", nullable = false, updatable = false)
  private Long claimId;

  @Column(name = "claim_no", nullable = false, updatable = false, length = 40)
  private String claimNo;

  @Column(name = "policy_id", nullable = false, updatable = false)
  private Long policyId;

  @Column(name = "cession_id", updatable = false)
  private Long cessionId;

  @Column(name = "business_line", nullable = false, updatable = false, length = 20)
  private String businessLine;

  @Column(name = "loss_date", nullable = false, updatable = false)
  private LocalDate lossDate;

  @Column(name = "movement_date", nullable = false, updatable = false)
  private LocalDate movementDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "movement_type", nullable = false, updatable = false, length = 20)
  private ClaimMovementType movementType;

  @Column(nullable = false, updatable = false, length = 3)
  private String currency;

  @Column(nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "base_amount", nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal baseAmount;

  @Column(name = "net_retained", nullable = false, precision = 19, scale = 2)
  private BigDecimal netRetained = Money.zero();

  @OneToMany(mappedBy = "movement", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private final List<RiClaimShare> shares = new ArrayList<>();

  /** For JPA. */
  protected RiClaimMovement() {}

  /**
   * Records a movement.
   *
   * @param m claim movement from the claims module
   * @param cessionId cession whose percentages apply, null when the policy was not ceded
   */
  public RiClaimMovement(ClaimMovement m, Long cessionId) {
    this.companyId = m.companyId();
    this.branchId = m.branchId();
    this.reference = m.reference();
    this.claimId = m.claimId();
    this.claimNo = m.claimNo();
    this.policyId = m.policyId();
    this.cessionId = cessionId;
    this.businessLine = m.lineOfBusiness();
    this.lossDate = m.lossDate();
    this.movementDate = m.movementDate();
    this.movementType = m.type();
    this.currency = m.currency();
    this.amount = Money.round(m.amount());
    this.baseAmount = Money.round(m.baseAmount());
  }

  /**
   * Adds a reinsurer's share.
   *
   * @param share share
   */
  public void addShare(RiClaimShare share) {
    shares.add(share);
  }

  /**
   * Records the company's net retained part of a payment or recovery.
   *
   * @param value signed amount (base currency)
   */
  public void retain(BigDecimal value) {
    this.netRetained = Money.round(value);
  }

  /**
   * Base amount signed as a loss: payments and reserve increases positive, recoveries negative.
   *
   * @return signed base amount
   */
  public BigDecimal signedLoss() {
    return movementType == ClaimMovementType.RECOVERY ? baseAmount.negate() : baseAmount;
  }

  /**
   * Total of the reinsurers' shares.
   *
   * @return base currency amount
   */
  public BigDecimal ceded() {
    return shares.stream().map(RiClaimShare::getBaseAmount).reduce(Money.zero(), BigDecimal::add);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getReference() {
    return reference;
  }

  public Long getClaimId() {
    return claimId;
  }

  public String getClaimNo() {
    return claimNo;
  }

  public Long getPolicyId() {
    return policyId;
  }

  public Long getCessionId() {
    return cessionId;
  }

  public String getBusinessLine() {
    return businessLine;
  }

  public LocalDate getLossDate() {
    return lossDate;
  }

  public LocalDate getMovementDate() {
    return movementDate;
  }

  public ClaimMovementType getMovementType() {
    return movementType;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getBaseAmount() {
    return baseAmount;
  }

  public BigDecimal getNetRetained() {
    return netRetained;
  }

  public List<RiClaimShare> getShares() {
    return List.copyOf(shares);
  }
}
