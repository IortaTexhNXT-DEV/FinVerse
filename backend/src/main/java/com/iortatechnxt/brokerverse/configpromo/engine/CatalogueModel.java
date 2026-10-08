package com.iortatechnxt.brokerverse.configpromo.engine;

import com.iortatechnxt.brokerverse.configpromo.catalogue.CatalogueDataset;
import com.iortatechnxt.brokerverse.configpromo.catalogue.ConfigCatalogue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The catalogue resolved against the schema of one database: a {@link DatasetModel} per dataset and
 * the load order (a dataset after every dataset it references).
 */
public final class CatalogueModel {

  private final ConfigCatalogue catalogue;
  private final Map<String, TableSchema> tables;
  private final Map<String, DatasetModel> models;
  private final List<String> order;
  private final List<String> errors;

  /**
   * Resolves the catalogue.
   *
   * @param catalogue catalogue
   * @param tables tables of the database
   */
  public CatalogueModel(ConfigCatalogue catalogue, Map<String, TableSchema> tables) {
    this.catalogue = catalogue;
    this.tables = Map.copyOf(tables);
    List<String> problems = new ArrayList<>();
    Map<String, DatasetModel> resolved = new LinkedHashMap<>();
    for (CatalogueDataset d : catalogue.datasets()) {
      TableSchema table = tables.get(d.table());
      if (table == null) {
        problems.add(d.code() + ": table " + d.table() + " does not exist");
      } else {
        DatasetModel model = DatasetModel.of(d, table, catalogue);
        problems.addAll(model.errors());
        resolved.put(d.code(), model);
      }
    }
    this.models = Collections.unmodifiableMap(resolved);
    this.order = sort(resolved, problems);
    this.errors = List.copyOf(problems);
  }

  private static List<String> sort(Map<String, DatasetModel> models, List<String> problems) {
    Map<String, Integer> pending = new HashMap<>();
    Map<String, List<String>> dependents = new HashMap<>();
    for (DatasetModel m : models.values()) {
      Set<String> deps = new HashSet<>(m.dependencies());
      deps.retainAll(models.keySet());
      pending.put(m.code(), deps.size());
      deps.forEach(dep -> dependents.computeIfAbsent(dep, k -> new ArrayList<>()).add(m.code()));
    }
    Deque<String> ready = new ArrayDeque<>();
    models.keySet().stream().filter(c -> pending.get(c) == 0).forEach(ready::add);
    List<String> sorted = new ArrayList<>();
    while (!ready.isEmpty()) {
      String code = ready.removeFirst();
      sorted.add(code);
      for (String next : dependents.getOrDefault(code, List.of())) {
        if (pending.merge(next, -1, Integer::sum) == 0) {
          ready.addLast(next);
        }
      }
    }
    if (sorted.size() < models.size()) {
      List<String> cyclic =
          models.keySet().stream().filter(c -> !sorted.contains(c)).toList();
      problems.add("The datasets " + cyclic + " depend on each other in a cycle");
    }
    return List.copyOf(sorted);
  }

  /**
   * The catalogue.
   *
   * @return catalogue
   */
  public ConfigCatalogue catalogue() {
    return catalogue;
  }

  /**
   * The model of a dataset.
   *
   * @param code dataset code
   * @return model
   */
  public DatasetModel model(String code) {
    DatasetModel model = models.get(code);
    if (model == null) {
      throw new IllegalArgumentException("Dataset " + code + " is not in this database");
    }
    return model;
  }

  /**
   * Whether the database has the dataset.
   *
   * @param code dataset code
   * @return true when present
   */
  public boolean has(String code) {
    return models.containsKey(code);
  }

  /**
   * Every dataset in load order.
   *
   * @return dataset codes
   */
  public List<String> loadOrder() {
    return order;
  }

  /**
   * Some datasets in load order.
   *
   * @param codes dataset codes
   * @return the same codes in load order
   */
  public List<String> loadOrder(Collection<String> codes) {
    Set<String> wanted = new HashSet<>(codes);
    return order.stream().filter(wanted::contains).toList();
  }

  /**
   * The datasets whose parent is the given dataset (collections replaced with their parent).
   *
   * @param code parent dataset code
   * @return child dataset codes
   */
  public List<String> collectionsOf(String code) {
    return models.values().stream()
        .filter(m -> m.dataset().collection())
        .filter(m -> m.references().containsKey(m.dataset().parent()))
        .filter(m -> code.equals(m.references().get(m.dataset().parent()).dataset()))
        .map(DatasetModel::code)
        .toList();
  }

  /**
   * The tables of the database.
   *
   * @return tables by name
   */
  public Map<String, TableSchema> tables() {
    return tables;
  }

  /**
   * What does not fit between catalogue and schema; empty when consistent.
   *
   * @return messages
   */
  public List<String> errors() {
    return errors;
  }
}
