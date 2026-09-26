package com.iortatechnxt.brokerverse.investment.api.dto;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.investment.domain.Classification;
import com.iortatechnxt.brokerverse.investment.domain.InvestmentPortfolio;

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
 * @param createdBy creator
 * @param maker user who created or last maintained the record (unchanged by authorization)
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
    String maker,
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
        p.getMaker(),
        p.getAuthorizedBy());
  }
}
