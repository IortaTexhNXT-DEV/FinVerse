package com.iortatechnxt.brokerverse.configpromo.engine;

import java.util.Set;

/**
 * Choices of an import.
 *
 * @param datasets datasets of the package to import; empty = all
 * @param deactivate datasets whose items only in the target are deactivated (full mode)
 * @param includeUsers whether the user datasets of the package are imported
 */
public record ImportOptions(Set<String> datasets, Set<String> deactivate, boolean includeUsers) {

  /** Defensive copies. */
  public ImportOptions {
    datasets = datasets == null ? Set.of() : Set.copyOf(datasets);
    deactivate = deactivate == null ? Set.of() : Set.copyOf(deactivate);
  }
}
