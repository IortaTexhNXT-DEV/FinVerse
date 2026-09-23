package com.iortatechnxt.finverse.investment.api.dto;

import com.iortatechnxt.finverse.common.domain.RecordStatus;
import com.iortatechnxt.finverse.investment.domain.Classification;
import com.iortatechnxt.finverse.investment.domain.InvestmentPortfolio;

/**
 * Investment portfolio view.
 *
 * @param id id
 * @param companyId company
 * @param code code
 * @param name name
 * @param classification classification
 * @param investmentAccount investment account
 * @param accruedInterestAccount accrued interest account
 * @param interestIncomeAccount interest income account
 * @param realizedGainAccount realized gain / loss account
 * @param fairValueAccount fair value account
 * @param recordStatus maker-checker status
 * @param createdBy maker
 * @param authorizedBy checker
 */
public record PortfolioResponse(
    Long id,
    Long companyId,
    String code,
    String name,
    Classification classification,
    String investmentAccount,
    String accruedInterestAccount,
    String interestIncomeAccount,
    String realizedGainAccount,
    String fairValueAccount,
    RecordStatus recordStatus,
    String createdBy,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param p portfolio
   * @return response
   */
  public static PortfolioResponse from(InvestmentPortfolio p) {
    return new PortfolioResponse(
        p.getId(),
        p.getCompanyId(),
        p.getCode(),
        p.getName(),
        p.getClassification(),
        p.getInvestmentAccount(),
        p.getAccruedInterestAccount(),
        p.getInterestIncomeAccount(),
        p.getRealizedGainAccount(),
        p.getFairValueAccount(),
        p.getRecordStatus(),
        p.getCreatedBy(),
        p.getAuthorizedBy());
  }
}
