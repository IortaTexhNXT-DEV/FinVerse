package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Minimum-field matrix rows. */
public interface FieldRuleRepository extends JpaRepository<FieldRule, Long> {

  /**
   * Rows of some scope codes ('*', a line, a product).
   *
   * @param scopeCodes scope codes
   * @return rows
   */
  List<FieldRule> findByScopeCodeInOrderBySortOrderAscIdAsc(Collection<String> scopeCodes);

  /**
   * Every row in display order.
   *
   * @return rows
   */
  List<FieldRule> findAllByOrderByScopeAscScopeCodeAscTargetAscSortOrderAsc();

  /**
   * One row by its identity.
   *
   * @param scope scope
   * @param scopeCode scope code
   * @param target target
   * @param fieldKey field key
   * @return row
   */
  Optional<FieldRule> findByScopeAndScopeCodeAndTargetAndFieldKey(
      RuleScope scope, String scopeCode, FieldTarget target, String fieldKey);
}
