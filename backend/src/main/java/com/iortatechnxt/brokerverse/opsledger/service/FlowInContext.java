package com.iortatechnxt.brokerverse.opsledger.service;

import java.util.function.Supplier;

/**
 * What a feed run's work sees (BRQID.005): {@link #accept} processes one record once per
 * idempotency key in its own transaction, so a failing record never stops the run (BRQID.006) and a
 * record already accepted is skipped as a duplicate.
 */
public interface FlowInContext {

  /**
   * Processes one record unless a record with the same key was already accepted.
   *
   * @param idempotencyKey key of the record in the feed (e.g. invoice no., bank reference)
   * @param payload the record's content, hashed and kept with the record
   * @param work processes the record and returns the reference of what it created; throw a {@code
   *     BusinessRuleException} or {@code ResourceNotFoundException} to fail the record only
   * @return true when processed now, false when failed or skipped as a duplicate
   */
  boolean accept(String idempotencyKey, String payload, Supplier<String> work);

  /**
   * Run number (reference for records created from the run).
   *
   * @return run number
   */
  String runNo();
}
