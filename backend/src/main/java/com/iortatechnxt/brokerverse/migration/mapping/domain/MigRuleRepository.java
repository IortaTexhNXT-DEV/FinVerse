package com.iortatechnxt.brokerverse.migration.mapping.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data-quality rules. */
public interface MigRuleRepository extends JpaRepository<MigRule, Long> {

  /**
   * A rule by code.
   *
   * @param code code
   * @return rule
   */
  Optional<MigRule> findByCode(String code);

  /**
   * Every rule by code.
   *
   * @return rules
   */
  List<MigRule> findAllByOrderByCodeAsc();
}
