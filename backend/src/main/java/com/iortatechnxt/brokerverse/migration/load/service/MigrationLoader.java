package com.iortatechnxt.brokerverse.migration.load.service;

import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SPI of a data object's loader (DATA_MIGRATION_DESIGN section 3.2): loads one unit through the
 * owning module's public service (never into its tables), reads a loaded record back for the field
 * reconciliation and, where the owning service allows it, undoes a record that has not changed
 * since the load. One bean per object; the batch framework does the chunking, the retries, the
 * cross-reference and the run log.
 */
public interface MigrationLoader {

  /**
   * The object loaded.
   *
   * @return object code, for example {@code F01}
   */
  String objectCode();

  /**
   * The sub-layouts joined to a main row (for example F01S and F01C for F01).
   *
   * @return sub-layout codes
   */
  default List<String> childLayouts() {
    return List.of();
  }

  /**
   * Groups the main rows into units; by default one unit per main row.
   *
   * @param units units built from the main rows
   * @return units to load, in load order
   */
  default List<LoadUnit> group(List<LoadUnit> units) {
    return units;
  }

  /**
   * The partition of a unit; units of one partition load in order in one thread (for example an
   * endorsement invoice with its parent).
   *
   * @param unit unit
   * @return partition key
   */
  default String partitionKey(LoadUnit unit) {
    return unit.legacyKey();
  }

  /**
   * Loads a unit through the owning service.
   *
   * @param unit unit
   * @param ctx batch context
   * @return the record created
   */
  LoadOutcome load(LoadUnit unit, LoadContext ctx);

  /**
   * Updates a loaded record whose legacy row changed (clients and headers before the freeze);
   * objects that cannot be updated leave the default, which refuses the row.
   *
   * @param unit unit
   * @param entry cross-reference of the loaded record
   * @param ctx batch context
   * @return the record updated, empty when updates are not supported
   */
  default Optional<LoadOutcome> update(LoadUnit unit, KeyXref entry, LoadContext ctx) {
    return Optional.empty();
  }

  /**
   * The columns compared by the field reconciliation (level L4).
   *
   * @return columns of the main layout
   */
  default List<String> reconciledColumns() {
    return List.of();
  }

  /**
   * Reads a loaded record back for the field reconciliation.
   *
   * @param entry cross-reference
   * @return target values keyed by the staged column names
   */
  default Map<String, String> readBack(KeyXref entry) {
    return Map.of();
  }

  /**
   * Whether the loaded record changed after the load (a changed record blocks the rollback).
   *
   * @param entry cross-reference
   * @return true when changed
   */
  default boolean changedSinceLoad(KeyXref entry) {
    return false;
  }

  /**
   * Whether the loader undoes records one by one (rollback before sign-off).
   *
   * @return true when {@link #compensate} is implemented
   */
  default boolean reversible() {
    return false;
  }

  /**
   * Undoes a loaded record through the owning service (rollback before sign-off).
   *
   * @param entry cross-reference
   * @param ctx batch context
   * @return true when undone; false when the object has no per-record rollback (snapshot only)
   */
  default boolean compensate(KeyXref entry, LoadContext ctx) {
    return false;
  }

  /**
   * The BIBS total of an amount measure over the loaded records (amount reconciliation L2).
   *
   * @param measure column, filter and currency of a control total
   * @param loaded cross-references of the batch
   * @return total, empty when the object has no BIBS amount for the measure
   */
  default Optional<BigDecimal> targetTotal(AmountMeasure measure, List<KeyXref> loaded) {
    return Optional.empty();
  }

  /**
   * A control-total measure.
   *
   * @param layoutCode layout
   * @param column amount column
   * @param filterColumn filter column, null when none
   * @param filterValue filter value
   * @param currency currency, null when all
   */
  record AmountMeasure(
      String layoutCode, String column, String filterColumn, String filterValue, String currency) {}
}
