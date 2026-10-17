package com.iortatechnxt.brokerverse.payrequest.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * The GL account Comptrollership assigns to an account role of the liquidation event {@code
 * PRQ_CA_LIQUIDATION} (PER_DIEM, REPRESENTATION, TRANSPORT, LODGING, OTHER, CASH). Configuration,
 * not code: the real accounts come with the BDOI chart (AQ02, OQ07).
 */
@Entity
@Table(name = "prq_liquidation_account")
public class LiquidationAccount extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "account_role", nullable = false, length = 20, updatable = false)
  private String accountRole;

  @Column(name = "account_code", nullable = false, length = 30)
  private String accountCode;

  protected LiquidationAccount() {}

  /**
   * Assigns an account to a role.
   *
   * @param companyId company
   * @param accountRole role
   * @param accountCode GL account
   */
  public LiquidationAccount(Long companyId, String accountRole, String accountCode) {
    this.companyId = companyId;
    this.accountRole = accountRole;
    this.accountCode = accountCode;
  }

  /**
   * Changes the account.
   *
   * @param newAccountCode GL account
   */
  public void changeAccount(String newAccountCode) {
    this.accountCode = newAccountCode;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getAccountRole() {
    return accountRole;
  }

  public String getAccountCode() {
    return accountCode;
  }
}
