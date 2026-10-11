package com.iortatechnxt.brokerverse.investment.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link InvestmentPortfolio}. */
public interface InvestmentPortfolioRepository extends JpaRepository<InvestmentPortfolio, Long> {

  /**
   * Lists portfolios.
   *
   * @param companyId company
   * @return portfolios by code
   */
  List<InvestmentPortfolio> findByCompanyIdOrderByCode(Long companyId);

  /**
   * Finds a portfolio by code.
   *
   * @param companyId company
   * @param code code
   * @return portfolio if present
   */
  Optional<InvestmentPortfolio> findByCompanyIdAndCode(Long companyId, String code);

  /**
   * Checks whether a code is taken.
   *
   * @param companyId company
   * @param code code
   * @return true when taken
   */
  boolean existsByCompanyIdAndCode(Long companyId, String code);
}
