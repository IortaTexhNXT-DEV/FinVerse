package com.iortatechnxt.brokerverse.configpromo.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Counts the records of the target that use an item (transactions and other records that are not
 * configuration): the foreign keys of the database and the references by code the catalogue
 * declares ({@code usedBy}). An item in use is never deactivated by an import.
 */
public final class UsageChecker {

  private final JdbcTemplate jdbc;
  private final DatasetReader reader;
  private final CatalogueModel model;
  private final Map<String, List<Usage>> usages = new HashMap<>();

  private record Usage(String table, List<String> columns, List<String> targetColumns) {}

  /**
   * Creates the checker.
   *
   * @param jdbc target database
   * @param reader reader of the target
   */
  public UsageChecker(JdbcTemplate jdbc, DatasetReader reader) {
    this.jdbc = jdbc;
    this.reader = reader;
    this.model = reader.model();
    for (TableSchema t : model.tables().values()) {
      if (model.catalogue().byTable(t.name()).isPresent()) {
        continue;
      }
      for (ForeignKey fk : t.foreignKeys()) {
        usages
            .computeIfAbsent(fk.table(), k -> new ArrayList<>())
            .add(new Usage(t.name(), fk.columns(), fk.targetColumns()));
      }
    }
  }

  /**
   * How many records of the target use an item.
   *
   * @param m dataset of the item
   * @param row the item as read from the target (with its id)
   * @return number of records
   */
  public long count(DatasetModel m, RowChange row) {
    long total = 0;
    for (Usage u : usages.getOrDefault(m.table().name(), List.of())) {
      total += count(u, m, row);
    }
    List<String> key = m.dataset().key();
    Object code = row.values().get(key.get(key.size() - 1));
    for (String soft : m.dataset().usedBy()) {
      String[] parts = soft.split("\\.");
      if (code != null && model.tables().containsKey(parts[0])) {
        Long n =
            jdbc.queryForObject(
                "select count(*) from " + Sql.quote(parts[0]) + " where " + Sql.quote(parts[1])
                    + " = ?",
                Long.class,
                code.toString());
        total += n == null ? 0 : n;
      }
    }
    return total;
  }

  private long count(Usage u, DatasetModel m, RowChange row) {
    List<Object> params = new ArrayList<>();
    List<String> conditions = new ArrayList<>();
    for (int i = 0; i < u.columns().size(); i++) {
      String target = u.targetColumns().get(i);
      Object value = "id".equals(target) ? row.targetId() : raw(m, target, row);
      if (value == null) {
        return 0;
      }
      TableSchema source = model.tables().get(u.table());
      String type = source.column(u.columns().get(i)).type();
      conditions.add(Sql.quote(u.columns().get(i)) + " = " + CanonicalValues.placeholder(type));
      params.add(CanonicalValues.parameter(value, type));
    }
    Long n =
        jdbc.queryForObject(
            "select count(*) from " + Sql.quote(u.table()) + " where "
                + String.join(" and ", conditions),
            Long.class,
            params.toArray());
    return n == null ? 0 : n;
  }

  /** The database value of a column: a reference is turned back into the id of the target. */
  private Object raw(DatasetModel m, String column, RowChange row) {
    Object value = row.values().get(column);
    Reference ref = m.references().get(column);
    if (ref == null || value == null) {
      return value;
    }
    return reader.idOf(ref.dataset(), value).orElse(null);
  }
}
