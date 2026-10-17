package com.iortatechnxt.brokerverse.configpromo.engine;

/** How an item of the package compares with the target. */
public enum ChangeType {
  /** In the package only: added by the import. */
  ADDED,
  /** In both with different values: updated by the import. */
  CHANGED,
  /** In both with the same values. */
  UNCHANGED,
  /** In the target only: kept, deactivated when chosen, or removed from a replaced collection. */
  ONLY_IN_TARGET
}
