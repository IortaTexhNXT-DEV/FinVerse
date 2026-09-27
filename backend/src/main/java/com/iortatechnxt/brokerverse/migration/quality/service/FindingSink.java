package com.iortatechnxt.brokerverse.migration.quality.service;

import com.iortatechnxt.brokerverse.migration.intake.domain.MigIssue.Finding;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;

/** Receives the findings of the rules. */
public interface FindingSink {

  /**
   * Records a finding on a row.
   *
   * @param row staged row
   * @param finding finding (severity may be overridden by the rule catalogue)
   */
  void add(StageRow row, Finding finding);

  /**
   * Records an error.
   *
   * @param row row
   * @param rule rule code
   * @param field column
   * @param value value
   * @param message message
   */
  default void error(StageRow row, String rule, String field, String value, String message) {
    add(row, Finding.error(rule, field, value, message));
  }

  /**
   * Records a warning.
   *
   * @param row row
   * @param rule rule code
   * @param field column
   * @param value value
   * @param message message
   */
  default void warning(StageRow row, String rule, String field, String value, String message) {
    add(row, Finding.warning(rule, field, value, message));
  }
}
