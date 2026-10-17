package com.iortatechnxt.brokerverse.catalog.service;

import java.util.List;
import java.util.Set;

/**
 * Which fields of a record (account, quotation) and of each of its risk items hold a value, for the
 * minimum-field check (BRNB.002/003/093). The owning module decides what "has a value" means (not
 * blank, positive amount, at least one element).
 *
 * @param record keys of the record-level fields that have a value (include "items" when there is at
 *     least one risk item)
 * @param items keys with a value, per risk item in order
 */
public record FieldPresence(Set<String> record, List<Set<String>> items) {

  /** Defensive copies. */
  public FieldPresence {
    record = record == null ? Set.of() : Set.copyOf(record);
    items = items == null ? List.of() : items.stream().map(Set::copyOf).toList();
  }
}
