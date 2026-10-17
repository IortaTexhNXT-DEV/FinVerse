package com.iortatechnxt.brokerverse.investment.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Investment portfolio: a PFRS 9 classification with the GL accounts its holdings post to
 * (investment, accrued interest, interest income, realized gain / loss and, for fair valued
 * portfolios, the fair value account - the equity reserve for FVOCI or a profit or loss account for
 * FVPL). Maintained under maker-checker control.
 */
@Entity
@Table(name = "inv_portfolio")
public class InvestmentPortfolio extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(nullable = false, length = 20)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Classification classification;

  @Column(name = "investment_account", nullable = false, length = 30)
  private String investmentAccount;

  @Column(name = "accrued_interest_account", nullable = false, length = 30)
  private String accruedInterestAccount;

  @Column(name = "interest_income_account", nullable = false, length = 30)
  private String interestIncomeAccount;

  @Column(name = "realized_gain_account", nullable = false, length = 30)
  private String realizedGainAccount;

  @Column(name = "fair_value_account", length = 30)
  private String fairValueAccount;

  protected InvestmentPortfolio() {}

  /**
   * Creates a portfolio (pending authorization).
   *
   * @param companyId company
   * @param code code
   * @param classification classification
   */
  public InvestmentPortfolio(Long companyId, String code, Classification classification) {
    this.companyId = companyId;
    this.code = code;
    this.classification = classification;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Classification getClassification() {
    return classification;
  }

  public String getInvestmentAccount() {
    return investmentAccount;
  }

  public void setInvestmentAccount(String investmentAccount) {
    this.investmentAccount = investmentAccount;
  }

  public String getAccruedInterestAccount() {
    return accruedInterestAccount;
  }

  public void setAccruedInterestAccount(String accruedInterestAccount) {
    this.accruedInterestAccount = accruedInterestAccount;
  }

  public String getInterestIncomeAccount() {
    return interestIncomeAccount;
  }

  public void setInterestIncomeAccount(String interestIncomeAccount) {
    this.interestIncomeAccount = interestIncomeAccount;
  }

  public String getRealizedGainAccount() {
    return realizedGainAccount;
  }

  public void setRealizedGainAccount(String realizedGainAccount) {
    this.realizedGainAccount = realizedGainAccount;
  }

  public String getFairValueAccount() {
    return fairValueAccount;
  }

  public void setFairValueAccount(String fairValueAccount) {
    this.fairValueAccount = fairValueAccount;
  }
}
