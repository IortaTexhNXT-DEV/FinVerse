package com.iortatechnxt.brokerverse.configpromo.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Applies the difference of a package to the target, in load order (a dataset after the datasets it
 * references), in the caller's transaction: updates, then inserts, then the references of a dataset
 * to its own rows, then the removal of items of replaced collections and the chosen deactivations.
 * References are remapped from the natural key in the package to the id of the same row in the
 * target.
 */
public final class ApplyEngine {

  private ApplyEngine() {}

  /**
   * Applies differences.
   *
   * @param jdbc target database (in a transaction)
   * @param reader reader of the target (same connection)
   * @param diffs differences in load order
   * @param deactivate datasets whose items only in the target are deactivated
   * @param actors maker, checker and time
   * @return result per dataset
   */
  public static List<DatasetResult> apply(
      JdbcTemplate jdbc,
      DatasetReader reader,
      List<DatasetDiff> diffs,
      Set<String> deactivate,
      ApplyActors actors) {
    CatalogueModel model = reader.model();
    List<DatasetResult> results = new ArrayList<>();
    for (DatasetDiff diff : diffs) {
      DatasetModel m = model.model(diff.code());
      RowWriter writer = new RowWriter(jdbc, reader, model, m, actors);
      diff.changed().forEach(writer::update);
      diff.added().forEach(writer::insert);
      reader.forget(m.code());
      diff.added().forEach(writer::writeDeferred);
      diff.changed().forEach(writer::writeDeferred);
      int deactivated = 0;
      int removed = 0;
      if (m.dataset().collection()) {
        for (RowChange row : diff.onlyInTarget()) {
          writer.delete(row);
          removed++;
        }
      } else if (deactivate.contains(m.code())) {
        deactivated = deactivate(writer, m, diff);
      }
      reader.forget(m.code());
      results.add(
          new DatasetResult(
              m.code(), diff.added().size(), diff.changed().size(), deactivated, removed));
    }
    return results;
  }

  private static int deactivate(RowWriter writer, DatasetModel m, DatasetDiff diff) {
    Optional<Deactivation> how = m.deactivation();
    if (how.isEmpty()) {
      return 0;
    }
    int count = 0;
    for (RowChange row : diff.onlyInTarget()) {
      if (!how.get().isInactive(row.values().get(how.get().column()))) {
        writer.deactivate(row, how.get());
        count++;
      }
    }
    return count;
  }
}
