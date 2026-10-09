package com.iortatechnxt.brokerverse.configpromo.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * The difference of one dataset between a package and the target.
 *
 * @param code dataset code
 * @param added items added by the import
 * @param changed items updated by the import
 * @param unchanged number of items equal on both sides
 * @param onlyInTarget items of the target the package does not hold (listed in full mode, and for
 *     the collections of the parents in the package)
 */
public record DatasetDiff(
    String code,
    List<RowChange> added,
    List<RowChange> changed,
    int unchanged,
    List<RowChange> onlyInTarget) {

  /** Defensive copies. */
  public DatasetDiff {
    added = List.copyOf(added);
    changed = List.copyOf(changed);
    onlyInTarget = List.copyOf(onlyInTarget);
  }

  /**
   * Whether the import changes nothing in the dataset (only-in-target items aside).
   *
   * @return true when nothing is added or changed
   */
  public boolean noChange() {
    return added.isEmpty() && changed.isEmpty();
  }

  /**
   * Every listed item: added, changed, then only in target.
   *
   * @return items
   */
  public List<RowChange> items() {
    List<RowChange> all = new ArrayList<>(added);
    all.addAll(changed);
    all.addAll(onlyInTarget);
    return all;
  }
}
