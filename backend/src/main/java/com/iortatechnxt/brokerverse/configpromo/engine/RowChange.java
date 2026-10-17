package com.iortatechnxt.brokerverse.configpromo.engine;

import java.util.List;
import java.util.Map;

/**
 * One item of a dataset in the difference between a package and the target.
 *
 * @param type kind of difference
 * @param keyText canonical text of the natural key
 * @param key natural key
 * @param values values of the package (added, changed) or of the target (only in target)
 * @param fields changed fields (changed items)
 * @param targetId id of the item in the target, null when added or without id
 */
public record RowChange(
    ChangeType type,
    String keyText,
    Map<String, Object> key,
    Map<String, Object> values,
    List<FieldChange> fields,
    Long targetId) {

  /** Defensive copies. */
  public RowChange {
    fields = fields == null ? List.of() : List.copyOf(fields);
  }
}
