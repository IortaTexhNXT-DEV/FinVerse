package com.iortatechnxt.brokerverse.configpromo.engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reads the datasets of a package back from the target and compares counts and checksums. */
public final class Reconciler {

  private Reconciler() {}

  /**
   * Reconciles datasets.
   *
   * @param reader reader of the target (after the import)
   * @param pkg package
   * @param codes datasets imported
   * @return one line per dataset
   */
  public static List<Reconciliation> reconcile(
      DatasetReader reader, ConfigPackage pkg, List<String> codes) {
    List<Reconciliation> lines = new ArrayList<>();
    for (String code : codes) {
      DatasetModel m = reader.model().model(code);
      List<Map<String, Object>> packageRows = pkg.rows(code);
      Set<String> keys = new HashSet<>();
      packageRows.forEach(r -> keys.add(CanonicalRow.of(null, r, m).keyText()));
      reader.forget(code);
      List<CanonicalRow> all = reader.rows(code);
      List<Map<String, Object>> matching =
          all.stream().filter(r -> keys.contains(r.keyText())).map(CanonicalRow::values).toList();
      List<Map<String, Object>> sortedPackage =
          packageRows.stream()
              .sorted(
                  (a, b) ->
                      CanonicalRow.of(null, a, m)
                          .keyText()
                          .compareTo(CanonicalRow.of(null, b, m).keyText()))
              .toList();
      lines.add(
          new Reconciliation(
              code,
              packageRows.size(),
              matching.size(),
              all.size(),
              Checksums.content(sortedPackage, m.comparedColumns()),
              Checksums.content(matching, m.comparedColumns())));
    }
    return lines;
  }
}
