package com.iortatechnxt.finverse.reinsurance.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.party.domain.Party;
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
import java.util.Objects;

/**
 * Quarterly statement of account of one treaty participant. Generated (and regenerated while
 * pending) by a maker, approved by a checker (statement adjustments posted), then settled (balance
 * paid or received and the reinsurer's open items matched).
 */
@Entity
@Table(name = "ri_soa")
public class Soa extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "soa_no", nullable = false, updatable = false, length = 40)
  private String soaNo;

  @ManyToOne(optional = false)
  @JoinColumn(name = "treaty_id", updatable = false)
  private Treaty treaty;

  @ManyToOne(optional = false)
  @JoinColumn(name = "party_id", updatable = false)
  private Party party;

  @Column(name = "soa_year", nullable = false, updatable = false)
  private int soaYear;

  @Column(nullable = false, updatable = false)
  private int quarter;

  @Column(name = "period_from", nullable = false, updatable = false)
  private LocalDate periodFrom;

  @Column(name = "period_to", nullable = false, updatable = false)
  private LocalDate periodTo;

  @Column(name = "statement_date", nullable = false)
  private LocalDate statementDate;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal premium;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal commission;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal levy;

  @Column(name = "losses_paid", nullable = false, precision = 19, scale = 2)
  private BigDecimal lossesPaid;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal recoveries;

  @Column(name = "premium_reserve_retained", nullable = false, precision = 19, scale = 2)
  private BigDecimal premiumReserveRetained;

  @Column(name = "premium_reserve_released", nullable = false, precision = 19, scale = 2)
  private BigDecimal premiumReserveReleased;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal interest;

  @Column(name = "loss_reserve_retained", nullable = false, precision = 19, scale = 2)
  private BigDecimal lossReserveRetained;

  @Column(name = "loss_reserve_released", nullable = false, precision = 19, scale = 2)
  private BigDecimal lossReserveReleased;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal balance;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SoaStatus status = SoaStatus.PENDING_APPROVAL;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "adjustment_batch_no", length = 40)
  private String adjustmentBatchNo;

  @Column(name = "settlement_date")
  private LocalDate settlementDate;

  @Column(name = "settlement_batch_no", length = 40)
  private String settlementBatchNo;

  @Column(name = "bank_account_code", length = 20)
  private String bankAccountCode;

  /** For JPA. */
  protected Soa() {}

  /**
   * Creates a statement pending approval.
   *
   * @param soaNo statement number
   * @param treaty treaty
   * @param party participant
   * @param period year and quarter
   * @param figures statement lines
   */
  public Soa(String soaNo, Treaty treaty, Party party, SoaPeriod period, SoaFigures figures) {
    this.soaNo = soaNo;
    this.treaty = treaty;
    this.party = party;
    this.companyId = treaty.getCompanyId();
    this.currency = treaty.getCurrency();
    this.soaYear = period.year();
    this.quarter = period.quarter();
    this.periodFrom = period.from();
    this.periodTo = period.to();
    apply(figures, period.statementDate());
  }

  /**
   * Replaces the figures of a statement still pending approval.
   *
   * @param figures new figures
   * @param date statement date
   */
  public void refresh(SoaFigures figures, LocalDate date) {
    if (status != SoaStatus.PENDING_APPROVAL) {
      throw new BusinessRuleException("SOA_STATUS", "Statement " + soaNo + " is " + status);
    }
    apply(figures, date);
  }

  private void apply(SoaFigures figures, LocalDate date) {
    this.statementDate = date;
    this.premium = figures.premium();
    this.commission = figures.commission();
    this.levy = figures.levy();
    this.lossesPaid = figures.lossesPaid();
    this.recoveries = figures.recoveries();
    this.premiumReserveRetained = figures.premiumReserveRetained();
    this.premiumReserveReleased = figures.premiumReserveReleased();
    this.interest = figures.interest();
    this.lossReserveRetained = figures.lossReserveRetained();
    this.lossReserveReleased = figures.lossReserveReleased();
    this.balance = figures.balance();
  }

  /**
   * Approves the statement (checker).
   *
   * @param checker approving user, not the maker
   * @param when approval time
   * @param batchNo journal of the statement adjustments, null when none
   */
  public void approve(String checker, Instant when, String batchNo) {
    if (status != SoaStatus.PENDING_APPROVAL) {
      throw new BusinessRuleException("SOA_STATUS", "Statement " + soaNo + " is " + status);
    }
    if (Objects.equals(maker(), checker)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A statement cannot be approved by the user who prepared it");
    }
    this.status = SoaStatus.APPROVED;
    this.approvedBy = checker;
    this.approvedAt = when;
    this.adjustmentBatchNo = batchNo;
  }

  /**
   * Records the settlement.
   *
   * @param date settlement date
   * @param batchNo journal of the payment or receipt
   * @param bankAccount bank account used
   */
  public void settle(LocalDate date, String batchNo, String bankAccount) {
    if (status != SoaStatus.APPROVED) {
      throw new BusinessRuleException(
          "SOA_STATUS", "Only an approved statement can be settled; " + soaNo + " is " + status);
    }
    this.status = SoaStatus.SETTLED;
    this.settlementDate = date;
    this.settlementBatchNo = batchNo;
    this.bankAccountCode = bankAccount;
  }

  /**
   * The user who prepared (or last regenerated) the statement.
   *
   * @return maker
   */
  public String maker() {
    return getUpdatedBy() != null ? getUpdatedBy() : getCreatedBy();
  }

  /**
   * Statement lines.
   *
   * @return figures
   */
  public SoaFigures figures() {
    return new SoaFigures(
        premium,
        commission,
        levy,
        lossesPaid,
        recoveries,
        premiumReserveRetained,
        premiumReserveReleased,
        interest,
        lossReserveRetained,
        lossReserveReleased);
  }

  /**
   * Year and quarter of the statement.
   *
   * @return period
   */
  public SoaPeriod period() {
    return new SoaPeriod(soaYear, quarter, statementDate);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getSoaNo() {
    return soaNo;
  }

  public Treaty getTreaty() {
    return treaty;
  }

  public Party getParty() {
    return party;
  }

  public int getSoaYear() {
    return soaYear;
  }

  public int getQuarter() {
    return quarter;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public LocalDate getStatementDate() {
    return statementDate;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public SoaStatus getStatus() {
    return status;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public String getAdjustmentBatchNo() {
    return adjustmentBatchNo;
  }

  public LocalDate getSettlementDate() {
    return settlementDate;
  }

  public String getSettlementBatchNo() {
    return settlementBatchNo;
  }

  public String getBankAccountCode() {
    return bankAccountCode;
  }
}
