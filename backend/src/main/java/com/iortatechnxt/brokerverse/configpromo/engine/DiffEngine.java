package com.iortatechnxt.brokerverse.configpromo.engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Compares the rows of a dataset in a package with the rows of the target, by natural key. */
public final class DiffEngine {

  private DiffEngine() {}

  /**
   * What the import of a dataset would do.
   *
   * @param m dataset model of the target
   * @param packageRows rows of the package
   * @param targetRows rows of the target
   * @param listOnlyInTarget whether items only in the target are listed (full mode)
   * @param parentKeys for a collection: key texts of the parents held by the package (their
   *     collections are replaced); ignored for other datasets
   * @return difference
   */
  public static DatasetDiff diff(
      DatasetModel m,
      List<Map<String, Object>> packageRows,
      List<CanonicalRow> targetRows,
      boolean listOnlyInTarget,
      Set<String> parentKeys) {
    Map<String, CanonicalRow> target = new LinkedHashMap<>();
    targetRows.forEach(r -> target.put(r.keyText(), r));
    List<RowChange> added = new ArrayList<>();
    List<RowChange> changed = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    int unchanged = 0;
    for (Map<String, Object> values : packageRows) {
      CanonicalRow row = CanonicalRow.of(null, values, m);
      if (isEnvironmentRow(m, row) || !seen.add(row.keyText())) {
        continue;
      }
      CanonicalRow existing = target.get(row.keyText());
      if (existing == null) {
        added.add(
            new RowChange(ChangeType.ADDED, row.keyText(), row.key(), values, List.of(), null));
        continue;
      }
      List<FieldChange> fields = fields(m, existing, row);
      if (fields.isEmpty()) {
        unchanged++;
      } else {
        changed.add(
            new RowChange(
                ChangeType.CHANGED, row.keyText(), row.key(), values, fields, existing.id()));
      }
    }
    List<RowChange> only = new ArrayList<>();
    for (CanonicalRow t : targetRows) {
      if (!seen.contains(t.keyText()) && listed(m, t, listOnlyInTarget, parentKeys)) {
        only.add(
            new RowChange(
                ChangeType.ONLY_IN_TARGET, t.keyText(), t.key(), t.values(), List.of(), t.id()));
      }
    }
    return new DatasetDiff(m.code(), added, changed, unchanged, only);
  }

  private static boolean isEnvironmentRow(DatasetModel m, CanonicalRow row) {
    return m.dataset().leavesOut(row::get);
  }

  private static boolean listed(
      DatasetModel m, CanonicalRow t, boolean listOnlyInTarget, Set<String> parentKeys) {
    if (m.dataset().collection()) {
      Object parent = t.get(m.dataset().parent());
      return parent != null && parentKeys.contains(CanonicalJson.text(parent));
    }
    return listOnlyInTarget;
  }

  private static List<FieldChange> fields(DatasetModel m, CanonicalRow target, CanonicalRow row) {
    List<FieldChange> fields = new ArrayList<>();
    for (String column : m.comparedColumns()) {
      Object from = target.get(column);
      Object to = row.get(column);
      if (!Objects.equals(from, to)) {
        fields.add(new FieldChange(column, from, to));
      }
    }
    return fields;
  }
}
