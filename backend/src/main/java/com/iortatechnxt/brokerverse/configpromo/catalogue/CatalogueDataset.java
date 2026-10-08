package com.iortatechnxt.brokerverse.configpromo.catalogue;

import java.util.List;
import java.util.Map;

/**
 * One configuration dataset of the catalogue (see {@code configpromo/catalogue.yml} for the meaning
 * of each attribute).
 *
 * @param code dataset code
 * @param name name in business words
 * @param group catalogue group
 * @param module owner module (top-level package)
 * @param table database table
 * @param key natural key columns
 * @param parent column of the parent row for a collection, null otherwise
 * @param refs references by id without a foreign key (column to dataset code)
 * @param exclude columns never promoted besides the audit columns
 * @param environment columns kept by the target on an existing row
 * @param environmentRows rows that belong to the environment, null when none
 * @param insertDefaults value of a column on a new row ("=column" copies another column)
 * @param userColumns columns naming a user
 * @param optional not selected by default
 * @param users user records, promoted only when users are included
 * @param usedBy references by code without a foreign key ("table.column")
 */
public record CatalogueDataset(
    String code,
    String name,
    String group,
    String module,
    String table,
    List<String> key,
    String parent,
    Map<String, String> refs,
    List<String> exclude,
    List<String> environment,
    EnvironmentRows environmentRows,
    Map<String, String> insertDefaults,
    List<String> userColumns,
    boolean optional,
    boolean users,
    List<String> usedBy) {

  /** Defensive copies; absent lists and maps are empty. */
  public CatalogueDataset {
    key = List.copyOf(key);
    refs = refs == null ? Map.of() : Map.copyOf(refs);
    exclude = exclude == null ? List.of() : List.copyOf(exclude);
    environment = environment == null ? List.of() : List.copyOf(environment);
    insertDefaults = insertDefaults == null ? Map.of() : Map.copyOf(insertDefaults);
    userColumns = userColumns == null ? List.of() : List.copyOf(userColumns);
    usedBy = usedBy == null ? List.of() : List.copyOf(usedBy);
  }

  /**
   * Whether the dataset is a collection of a parent row (lines of a rule, grants of a profile).
   *
   * @return true for a collection
   */
  public boolean collection() {
    return parent != null;
  }

  /**
   * Whether the dataset is selected when the user exports "all" datasets.
   *
   * @param includeUsers whether users are included
   * @return true when selected by default
   */
  public boolean selectedByDefault(boolean includeUsers) {
    return !optional && (!users || includeUsers);
  }
}
