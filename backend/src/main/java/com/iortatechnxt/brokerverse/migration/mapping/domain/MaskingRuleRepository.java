package com.iortatechnxt.brokerverse.migration.mapping.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Masking rules. */
public interface MaskingRuleRepository extends JpaRepository<MaskingRule, Long> {

  /**
   * The active rules of a layout.
   *
   * @param layoutCode layout
   * @return rules
   */
  List<MaskingRule> findByLayoutCodeAndActiveTrue(String layoutCode);

  /**
   * Every rule.
   *
   * @return rules by layout and column
   */
  List<MaskingRule> findAllByOrderByLayoutCodeAscColumnNameAsc();
}
