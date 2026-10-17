package com.iortatechnxt.brokerverse.migration.load.service;

import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One unit loaded in one call of a loader: the row of the object's main layout with the rows of its
 * sub-layouts joined on the key (for example an invoice header with its insurer shares and
 * components), or a group of rows posted together (a trial balance per branch and currency).
 *
 * @param legacyKey key of the unit in the cross-reference
 * @param sourceSystem source system
 * @param main main row (its mapped values)
 * @param children rows of the sub-layouts by layout code
 * @param hash hash of the unit (changes when any of its rows changes)
 * @param aliases other legacy keys loaded into the same record (merged clients)
 */
public record LoadUnit(
    String legacyKey,
    String sourceSystem,
    StageRow main,
    Map<String, List<StageRow>> children,
    String hash,
    List<Alias> aliases) {

  /** Child layout of the rows merged into the main row (clients of one cluster). */
  public static final String MERGED = "MERGED";

  /** Defensive copies. */
  public LoadUnit {
    children = Map.copyOf(children);
    aliases = aliases == null ? List.of() : List.copyOf(aliases);
  }

  /**
   * A unit without aliases.
   *
   * @param legacyKey key
   * @param sourceSystem source system
   * @param main main row
   * @param children child rows
   * @param hash hash
   */
  public LoadUnit(
      String legacyKey,
      String sourceSystem,
      StageRow main,
      Map<String, List<StageRow>> children,
      String hash) {
    this(legacyKey, sourceSystem, main, children, hash, List.of());
  }

  /**
   * The same unit with other children and aliases.
   *
   * @param newChildren children
   * @param newAliases aliases
   * @return unit
   */
  public LoadUnit with(Map<String, List<StageRow>> newChildren, List<Alias> newAliases) {
    return new LoadUnit(legacyKey, sourceSystem, main, newChildren, hash, newAliases);
  }

  /**
   * Another legacy key of the same record.
   *
   * @param sourceSystem source system
   * @param legacyKey legacy key
   * @param rowHash hash of its row
   */
  public record Alias(String sourceSystem, String legacyKey, String rowHash) {}

  /**
   * A mapped value of the main row.
   *
   * @param column column
   * @return value, null when blank
   */
  public String value(String column) {
    return main.getMappedPayload().get(column);
  }

  /**
   * Mapped values of the main row.
   *
   * @return values
   */
  public Map<String, String> values() {
    return main.getMappedPayload();
  }

  /**
   * Rows of a sub-layout.
   *
   * @param layoutCode layout
   * @return rows (empty when none)
   */
  public List<StageRow> rows(String layoutCode) {
    return children.getOrDefault(layoutCode, List.of());
  }

  /**
   * Every row of the unit.
   *
   * @return main row and child rows
   */
  public List<StageRow> allRows() {
    List<StageRow> all = new ArrayList<>();
    all.add(main);
    children.values().forEach(all::addAll);
    return all;
  }
}
