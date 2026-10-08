package com.iortatechnxt.brokerverse.configpromo.engine;

import com.iortatechnxt.brokerverse.configpromo.catalogue.CatalogueDataset;
import com.iortatechnxt.brokerverse.configpromo.catalogue.ConfigCatalogue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A catalogue dataset resolved against the schema of a database: the promoted columns, the columns
 * that reference other rows by id, the datasets it depends on and how its items are deactivated.
 */
public final class DatasetModel {

  /** Columns never promoted: surrogate key, optimistic lock and audit columns. */
  public static final Set<String> AUDIT_COLUMNS =
      Set.of(
          "id",
          "version",
          "created_at",
          "created_by",
          "updated_at",
          "updated_by",
          "authorized_by",
          "authorized_at");

  private static final String RECORD_STATUS = "record_status";
  private static final String ACTIVE = "active";

  private final CatalogueDataset dataset;
  private final TableSchema table;
  private final List<String> columns;
  private final Map<String, Reference> references;
  private final Set<String> dependencies;
  private final Set<String> deferredColumns;
  private final List<String> errors;

  private DatasetModel(
      CatalogueDataset dataset,
      TableSchema table,
      List<String> columns,
      Map<String, Reference> references,
      Set<String> dependencies,
      Set<String> deferredColumns,
      List<String> errors) {
    this.dataset = dataset;
    this.table = table;
    this.columns = List.copyOf(columns);
    this.references = Collections.unmodifiableMap(references);
    this.dependencies = Collections.unmodifiableSet(dependencies);
    this.deferredColumns = Collections.unmodifiableSet(deferredColumns);
    this.errors = List.copyOf(errors);
  }

  /**
   * Resolves a dataset against its table.
   *
   * @param dataset catalogue dataset
   * @param table table of the database
   * @param catalogue catalogue (datasets of referenced tables)
   * @return model; {@link #errors()} lists what does not fit
   */
  public static DatasetModel of(
      CatalogueDataset dataset, TableSchema table, ConfigCatalogue catalogue) {
    List<String> errors = new ArrayList<>();
    for (String c : concat(dataset.key(), dataset.exclude(), dataset.environment())) {
      if (!table.has(c)) {
        errors.add(dataset.code() + ": unknown column " + c);
      }
    }
    Set<String> skipped = new LinkedHashSet<>(AUDIT_COLUMNS);
    skipped.addAll(dataset.exclude());
    List<String> columns =
        table.columns().keySet().stream().filter(c -> !skipped.contains(c)).toList();
    Map<String, Reference> refs = new LinkedHashMap<>();
    Set<String> deps = new LinkedHashSet<>();
    Set<String> deferred = new LinkedHashSet<>();
    for (ForeignKey fk : table.foreignKeys()) {
      if (columns.containsAll(fk.columns())) {
        link(dataset, catalogue, fk, new Links(refs, deps, deferred, errors));
      }
    }
    dataset
        .refs()
        .forEach(
            (column, target) -> {
              refs.put(column, new Reference(column, target));
              addDependency(dataset, target, column, new Links(refs, deps, deferred, errors));
            });
    checkDeferred(dataset, table, deferred, errors);
    return new DatasetModel(dataset, table, columns, refs, deps, deferred, errors);
  }

  private record Links(
      Map<String, Reference> refs, Set<String> deps, Set<String> deferred, List<String> errors) {}

  private static void link(
      CatalogueDataset dataset, ConfigCatalogue catalogue, ForeignKey fk, Links links) {
    Optional<CatalogueDataset> target = catalogue.byTable(fk.table());
    if (target.isEmpty()) {
      links.errors().add(
              dataset.code()
                  + ": column "
                  + String.join(",", fk.columns())
                  + " refers to "
                  + fk.table()
                  + ", which is not configuration");
      return;
    }
    if (fk.byId()) {
      links.refs().put(fk.columns().get(0), new Reference(fk.columns().get(0), target.get().code()));
      addDependency(dataset, target.get().code(), fk.columns().get(0), links);
    } else if (target.get().code().equals(dataset.code())) {
      fk.columns().stream().filter(c -> !dataset.key().contains(c)).forEach(links.deferred()::add);
    } else {
      links.deps().add(target.get().code());
    }
  }

  private static void addDependency(
      CatalogueDataset dataset, String target, String column, Links links) {
    if (target.equals(dataset.code())) {
      links.deferred().add(column);
    } else {
      links.deps().add(target);
    }
  }

  private static void checkDeferred(
      CatalogueDataset dataset, TableSchema table, Set<String> deferred, List<String> errors) {
    for (String c : deferred) {
      if (!table.column(c).nullable()) {
        errors.add(dataset.code() + ": the self reference " + c + " must accept null");
      }
    }
  }

  private static List<String> concat(List<String> a, List<String> b, List<String> c) {
    List<String> all = new ArrayList<>(a);
    all.addAll(b);
    all.addAll(c);
    return all;
  }

  /**
   * The catalogue dataset.
   *
   * @return dataset
   */
  public CatalogueDataset dataset() {
    return dataset;
  }

  /**
   * Dataset code.
   *
   * @return code
   */
  public String code() {
    return dataset.code();
  }

  /**
   * The table.
   *
   * @return table
   */
  public TableSchema table() {
    return table;
  }

  /**
   * Promoted columns in table order.
   *
   * @return columns
   */
  public List<String> columns() {
    return columns;
  }

  /**
   * Columns compared between package and target: the promoted columns without the environment
   * columns.
   *
   * @return compared columns
   */
  public List<String> comparedColumns() {
    return columns.stream().filter(c -> !dataset.environment().contains(c)).toList();
  }

  /**
   * References by id, by column.
   *
   * @return references
   */
  public Map<String, Reference> references() {
    return references;
  }

  /**
   * Datasets that must be loaded first.
   *
   * @return dataset codes
   */
  public Set<String> dependencies() {
    return dependencies;
  }

  /**
   * Columns referencing rows of the same dataset, written after every row of the dataset exists.
   *
   * @return columns
   */
  public Set<String> deferredColumns() {
    return deferredColumns;
  }

  /**
   * How items are deactivated.
   *
   * @return deactivation, empty when the dataset has no status or active flag
   */
  public Optional<Deactivation> deactivation() {
    if (table.has(RECORD_STATUS) && columns.contains(RECORD_STATUS)) {
      return Optional.of(new Deactivation(RECORD_STATUS, "INACTIVE"));
    }
    if (table.has(ACTIVE) && "bool".equals(table.column(ACTIVE).type())) {
      return Optional.of(new Deactivation(ACTIVE, Boolean.FALSE));
    }
    return Optional.empty();
  }

  /**
   * Whether the table has a surrogate id column.
   *
   * @return true when rows have an id
   */
  public boolean hasId() {
    return table.has("id");
  }

  /**
   * Fingerprint of the promoted columns (names and types).
   *
   * @return fingerprint
   */
  public String fingerprint() {
    return table.fingerprint(columns);
  }

  /**
   * What does not fit between catalogue and schema.
   *
   * @return error messages, empty when the dataset is consistent
   */
  public List<String> errors() {
    return errors;
  }
}
