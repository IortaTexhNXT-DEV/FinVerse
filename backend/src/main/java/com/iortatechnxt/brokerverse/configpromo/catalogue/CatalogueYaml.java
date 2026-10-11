package com.iortatechnxt.brokerverse.configpromo.catalogue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** Reads the catalogue file (plain YAML maps and lists, no custom types). */
final class CatalogueYaml {

  private static final String CODE = "code";
  private static final String NAME = "name";

  private CatalogueYaml() {}

  /**
   * Reads a catalogue.
   *
   * @param in YAML
   * @return catalogue
   */
  static ConfigCatalogue read(InputStream in) {
    Map<String, Object> root = map(new Yaml(new SafeConstructor(new LoaderOptions())).load(in));
    List<CatalogueGroup> groups = new ArrayList<>();
    for (Object g : list(root.get("groups"))) {
      Map<String, Object> m = map(g);
      groups.add(new CatalogueGroup(text(m, CODE), text(m, NAME)));
    }
    List<CatalogueDataset> datasets = new ArrayList<>();
    for (Object d : list(root.get("datasets"))) {
      datasets.add(dataset(map(d)));
    }
    Map<String, String> excluded = new LinkedHashMap<>();
    map(root.get("excluded"))
        .forEach(
            (reason, tables) -> {
              for (Object table : list(tables)) {
                if (excluded.put(table.toString(), reason) != null) {
                  throw new IllegalStateException("Table " + table + " is excluded twice");
                }
              }
            });
    Map<String, String> reasons = new LinkedHashMap<>();
    map(root.get("reasons")).forEach((k, v) -> reasons.put(k, v.toString()));
    return new ConfigCatalogue(groups, datasets, excluded, reasons);
  }

  private static CatalogueDataset dataset(Map<String, Object> m) {
    Map<String, Object> rows =
        m.containsKey("environmentRows") ? map(m.get("environmentRows")) : null;
    Map<String, Object> scope = m.containsKey("rows") ? map(m.get("rows")) : null;
    return new CatalogueDataset(
        text(m, CODE),
        text(m, NAME),
        text(m, "group"),
        text(m, "module"),
        text(m, "table"),
        strings(m.get("key")),
        (String) m.get("parent"),
        stringMap(m.get("refs")),
        strings(m.get("exclude")),
        strings(m.get("environment")),
        rows == null
            ? null
            : new EnvironmentRows(text(rows, "column"), strings(rows.get("values"))),
        scope == null ? null : new DatasetRows(text(scope, "column"), strings(scope.get("values"))),
        stringMap(m.get("insertDefaults")),
        strings(m.get("userColumns")),
        Boolean.TRUE.equals(m.get("optional")),
        Boolean.TRUE.equals(m.get("users")),
        strings(m.get("usedBy")));
  }

  private static String text(Map<String, Object> m, String key) {
    Object v = m.get(key);
    if (v == null) {
      throw new IllegalStateException("Catalogue entry without " + key + ": " + m);
    }
    return v.toString();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> map(Object value) {
    return value == null ? Map.of() : (Map<String, Object>) value;
  }

  private static List<?> list(Object value) {
    return value == null ? List.of() : (List<?>) value;
  }

  private static List<String> strings(Object value) {
    return list(value).stream().map(Object::toString).toList();
  }

  private static Map<String, String> stringMap(Object value) {
    Map<String, String> out = new LinkedHashMap<>();
    map(value).forEach((k, v) -> out.put(k, v.toString()));
    return out;
  }
}
