package com.iortatechnxt.brokerverse.receivables.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Auto-matching rule of a bank account (FRBS 3.3.2): {@link #CHECK_NO_AND_AMOUNT} matches a bank
 * line and a book entry with the same cheque number and amount before the {@link #STANDARD} rules
 * (amount within the date window, reference preference, deposit slips).
 */
@Entity
@Table(name = "brs_match_rule")
public class MatchRule extends BaseEntity {

  /** Exact cheque number and amount. */
  public static final String CHECK_NO_AND_AMOUNT = "CHECK_NO_AND_AMOUNT";

  /** Amount, date window, reference, deposit slips. */
  public static final String STANDARD = "STANDARD";

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "bank_account_code", nullable = false, length = 30)
  private String bankAccountCode;

  @Column(name = "rule_code", nullable = false, length = 30)
  private String ruleCode;

  @Column(nullable = false)
  private int priority;

  @Column(nullable = false)
  private boolean active = true;

  protected MatchRule() {}

  /**
   * Creates a rule.
   *
   * @param companyId company
   * @param bankAccountCode bank account
   * @param ruleCode rule
   * @param priority order
   */
  public MatchRule(Long companyId, String bankAccountCode, String ruleCode, int priority) {
    this.companyId = companyId;
    this.bankAccountCode = bankAccountCode;
    this.ruleCode = ruleCode;
    this.priority = priority;
  }

  /**
   * Switches the rule on or off.
   *
   * @param on whether applied
   */
  public void setActive(boolean on) {
    this.active = on;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBankAccountCode() {
    return bankAccountCode;
  }

  public String getRuleCode() {
    return ruleCode;
  }

  public int getPriority() {
    return priority;
  }

  public boolean isActive() {
    return active;
  }
}
