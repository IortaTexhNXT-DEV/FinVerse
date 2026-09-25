package com.iortatechnxt.brokerverse.lov.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Cached snapshot of one list of values (all statuses), in display order.
 *
 * @param typeCode list type
 * @param known false when the list type does not exist
 * @param entries values
 */
public record LovEntries(String typeCode, boolean known, List<Entry> entries) {

  /** Defensive copy. */
  public LovEntries {
    entries = entries == null ? List.of() : List.copyOf(entries);
  }

  /**
   * One value.
   *
   * @param code code
   * @param label label
   * @param parentCode parent value (dependent lists)
   * @param active authorized and active
   * @param effectiveFrom first usable date
   * @param effectiveTo last usable date, null when open ended
   */
  public record Entry(
      String code,
      String label,
      String parentCode,
      boolean active,
      LocalDate effectiveFrom,
      LocalDate effectiveTo) {

    /**
     * Whether the value may be used on a date (same rule as {@code LovValue.isUsableOn}).
     *
     * @param date business date
     * @return true when usable
     */
    public boolean isUsableOn(LocalDate date) {
      return active
          && !date.isBefore(effectiveFrom)
          && (effectiveTo == null || !date.isAfter(effectiveTo));
    }
  }

  /**
   * A value by code.
   *
   * @param code code
   * @return value, empty when unknown
   */
  public Optional<Entry> find(String code) {
    return entries.stream().filter(e -> e.code().equals(code)).findFirst();
  }

  /**
   * Values usable on a date.
   *
   * @param date business date
   * @return usable values in display order
   */
  public List<Entry> usableOn(LocalDate date) {
    return entries.stream().filter(e -> e.isUsableOn(date)).toList();
  }
}
