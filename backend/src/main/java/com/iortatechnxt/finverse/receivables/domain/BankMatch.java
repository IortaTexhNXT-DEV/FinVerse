package com.iortatechnxt.finverse.receivables.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Reconciliation match: one or more ledger entries of a bank GL account agreed with one or more
 * bank statement lines of the same (signed) total.
 */
@Entity
@Table(name = "brs_match")
public class BankMatch extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "bank_account_code", nullable = false, length = 30)
  private String bankAccountCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private MatchMethod method;

  @Column(name = "match_date", nullable = false)
  private LocalDate matchDate;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  protected BankMatch() {}

  /**
   * Creates a match.
   *
   * @param companyId company
   * @param bankAccountCode bank GL account
   * @param method auto or manual
   * @param matchDate reconciliation date (latest date of the matched items)
   * @param amount signed total (deposits positive)
   */
  public BankMatch(
      Long companyId,
      String bankAccountCode,
      MatchMethod method,
      LocalDate matchDate,
      BigDecimal amount) {
    this.companyId = companyId;
    this.bankAccountCode = bankAccountCode;
    this.method = method;
    this.matchDate = matchDate;
    this.amount = amount;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBankAccountCode() {
    return bankAccountCode;
  }

  public MatchMethod getMethod() {
    return method;
  }

  public LocalDate getMatchDate() {
    return matchDate;
  }

  public BigDecimal getAmount() {
    return amount;
  }
}
