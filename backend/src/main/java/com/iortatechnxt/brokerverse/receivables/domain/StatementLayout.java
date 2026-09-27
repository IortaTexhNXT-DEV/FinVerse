package com.iortatechnxt.brokerverse.receivables.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Column mapping of the spreadsheet statements (.xlsx / .ods) of one bank account (FRBS 3.3.1;
 * AQ08): which column holds the date, description, reference (cheque number), debit and credit or
 * one signed amount, and the running balance. Column names are matched case-insensitively.
 */
@Entity
@Table(name = "brs_statement_layout")
public class StatementLayout extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "bank_account_code", nullable = false, length = 30)
  private String bankAccountCode;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(name = "date_column", nullable = false, length = 60)
  private String dateColumn;

  @Column(name = "description_column", length = 60)
  private String descriptionColumn;

  @Column(name = "reference_column", length = 60)
  private String referenceColumn;

  @Column(name = "debit_column", length = 60)
  private String debitColumn;

  @Column(name = "credit_column", length = 60)
  private String creditColumn;

  @Column(name = "amount_column", length = 60)
  private String amountColumn;

  @Column(name = "balance_column", length = 60)
  private String balanceColumn;

  @Column(name = "date_pattern", nullable = false, length = 20)
  private String datePattern;

  protected StatementLayout() {}

  /**
   * Creates a layout.
   *
   * @param companyId company
   * @param bankAccountCode GL bank account
   * @param values mapping
   */
  public StatementLayout(Long companyId, String bankAccountCode, StatementLayoutValues values) {
    this.companyId = companyId;
    this.bankAccountCode = bankAccountCode;
    change(values);
  }

  /**
   * Changes the mapping.
   *
   * @param v mapping
   */
  public final void change(StatementLayoutValues v) {
    this.name = v.name();
    this.dateColumn = v.dateColumn();
    this.descriptionColumn = v.descriptionColumn();
    this.referenceColumn = v.referenceColumn();
    this.debitColumn = v.debitColumn();
    this.creditColumn = v.creditColumn();
    this.amountColumn = v.amountColumn();
    this.balanceColumn = v.balanceColumn();
    this.datePattern = v.datePattern();
  }

  /**
   * The mapping.
   *
   * @return values
   */
  public StatementLayoutValues values() {
    return new StatementLayoutValues(
        name,
        dateColumn,
        descriptionColumn,
        referenceColumn,
        debitColumn,
        creditColumn,
        amountColumn,
        balanceColumn,
        datePattern);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBankAccountCode() {
    return bankAccountCode;
  }
}
