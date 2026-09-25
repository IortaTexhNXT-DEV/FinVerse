package com.iortatechnxt.brokerverse.acsl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * One control account in a GL-SL reconciliation (ACSL 2.13.2): the GL balance, the sub-ledger
 * balance and the difference, all debit positive in base currency.
 */
@Entity
@Table(name = "acsl_glsl_recon")
public class GlSlRecon {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "run_id", nullable = false, updatable = false)
  private Long runId;

  @Column(name = "account_code", nullable = false, length = 30, updatable = false)
  private String accountCode;

  @Column(name = "account_name", nullable = false, length = 200, updatable = false)
  private String accountName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private SlSource source;

  @Column(name = "gl_balance", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal glBalance;

  @Column(name = "sl_balance", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal slBalance;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal difference;

  protected GlSlRecon() {}

  /**
   * Records one account.
   *
   * @param runId run
   * @param accountCode control account
   * @param accountName its name
   * @param source sub-ledger compared
   * @param glBalance GL balance (debit positive)
   * @param slBalance sub-ledger balance (debit positive)
   */
  public GlSlRecon(
      Long runId,
      String accountCode,
      String accountName,
      SlSource source,
      BigDecimal glBalance,
      BigDecimal slBalance) {
    this.runId = runId;
    this.accountCode = accountCode;
    this.accountName = accountName;
    this.source = source;
    this.glBalance = glBalance;
    this.slBalance = slBalance;
    this.difference = glBalance.subtract(slBalance);
  }

  public Long getId() {
    return id;
  }

  public Long getRunId() {
    return runId;
  }

  public String getAccountCode() {
    return accountCode;
  }

  public String getAccountName() {
    return accountName;
  }

  public SlSource getSource() {
    return source;
  }

  public BigDecimal getGlBalance() {
    return glBalance;
  }

  public BigDecimal getSlBalance() {
    return slBalance;
  }

  public BigDecimal getDifference() {
    return difference;
  }
}
