package com.iortatechnxt.brokerverse.migration.load.service;

import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;

/**
 * Result of loading one unit: the BIBS record created or updated.
 *
 * @param target target entity, id, code and version
 * @param message remark for the batch log (may be null)
 */
public record LoadOutcome(KeyXref.Target target, String message) {

  /**
   * A loaded record.
   *
   * @param entity entity type
   * @param id id
   * @param code code
   * @param version optimistic-lock version
   * @return outcome
   */
  public static LoadOutcome of(String entity, Long id, String code, Long version) {
    return new LoadOutcome(new KeyXref.Target(entity, id, code, version), null);
  }
}
