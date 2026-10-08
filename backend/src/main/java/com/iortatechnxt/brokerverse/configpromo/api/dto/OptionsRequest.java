package com.iortatechnxt.brokerverse.configpromo.api.dto;

import com.iortatechnxt.brokerverse.configpromo.engine.ImportOptions;
import java.util.List;
import java.util.Set;

/**
 * The choices of an import for a new dry run.
 *
 * @param datasets datasets of the package to import; empty = all
 * @param deactivate datasets whose items only in this environment are deactivated
 * @param includeUsers whether the users of the package are imported
 */
public record OptionsRequest(List<String> datasets, List<String> deactivate, boolean includeUsers) {

  /**
   * The engine options.
   *
   * @return options
   */
  public ImportOptions options() {
    return new ImportOptions(
        datasets == null ? Set.of() : Set.copyOf(datasets),
        deactivate == null ? Set.of() : Set.copyOf(deactivate),
        includeUsers);
  }
}
