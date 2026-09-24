package com.iortatechnxt.brokerverse.investment.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * An investment holding (one purchase of one security or deposit).
 *
 * <p>Captured by a maker ({@link HoldingStatus#PENDING_APPROVAL}); a checker's approval posts the
 * purchase (or, for a holding brought over from a previous system, the take-on opening balance).
 * The holding then carries its amortized cost, accrued interest receivable and cumulative fair
 * value adjustment, updated by the month-end runs, coupon receipts and fair value updates, until it
 * matures or is sold.
 */
@Entity
@Table(name = "inv_holding")
public class InvestmentHolding extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @ManyToOne(optional = false)
  @JoinColumn(name = "portfolio_id", nullable = false)
  private InvestmentPortfolio portfolio;

  @Column(name = "holding_no", nullable = false, length = 40)
  private String holdingNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "instrument_type", nullable = false, length = 20)
  private InstrumentType instrumentType;

  @Column(name = "security_code", length = 40)
  private String securityCode;

  @Column(nullable = false, length = 200)
  private String description;

  @Column(name = "issuer_code", nullable = false, length = 30)
  private String issuerCode;

  @Column(length = 120)
  private String custodian;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "face_value", nullable = false, precision = 19, scale = 2)
  private BigDecimal faceValue;

  @Column(name = "purchase_price", nullable = false, precision = 19, scale = 2)
  private BigDecimal purchasePrice;

  @Column(name = "purchased_interest", nullable = false, precision = 19, scale = 2)
  private BigDecimal purchasedInterest = BigDecimal.ZERO;

  @Column(name = "trade_date", nullable = false)
  private LocalDate tradeDate;

  @Column(name = "settlement_date", nullable = false)
  private LocalDate settlementDate;

  @Column(name = "maturity_date")
  private LocalDate maturityDate;

  @Column(name = "coupon_rate", nullable = false, precision = 19, scale = 8)
  private BigDecimal couponRate = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(name = "coupon_frequency", nullable = false, length = 20)
  private CouponFrequency couponFrequency;

  @Enumerated(EnumType.STRING)
  @Column(name = "day_count", nullable = false, length = 20)
  private DayCountConvention dayCount;

  @Enumerated(EnumType.STRING)
  @Column(name = "amortization_method", nullable = false, length = 20)
  private AmortizationMethod amortizationMethod;

  @Column(name = "effective_rate", precision = 19, scale = 8)
  private BigDecimal effectiveRate;

  @Column(name = "security_deposit", nullable = false)
  private boolean securityDeposit;

  @Column(name = "bank_account", nullable = false, length = 30)
  private String bankAccount;

  @Column(name = "take_on", nullable = false)
  private boolean takeOn;

  @Column(name = "take_on_date")
  private LocalDate takeOnDate;

  @Column(name = "amortized_cost", nullable = false, precision = 19, scale = 2)
  private BigDecimal amortizedCost = BigDecimal.ZERO;

  @Column(name = "accrued_interest", nullable = false, precision = 19, scale = 2)
  private BigDecimal accruedInterest = BigDecimal.ZERO;

  @Column(name = "fair_value_adjustment", nullable = false, precision = 19, scale = 2)
  private BigDecimal fairValueAdjustment = BigDecimal.ZERO;

  @Column(name = "fair_value", precision = 19, scale = 2)
  private BigDecimal fairValue;

  @Column(name = "fair_value_date")
  private LocalDate fairValueDate;

  @Column(name = "last_accrual_date")
  private LocalDate lastAccrualDate;

  @Column(name = "last_amortization_date")
  private LocalDate lastAmortizationDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private HoldingStatus status = HoldingStatus.PENDING_APPROVAL;

  @Column(name = "closed_date")
  private LocalDate closedDate;

  @Column(name = "purchase_batch_no", length = 40)
  private String purchaseBatchNo;

  protected InvestmentHolding() {}

  /**
   * Captures a holding (pending approval).
   *
   * @param portfolio portfolio
   * @param branchId owning branch
   * @param holdingNo holding number
   * @param instrumentType instrument
   * @param issuerCode issuer / depository bank party code
   * @param currency currency
   */
  public InvestmentHolding(
      InvestmentPortfolio portfolio,
      Long branchId,
      String holdingNo,
      InstrumentType instrumentType,
      String issuerCode,
      String currency) {
    this.companyId = portfolio.getCompanyId();
    this.portfolio = portfolio;
    this.branchId = branchId;
    this.holdingNo = holdingNo;
    this.instrumentType = instrumentType;
    this.issuerCode = issuerCode;
    this.currency = currency;
  }

  /**
   * Sets the contractual terms (only while pending approval).
   *
   * @param t terms
   */
  public void defineTerms(HoldingTerms t) {
    requirePending();
    faceValue = t.faceValue();
    purchasePrice = t.purchasePrice();
    purchasedInterest = t.purchasedInterest();
    tradeDate = t.tradeDate();
    settlementDate = t.settlementDate();
    maturityDate = t.maturityDate();
    couponRate = t.couponRate();
    couponFrequency = t.couponFrequency();
    dayCount = t.dayCount();
    amortizationMethod = t.amortizationMethod();
  }

  /**
   * Terms of the holding.
   *
   * @return terms
   */
  public HoldingTerms terms() {
    return new HoldingTerms(
        faceValue,
        purchasePrice,
        purchasedInterest,
        tradeDate,
        settlementDate,
        maturityDate,
        couponRate,
        couponFrequency,
        dayCount,
        amortizationMethod);
  }

  /**
   * Marks the holding as brought over from a previous system as of a date.
   *
   * @param date take-on date (value date of the opening balance)
   */
  public void takeOnAt(LocalDate date) {
    requirePending();
    takeOn = true;
    takeOnDate = date;
  }

  /**
   * Approves the holding (checker) and sets its opening position.
   *
   * @param checker approving user
   * @param when timestamp
   * @param carrying opening amortized cost
   * @param accrued opening accrued interest
   * @param rate effective annual interest rate (null when not amortized)
   */
  public void approve(
      String checker, Instant when, BigDecimal carrying, BigDecimal accrued, BigDecimal rate) {
    requirePending();
    authorize(checker, when);
    status = HoldingStatus.ACTIVE;
    amortizedCost = carrying;
    accruedInterest = accrued;
    effectiveRate = rate;
    lastAccrualDate = startDate();
    lastAmortizationDate = startDate();
  }

  /**
   * Adds accrued interest up to a date.
   *
   * @param to accrued up to
   * @param interest interest
   */
  public void accrue(LocalDate to, BigDecimal interest) {
    accruedInterest = accruedInterest.add(interest);
    lastAccrualDate = to;
  }

  /**
   * Adds amortization (negative for premium) up to a date.
   *
   * @param to amortized up to
   * @param amount amortization
   */
  public void amortize(LocalDate to, BigDecimal amount) {
    amortizedCost = amortizedCost.add(amount);
    lastAmortizationDate = to;
  }

  /**
   * Relieves the whole accrued interest receivable (coupon received or holding derecognized).
   *
   * @return the accrued interest relieved
   */
  public BigDecimal relieveAccruedInterest() {
    BigDecimal relieved = accruedInterest;
    accruedInterest = BigDecimal.ZERO;
    return relieved;
  }

  /**
   * Records a fair value remeasurement.
   *
   * @param date valuation date
   * @param value fair value (clean)
   * @param change change in carrying amount booked
   */
  public void remeasure(LocalDate date, BigDecimal value, BigDecimal change) {
    fairValueAdjustment = fairValueAdjustment.add(change);
    fairValue = value;
    fairValueDate = date;
  }

  /**
   * Closes (derecognizes) the holding: nothing remains carried.
   *
   * @param outcome MATURED or SOLD
   * @param date closing date
   */
  public void close(HoldingStatus outcome, LocalDate date) {
    requireActive();
    status = outcome;
    closedDate = date;
    amortizedCost = BigDecimal.ZERO;
    accruedInterest = BigDecimal.ZERO;
    fairValueAdjustment = BigDecimal.ZERO;
  }

  /** Fails unless the holding is still pending approval. */
  public void requirePending() {
    if (status != HoldingStatus.PENDING_APPROVAL) {
      throw new BusinessRuleException(
          "HOLDING_NOT_PENDING", "Holding " + holdingNo + " is already approved");
    }
  }

  /** Fails unless the holding is held (approved and not matured or sold). */
  public void requireActive() {
    if (status != HoldingStatus.ACTIVE) {
      throw new BusinessRuleException(
          "HOLDING_NOT_ACTIVE", "Holding " + holdingNo + " is " + status);
    }
  }

  /**
   * Carrying amount in the ledger: amortized cost plus the cumulative fair value adjustment.
   *
   * @return carrying amount
   */
  public BigDecimal carryingAmount() {
    return amortizedCost.add(fairValueAdjustment);
  }

  /**
   * Date from which the holding accrues and amortizes in this system.
   *
   * @return take-on date for take-on holdings, else the settlement date
   */
  public LocalDate startDate() {
    return takeOn ? takeOnDate : settlementDate;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public InvestmentPortfolio getPortfolio() {
    return portfolio;
  }

  public String getHoldingNo() {
    return holdingNo;
  }

  public InstrumentType getInstrumentType() {
    return instrumentType;
  }

  public String getSecurityCode() {
    return securityCode;
  }

  public void setSecurityCode(String securityCode) {
    this.securityCode = securityCode;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getIssuerCode() {
    return issuerCode;
  }

  public String getCustodian() {
    return custodian;
  }

  public void setCustodian(String custodian) {
    this.custodian = custodian;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getFaceValue() {
    return faceValue;
  }

  public LocalDate getMaturityDate() {
    return maturityDate;
  }

  public BigDecimal getEffectiveRate() {
    return effectiveRate;
  }

  public boolean isSecurityDeposit() {
    return securityDeposit;
  }

  public void setSecurityDeposit(boolean securityDeposit) {
    this.securityDeposit = securityDeposit;
  }

  public String getBankAccount() {
    return bankAccount;
  }

  public void setBankAccount(String bankAccount) {
    this.bankAccount = bankAccount;
  }

  public boolean isTakeOn() {
    return takeOn;
  }

  public BigDecimal getAmortizedCost() {
    return amortizedCost;
  }

  public BigDecimal getAccruedInterest() {
    return accruedInterest;
  }

  public BigDecimal getFairValueAdjustment() {
    return fairValueAdjustment;
  }

  public BigDecimal getFairValue() {
    return fairValue;
  }

  public LocalDate getFairValueDate() {
    return fairValueDate;
  }

  public LocalDate getLastAccrualDate() {
    return lastAccrualDate;
  }

  public LocalDate getLastAmortizationDate() {
    return lastAmortizationDate;
  }

  public HoldingStatus getStatus() {
    return status;
  }

  public LocalDate getClosedDate() {
    return closedDate;
  }

  public String getPurchaseBatchNo() {
    return purchaseBatchNo;
  }

  public void setPurchaseBatchNo(String purchaseBatchNo) {
    this.purchaseBatchNo = purchaseBatchNo;
  }
}
