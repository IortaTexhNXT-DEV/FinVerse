package com.iortatechnxt.brokerverse.commission.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Incentive schemes (CMRID.005/006). */
public interface IncentiveSchemeRepository extends JpaRepository<IncentiveScheme, Long> {

  /**
   * Schemes of a company by code, with their tiers.
   *
   * @param companyId company
   * @return schemes
   */
  @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = "tiers")
  List<IncentiveScheme> findByCompanyIdOrderByCodeAsc(Long companyId);

  /**
   * A scheme by code.
   *
   * @param companyId company
   * @param code code
   * @return scheme
   */
  Optional<IncentiveScheme> findByCompanyIdAndCode(Long companyId, String code);

  /**
   * A scheme with its tiers.
   *
   * @param id id
   * @return scheme
   */
  @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = "tiers")
  Optional<IncentiveScheme> findWithTiersById(Long id);
}
