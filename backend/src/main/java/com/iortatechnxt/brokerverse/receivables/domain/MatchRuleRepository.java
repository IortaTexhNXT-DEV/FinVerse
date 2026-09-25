package com.iortatechnxt.brokerverse.receivables.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link MatchRule}. */
public interface MatchRuleRepository extends JpaRepository<MatchRule, Long> {

  /**
   * Rules of a bank account in order.
   *
   * @param companyId company
   * @param bankAccountCode bank account
   * @return rules
   */
  List<MatchRule> findByCompanyIdAndBankAccountCodeOrderByPriority(
      Long companyId, String bankAccountCode);

  /**
   * One rule of a bank account.
   *
   * @param companyId company
   * @param bankAccountCode bank account
   * @param ruleCode rule
   * @return rule if any
   */
  Optional<MatchRule> findByCompanyIdAndBankAccountCodeAndRuleCode(
      Long companyId, String bankAccountCode, String ruleCode);
}
