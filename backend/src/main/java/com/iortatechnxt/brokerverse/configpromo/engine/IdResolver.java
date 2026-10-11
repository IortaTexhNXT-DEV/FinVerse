package com.iortatechnxt.brokerverse.configpromo.engine;

import java.util.Optional;

/** Finds the id of a row of the target from its natural key (the remapping of references). */
@FunctionalInterface
public interface IdResolver {

  /**
   * The id of the row of a dataset with a natural key.
   *
   * @param dataset dataset code
   * @param key natural key as exported
   * @return id, empty when the target has no such row
   */
  Optional<Long> idOf(String dataset, Object key);
}
