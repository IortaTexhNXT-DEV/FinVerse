package com.iortatechnxt.brokerverse.configpromo.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Writes the rows of one dataset: inserts and updates with the references remapped to the ids of
 * the target, the audit columns of the import (maker = the user who prepared it, checker = the user
 * who approved it), deactivations and the removal of collection items.
 */
final class RowWriter {

  private static final String RECORD_STATUS = "record_status";
  private static final String AUTHORIZED_BY = "authorized_by";
  private static final String ACTIVE = "ACTIVE";

  private final JdbcTemplate jdbc;
  private final IdResolver ids;
  private final CatalogueModel model;
  private final DatasetModel m;
  private final ApplyActors actors;

  RowWriter(
      JdbcTemplate jdbc, IdResolver ids, CatalogueModel model, DatasetModel m, ApplyActors actors) {
    this.jdbc = jdbc;
    this.ids = ids;
    this.model = model;
    this.m = m;
    this.actors = actors;
  }

  /**
   * Inserts an item; its references to rows of the same dataset are written later ({@link
   * #writeDeferred}).
   */
  void insert(RowChange row) {
    Map<String, Object> values = new LinkedHashMap<>();
    for (String column : m.columns()) {
      values.put(column, m.deferredColumns().contains(column) ? null : value(row, column));
    }
    m.dataset()
        .insertDefaults()
        .forEach((column, spec) -> values.put(column, defaultValue(row, spec)));
    Map<String, Object> audit = new LinkedHashMap<>();
    putIfPresent(audit, "version", "0");
    putIfPresent(audit, "created_at", timestamp());
    putIfPresent(audit, "created_by", actors.maker());
    authorization(audit, row);
    values.putAll(audit);
    List<String> columns = new ArrayList<>(values.keySet());
    String sql =
        "insert into "
            + Sql.quote(m.table().name())
            + " ("
            + columns.stream().map(Sql::quote).collect(Collectors.joining(", "))
            + ") values ("
            + columns.stream().map(this::placeholder).collect(Collectors.joining(", "))
            + ")";
    execute(row, sql, parameters(columns, values));
  }

  /** Updates the compared columns of an item. */
  void update(RowChange row) {
    Map<String, Object> values = new LinkedHashMap<>();
    for (String column : m.comparedColumns()) {
      if (!m.deferredColumns().contains(column)) {
        values.put(column, value(row, column));
      }
    }
    putIfPresent(values, "updated_at", timestamp());
    putIfPresent(values, "updated_by", actors.maker());
    authorization(values, row);
    setAndWhere(row, values);
  }

  /** Writes the references of an item to rows of the same dataset. */
  void writeDeferred(RowChange row) {
    if (m.deferredColumns().isEmpty()) {
      return;
    }
    Map<String, Object> values = new LinkedHashMap<>();
    for (String column : m.deferredColumns()) {
      values.put(column, value(row, column));
    }
    setAndWhere(row, values);
  }

  /** Deactivates an item of the target (never deleted). */
  void deactivate(RowChange row, Deactivation deactivation) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put(deactivation.column(), deactivation.inactive());
    putIfPresent(values, "updated_at", timestamp());
    putIfPresent(values, "updated_by", actors.maker());
    setAndWhere(row, values);
  }

  private void setAndWhere(RowChange row, Map<String, Object> values) {
    List<String> columns = new ArrayList<>(values.keySet());
    String set =
        columns.stream()
            .map(c -> Sql.quote(c) + " = " + placeholder(c))
            .collect(Collectors.joining(", "));
    if (m.table().has("version")) {
      set += ", \"version\" = \"version\" + 1";
    }
    List<Object> params = parameters(columns, values);
    String where = where(row, params);
    execute(row, "update " + Sql.quote(m.table().name()) + " set " + set + where, params);
  }

  private String where(RowChange row, List<Object> params) {
    Long id = row.targetId() != null ? row.targetId() : idOfKey(row);
    if (id != null) {
      params.add(id);
      return " where \"id\" = ?";
    }
    List<String> keys = m.dataset().key();
    for (String k : keys) {
      params.add(CanonicalValues.parameter(value(row, k), m.table().column(k).type()));
    }
    return " where "
        + keys.stream()
            .map(k -> Sql.quote(k) + " = " + placeholder(k))
            .collect(Collectors.joining(" and "));
  }

  private Long idOfKey(RowChange row) {
    return m.hasId() ? ids.idOf(m.code(), row.key()).orElse(null) : null;
  }

  /** The value of a column of an item, with a reference remapped to the id in the target. */
  Object value(RowChange row, String column) {
    Object value = row.values().get(column);
    Reference ref = m.references().get(column);
    if (ref == null || value == null) {
      return value;
    }
    return ids.idOf(ref.dataset(), value)
        .map(String::valueOf)
        .orElseThrow(
            () ->
                new ApplyException(
                    m.dataset().name()
                        + " "
                        + row.keyText()
                        + ": the referenced record "
                        + CanonicalJson.text(value)
                        + " of "
                        + model.model(ref.dataset()).dataset().name()
                        + " exists neither in the package nor in this environment"));
  }

  private Object defaultValue(RowChange row, String spec) {
    return spec.startsWith("=") ? value(row, spec.substring(1)) : spec;
  }

  private void authorization(Map<String, Object> values, RowChange row) {
    if (m.table().has(AUTHORIZED_BY) && ACTIVE.equals(row.values().get(RECORD_STATUS))) {
      values.put(AUTHORIZED_BY, actors.checker());
      putIfPresent(values, "authorized_at", timestamp());
    }
  }

  private void putIfPresent(Map<String, Object> values, String column, Object value) {
    if (m.table().has(column)) {
      values.put(column, value);
    }
  }

  private String timestamp() {
    return actors.at().toString();
  }

  private String placeholder(String column) {
    return CanonicalValues.placeholder(m.table().column(column).type());
  }

  private List<Object> parameters(List<String> columns, Map<String, Object> values) {
    List<Object> params = new ArrayList<>();
    for (String c : columns) {
      params.add(CanonicalValues.parameter(values.get(c), m.table().column(c).type()));
    }
    return params;
  }

  private void execute(RowChange row, String sql, List<Object> params) {
    try {
      jdbc.update(sql, params.toArray());
    } catch (DataAccessException e) {
      Throwable cause = NestedExceptionUtils.getMostSpecificCause(e);
      throw new ApplyException(
          m.dataset().name() + " " + row.keyText() + " could not be written: " + cause.getMessage(),
          e);
    }
  }

  /**
   * Removes an item of a replaced collection, with the items of its own collections.
   *
   * @param row item only in the target
   */
  void delete(RowChange row) {
    if (row.targetId() != null) {
      deleteChildren(m, row.targetId());
    }
    List<Object> params = new ArrayList<>();
    String where = where(row, params);
    execute(row, "delete from " + Sql.quote(m.table().name()) + where, params);
  }

  private void deleteChildren(DatasetModel parent, long parentId) {
    for (String childCode : model.collectionsOf(parent.code())) {
      DatasetModel child = model.model(childCode);
      String table = Sql.quote(child.table().name());
      String parentColumn = Sql.quote(child.dataset().parent());
      if (child.hasId()) {
        List<Long> children =
            jdbc.queryForList(
                "select \"id\" from " + table + " where " + parentColumn + " = ?",
                Long.class,
                parentId);
        children.forEach(id -> deleteChildren(child, id));
      }
      jdbc.update("delete from " + table + " where " + parentColumn + " = ?", parentId);
    }
  }
}
