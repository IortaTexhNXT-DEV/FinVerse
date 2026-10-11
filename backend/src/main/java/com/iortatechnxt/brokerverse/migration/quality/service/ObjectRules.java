package com.iortatechnxt.brokerverse.migration.quality.service;

import java.util.Set;

/**
 * The object-specific data-quality rules of one or more layouts (DATA_MIGRATION_DESIGN section 8;
 * workbook rules DQ-006 onwards): cross-field, balance, referential, plausibility and cross-row
 * checks run after the generic checks. Each finding names its rule code; the catalogue decides
 * whether the rule is active and its severity.
 */
public interface ObjectRules {

  /**
   * The layouts checked.
   *
   * @return layout codes
   */
  Set<String> layouts();

  /**
   * Checks the rows of a batch.
   *
   * @param scope rows and look-ups of the batch
   * @param sink findings
   */
  void check(ValidationScope scope, FindingSink sink);
}
