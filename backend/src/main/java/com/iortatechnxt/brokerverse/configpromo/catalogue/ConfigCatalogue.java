package com.iortatechnxt.brokerverse.configpromo.catalogue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The configuration catalogue: every configuration dataset of the platform and every table that is
 * never promoted, with its reason. Read once from {@code configpromo/catalogue.yml}.
 */
public final class ConfigCatalogue {

  /** Classpath location of the catalogue. */
  public static final String RESOURCE = "configpromo/catalogue.yml";

  private final List<CatalogueGroup> groups;
  private final Map<String, CatalogueDataset> byCode;
  private final Map<String, CatalogueDataset> byTable;
  private final Map<String, String> excluded;
  private final Map<String, String> reasons;

  ConfigCatalogue(
      List<CatalogueGroup> groups,
      List<CatalogueDataset> datasets,
      Map<String, String> excluded,
      Map<String, String> reasons) {
    this.groups = List.copyOf(groups);
    this.byCode = index(datasets, CatalogueDataset::code, "dataset code");
    this.byTable = index(datasets, CatalogueDataset::table, "table");
    this.excluded = Collections.unmodifiableMap(new LinkedHashMap<>(excluded));
    this.reasons = Collections.unmodifiableMap(new LinkedHashMap<>(reasons));
    validate();
  }

  /**
   * Reads the catalogue of the application.
   *
   * @return catalogue
   */
  public static ConfigCatalogue load() {
    try (InputStream in =
        ConfigCatalogue.class.getClassLoader().getResourceAsStream(RESOURCE)) {
      if (in == null) {
        throw new IllegalStateException("The configuration catalogue " + RESOURCE + " is missing");
      }
      return CatalogueYaml.read(in);
    } catch (IOException e) {
      throw new IllegalStateException("The configuration catalogue cannot be read", e);
    }
  }

  private static Map<String, CatalogueDataset> index(
      List<CatalogueDataset> datasets, Function<CatalogueDataset, String> key, String what) {
    Map<String, CatalogueDataset> map = new LinkedHashMap<>();
    for (CatalogueDataset d : datasets) {
      if (map.put(key.apply(d), d) != null) {
        throw new IllegalStateException("Duplicate " + what + " in the catalogue: " + key.apply(d));
      }
    }
    return Collections.unmodifiableMap(map);
  }

  private void validate() {
    Map<String, CatalogueGroup> groupCodes =
        groups.stream().collect(Collectors.toMap(CatalogueGroup::code, g -> g));
    for (CatalogueDataset d : byCode.values()) {
      if (!groupCodes.containsKey(d.group())) {
        throw new IllegalStateException(d.code() + " has the unknown group " + d.group());
      }
      if (d.key().isEmpty()) {
        throw new IllegalStateException(d.code() + " has no natural key");
      }
      if (excluded.containsKey(d.table())) {
        throw new IllegalStateException(d.table() + " is both a dataset and excluded");
      }
      for (String target : d.refs().values()) {
        if (!byCode.containsKey(target)) {
          throw new IllegalStateException(d.code() + " refers to the unknown dataset " + target);
        }
      }
    }
    for (String reason : excluded.values()) {
      if (!reasons.containsKey(reason)) {
        throw new IllegalStateException("Unknown reason " + reason + " of an excluded table");
      }
    }
  }

  /**
   * Groups in screen order.
   *
   * @return groups
   */
  public List<CatalogueGroup> groups() {
    return groups;
  }

  /**
   * Datasets in catalogue order.
   *
   * @return datasets
   */
  public List<CatalogueDataset> datasets() {
    return List.copyOf(byCode.values());
  }

  /**
   * A dataset by code.
   *
   * @param code dataset code
   * @return dataset, empty when unknown
   */
  public Optional<CatalogueDataset> find(String code) {
    return Optional.ofNullable(byCode.get(code));
  }

  /**
   * A dataset by code that must exist.
   *
   * @param code dataset code
   * @return dataset
   */
  public CatalogueDataset dataset(String code) {
    CatalogueDataset d = byCode.get(code);
    if (d == null) {
      throw new IllegalArgumentException("Unknown configuration dataset " + code);
    }
    return d;
  }

  /**
   * The dataset of a table.
   *
   * @param table table
   * @return dataset, empty when the table is not configuration
   */
  public Optional<CatalogueDataset> byTable(String table) {
    return Optional.ofNullable(byTable.get(table));
  }

  /**
   * Tables never promoted, with their reason code.
   *
   * @return table to reason code
   */
  public Map<String, String> excludedTables() {
    return excluded;
  }

  /**
   * Reasons of the excluded tables in words.
   *
   * @return reason code to text
   */
  public Map<String, String> reasons() {
    return reasons;
  }

  /**
   * The group of a dataset.
   *
   * @param code group code
   * @return group
   */
  public CatalogueGroup group(String code) {
    return groups.stream()
        .filter(g -> g.code().equals(code))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown catalogue group " + code));
  }
}
