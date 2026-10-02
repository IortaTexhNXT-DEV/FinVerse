package com.iortatechnxt.brokerverse.remittance.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Early-remittance incentive rules. */
public interface EarlyIncentiveRuleRepository extends JpaRepository<EarlyIncentiveRule, Long> {

  /**
   * Active rules of an insurer.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @return rules, oldest first
   */
  List<EarlyIncentiveRule> findByCompanyIdAndInsurerCodeAndActiveTrueOrderByIdAsc(
      Long companyId, String insurerCode);

  /**
   * Rules of a company.
   *
   * @param companyId company
   * @return rules by insurer
   */
  List<EarlyIncentiveRule> findByCompanyIdOrderByInsurerCodeAscIdAsc(Long companyId);
}
