package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Document checklist rows. */
public interface DocumentRuleRepository extends JpaRepository<DocumentRule, Long> {

  /**
   * Rows of some scope codes ('*', a line, a product).
   *
   * @param scopeCodes scope codes
   * @return rows
   */
  List<DocumentRule> findByScopeCodeIn(Collection<String> scopeCodes);

  /**
   * Every row.
   *
   * @return rows
   */
  List<DocumentRule> findAllByOrderByScopeAscScopeCodeAscDocumentTypeAsc();

  /**
   * One row by its identity.
   *
   * @param scope scope
   * @param scopeCode scope code
   * @param documentType document type
   * @return row
   */
  Optional<DocumentRule> findByScopeAndScopeCodeAndDocumentType(
      RuleScope scope, String scopeCode, String documentType);
}
