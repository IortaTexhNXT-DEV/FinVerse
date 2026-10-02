package com.iortatechnxt.brokerverse.closing.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Record of a completed year-end close: closing journals, net result transferred to retained
 * earnings and the fiscal year opened next.
 */
@Entity
@Table(name = "yec_year_end_close")
public class YearEndClose extends BaseEntity {

  /** Status of a completed close. */
  public static final String CLOSED = "CLOSED";

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "fiscal_year_id", nullable = false, unique = true)
  private Long fiscalYearId;

  @Column(name = "year_code", nullable = false)
  private int yearCode;

  @Column(name = "closing_date", nullable = false)
  private LocalDate closingDate;

  @Column(name = "net_result", nullable = false, precision = 19, scale = 2)
  private BigDecimal netResult;

  @Column(name = "retained_earnings_account", nullable = false, length = 30)
  private String retainedEarningsAccount;

  @Column(name = "closing_batches", length = 1000)
  private String closingBatches;

  @Column(name = "next_year_code")
  private Integer nextYearCode;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(name = "nominal_balance", precision = 19, scale = 2)
  private BigDecimal nominalBalance;

  @Column(name = "tb_difference", precision = 19, scale = 2)
  private BigDecimal tbDifference;

  @Column(name = "verified")
  private Boolean verified;

  @Column(name = "verified_at")
  private Instant verifiedAt;

  protected YearEndClose() {}

  /**
   * Records a close.
   *
   * @param values close values
   */
  public YearEndClose(YearEndCloseValues values) {
    this.companyId = values.companyId();
    this.fiscalYearId = values.fiscalYearId();
    this.yearCode = values.yearCode();
    this.closingDate = values.closingDate();
    this.netResult = values.netResult();
    this.retainedEarningsAccount = values.retainedEarningsAccount();
    this.closingBatches = values.closingBatches();
    this.nextYearCode = values.nextYearCode();
    this.status = CLOSED;
  }

  /**
   * Records the post-close verification (FRBS 2.7.1): the nominal (income and expense) balances as
   * of the year end and the trial balance difference must both be zero.
   *
   * @param nominal net nominal balance as of the year end
   * @param tbDifference total debit minus total credit of the ledger as of the year end
   * @param when verification time
   */
  public void verify(BigDecimal nominal, BigDecimal tbDifference, Instant when) {
    this.nominalBalance = nominal;
    this.tbDifference = tbDifference;
    this.verified = nominal.signum() == 0 && tbDifference.signum() == 0;
    this.verifiedAt = when;
  }

  public BigDecimal getNominalBalance() {
    return nominalBalance;
  }

  public BigDecimal getTbDifference() {
    return tbDifference;
  }

  public Boolean getVerified() {
    return verified;
  }

  public Instant getVerifiedAt() {
    return verifiedAt;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getFiscalYearId() {
    return fiscalYearId;
  }

  public int getYearCode() {
    return yearCode;
  }

  public LocalDate getClosingDate() {
    return closingDate;
  }

  public BigDecimal getNetResult() {
    return netResult;
  }

  public String getRetainedEarningsAccount() {
    return retainedEarningsAccount;
  }

  public String getClosingBatches() {
    return closingBatches;
  }

  public Integer getNextYearCode() {
    return nextYearCode;
  }

  public String getStatus() {
    return status;
  }
}
