package com.iortatechnxt.brokerverse.configpromo.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The dry run of an import: the difference of every dataset of the package with the target, the
 * references that resolve neither in the package nor in the target, the items whose deactivation is
 * refused because records use them, and the users named by the package that the target lacks.
 */
public final class ImportAnalyzer {

  private final DatasetReader reader;
  private final UsageChecker usage;
  private final Predicate<String> userExists;

  /**
   * Creates the analyzer.
   *
   * @param jdbc target database
   * @param reader reader of the target
   * @param userExists whether a user name exists in the target
   */
  public ImportAnalyzer(JdbcTemplate jdbc, DatasetReader reader, Predicate<String> userExists) {
    this.reader = reader;
    this.usage = new UsageChecker(jdbc, reader);
    this.userExists = userExists;
  }

  /**
   * The datasets of a package an import takes, in the load order of the target.
   *
   * @param pkg package
   * @param options choices
   * @return dataset codes
   */
  public List<String> datasets(ConfigPackage pkg, ImportOptions options) {
    CatalogueModel model = reader.model();
    List<String> codes = new ArrayList<>();
    for (ManifestDataset d : pkg.manifest().datasets()) {
      boolean chosen = options.datasets().isEmpty() || options.datasets().contains(d.code());
      boolean users = model.has(d.code()) && model.model(d.code()).dataset().users();
      if (chosen && model.has(d.code()) && (!users || options.includeUsers())) {
        codes.add(d.code());
      }
    }
    return model.loadOrder(codes);
  }

  /**
   * Runs the dry run.
   *
   * @param pkg verified, compatible package
   * @param options choices
   * @return analysis
   */
  public Analysis analyze(ConfigPackage pkg, ImportOptions options) {
    CatalogueModel model = reader.model();
    boolean full = PackageManifest.FULL.equals(pkg.manifest().mode());
    List<String> codes = datasets(pkg, options);
    Map<String, Set<String>> packageKeys = new HashMap<>();
    for (String code : codes) {
      Set<String> keys = new HashSet<>();
      pkg.rows(code).forEach(r -> keys.add(CanonicalRow.of(null, r, model.model(code)).keyText()));
      packageKeys.put(code, keys);
    }
    List<DatasetDiff> diffs = new ArrayList<>();
    List<Issue> blockers = new ArrayList<>();
    List<Issue> warnings = new ArrayList<>();
    for (String code : codes) {
      DatasetModel m = model.model(code);
      Set<String> parents =
          m.dataset().collection()
              ? packageKeys.getOrDefault(
                  m.references().get(m.dataset().parent()).dataset(), Set.of())
              : Set.of();
      DatasetDiff diff = DiffEngine.diff(m, pkg.rows(code), reader.rows(code), full, parents);
      diffs.add(diff);
      checkReferences(m, diff, packageKeys, blockers);
      checkDeactivations(m, diff, full && options.deactivate().contains(code), blockers, warnings);
      checkUsers(m, diff, warnings);
    }
    return new Analysis(diffs, blockers, warnings);
  }

  private void checkReferences(
      DatasetModel m, DatasetDiff diff, Map<String, Set<String>> packageKeys, List<Issue> out) {
    for (RowChange row : concat(diff.added(), diff.changed())) {
      for (Reference ref : m.references().values()) {
        Object value = row.values().get(ref.column());
        if (value == null) {
          continue;
        }
        boolean inPackage =
            packageKeys.getOrDefault(ref.dataset(), Set.of()).contains(CanonicalJson.text(value));
        if (!inPackage && reader.idOf(ref.dataset(), value).isEmpty()) {
          out.add(
              new Issue(
                  m.code(),
                  row.keyText(),
                  "Refers to "
                      + reader.model().model(ref.dataset()).dataset().name()
                      + " "
                      + CanonicalJson.text(value)
                      + ", which exists neither in the package nor in this environment"));
        }
      }
    }
  }

  private void checkDeactivations(
      DatasetModel m,
      DatasetDiff diff,
      boolean deactivate,
      List<Issue> blockers,
      List<Issue> warnings) {
    Optional<Deactivation> how = m.deactivation();
    if (deactivate && how.isEmpty() && !diff.onlyInTarget().isEmpty()) {
      warnings.add(
          new Issue(
              m.code(),
              null,
              diff.onlyInTarget().size()
                  + " item(s) only in this environment cannot be deactivated; they are kept"));
    }
    if (how.isEmpty()) {
      return;
    }
    List<RowChange> candidates = new ArrayList<>();
    if (deactivate) {
      diff.onlyInTarget().stream()
          .filter(r -> !how.get().isInactive(r.values().get(how.get().column())))
          .forEach(candidates::add);
    }
    diff.changed().stream().filter(r -> becomesInactive(r, how.get())).forEach(candidates::add);
    for (RowChange row : candidates) {
      long uses = usage.count(m, withTargetValues(row));
      if (uses > 0) {
        blockers.add(
            new Issue(
                m.code(),
                row.keyText(),
                "Is used by " + uses + " record(s) in this environment; it cannot be deactivated"));
      }
    }
  }

  private static boolean becomesInactive(RowChange row, Deactivation how) {
    return row.fields().stream()
        .anyMatch(f -> f.column().equals(how.column()) && how.isInactive(f.to()));
  }

  /** A changed item with the values of the target, as the usage check reads the target. */
  private static RowChange withTargetValues(RowChange row) {
    if (row.type() != ChangeType.CHANGED) {
      return row;
    }
    Map<String, Object> values = new HashMap<>(row.values());
    row.fields().forEach(f -> values.put(f.column(), f.from()));
    return new RowChange(
        row.type(), row.keyText(), row.key(), values, row.fields(), row.targetId());
  }

  private void checkUsers(DatasetModel m, DatasetDiff diff, List<Issue> warnings) {
    for (RowChange row : concat(diff.added(), diff.changed())) {
      for (String column : m.dataset().userColumns()) {
        Object user = row.values().get(column);
        if (user != null && !user.toString().isBlank() && !userExists.test(user.toString())) {
          warnings.add(
              new Issue(
                  m.code(),
                  row.keyText(),
                  "Names the user " + user + ", who does not exist in this environment"));
        }
      }
    }
  }

  private static List<RowChange> concat(List<RowChange> a, List<RowChange> b) {
    List<RowChange> all = new ArrayList<>(a);
    all.addAll(b);
    return all;
  }
}
