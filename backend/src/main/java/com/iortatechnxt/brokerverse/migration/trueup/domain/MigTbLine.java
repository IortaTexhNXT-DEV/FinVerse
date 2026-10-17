package com.iortatechnxt.brokerverse.migration.trueup.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A line of a legacy trial balance loaded by object G01 (DATA_MIGRATION_DESIGN 17.7): the
 * provisional opening or a true-up version (TU1-TU3, FINAL), mapped to the BIBS account, kept for
 * the movement and balance checks of the true-up reconciliation.
 */
@Entity
@Table(name = "mig_tb_line")
public class MigTbLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "batch_id", nullable = false, updatable = false)
  private Long batchId;

  @Column(name = "tb_version", nullable = false, length = 12, updatable = false)
  private String tbVersion;

  @Column(name = "as_of", nullable = false, updatable = false)
  private LocalDate asOf;

  @Column(name = "branch_id", nullable = false, updatable = false)
  private Long branchId;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(name = "legacy_account_code", nullable = false, length = 30, updatable = false)
  private String legacyAccountCode;

  @Column(name = "account_code", nullable = false, length = 30, updatable = false)
  private String accountCode;

  @Column(name = "cost_center", length = 20, updatable = false)
  private String costCenter;

  @Column(name = "debit_fc", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal debitFc;

  @Column(name = "credit_fc", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal creditFc;

  @Column(name = "debit_php", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal debitPhp;

  @Column(name = "credit_php", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal creditPhp;

  @Column(name = "rolled_back_at")
  private Instant rolledBackAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "created_by", nullable = false, length = 50, updatable = false)
  private String createdBy;

  protected MigTbLine() {}

  /**
   * A loaded line.
   *
   * @param key company, batch, version, as-of, branch and currency
   * @param account legacy and BIBS account and cost centre
   * @param amounts debit and credit in the currency and in pesos
   * @param user loading user
   * @param when time
   */
  public MigTbLine(Key key, Account account, Amounts amounts, String user, Instant when) {
    this.companyId = key.companyId();
    this.batchId = key.batchId();
    this.tbVersion = key.tbVersion();
    this.asOf = key.asOf();
    this.branchId = key.branchId();
    this.currency = key.currency();
    this.legacyAccountCode = account.legacyAccountCode();
    this.accountCode = account.accountCode();
    this.costCenter = account.costCenter();
    this.debitFc = amounts.debitFc();
    this.creditFc = amounts.creditFc();
    this.debitPhp = amounts.debitPhp();
    this.creditPhp = amounts.creditPhp();
    this.createdBy = user;
    this.createdAt = when;
  }

  /**
   * Marks the line of a rolled-back batch.
   *
   * @param when time
   */
  public void rollBack(Instant when) {
    this.rolledBackAt = when;
  }

  /**
   * Identity of a trial balance.
   *
   * @param companyId company
   * @param batchId loading batch
   * @param tbVersion PROVISIONAL, TU1, TU2, TU3 or FINAL
   * @param asOf as-of date
   * @param branchId branch
   * @param currency currency
   */
  public record Key(
      Long companyId,
      Long batchId,
      String tbVersion,
      LocalDate asOf,
      Long branchId,
      String currency) {}

  /**
   * Accounts of a line.
   *
   * @param legacyAccountCode legacy account
   * @param accountCode BIBS account (through code map GL_ACCOUNT)
   * @param costCenter cost centre
   */
  public record Account(String legacyAccountCode, String accountCode, String costCenter) {}

  /**
   * Amounts of a line.
   *
   * @param debitFc debit in the currency
   * @param creditFc credit in the currency
   * @param debitPhp debit in pesos
   * @param creditPhp credit in pesos
   */
  public record Amounts(
      BigDecimal debitFc, BigDecimal creditFc, BigDecimal debitPhp, BigDecimal creditPhp) {}

  public Long getId() {
    return id;
  }

  public Long getBatchId() {
    return batchId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getCurrency() {
    return currency;
  }

  public String getTbVersion() {
    return tbVersion;
  }

  public String getAccountCode() {
    return accountCode;
  }

  public Instant getRolledBackAt() {
    return rolledBackAt;
  }
}
