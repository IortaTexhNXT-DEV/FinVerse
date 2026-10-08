package com.iortatechnxt.brokerverse.configpromo.engine;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Reads datasets of one database in package form: references by id become the natural key of the
 * referenced row, environment rows are left out, rows come sorted by natural key. Keeps the keys of
 * the rows read, by id and by key text, for the references of other datasets; {@link #forget}
 * drops them after the rows of a dataset changed.
 */
public final class DatasetReader {

  private final JdbcTemplate jdbc;
  private final CatalogueModel model;
  private final Map<String, Map<Long, Map<String, Object>>> keysById = new HashMap<>();
  private final Map<String, Map<String, Long>> idsByKey = new HashMap<>();

  /**
   * Creates a reader.
   *
   * @param jdbc database
   * @param model catalogue of that database
   */
  public DatasetReader(JdbcTemplate jdbc, CatalogueModel model) {
    this.jdbc = jdbc;
    this.model = model;
  }

  /**
   * The catalogue model of the database.
   *
   * @return model
   */
  public CatalogueModel model() {
    return model;
  }

  /**
   * The rows of a dataset, sorted by natural key, without the environment rows.
   *
   * @param code dataset code
   * @return rows
   */
  public List<CanonicalRow> rows(String code) {
    DatasetModel m = model.model(code);
    List<String> select = new ArrayList<>(m.columns());
    if (m.hasId()) {
      select.add(0, "id");
    }
    String sql =
        "select " + select.stream().map(Sql::quote).collect(Collectors.joining(", "))
            + " from " + Sql.quote(m.table().name());
    List<CanonicalRow> rows = new ArrayList<>();
    jdbc.query(sql, rs -> {
      CanonicalRow row = row(rs, m);
      if (m.dataset().environmentRows() == null
          || !m.dataset().environmentRows().matches(row.get(m.dataset().environmentRows().column()))) {
        rows.add(row);
      }
    });
    rows.sort(Comparator.comparing(CanonicalRow::keyText));
    return rows;
  }

  private CanonicalRow row(ResultSet rs, DatasetModel m) throws SQLException {
    Map<String, Object> values = new LinkedHashMap<>();
    for (String column : m.columns()) {
      Object value = CanonicalValues.read(rs, column, m.table().column(column).type());
      Reference ref = m.references().get(column);
      values.put(column, ref == null || value == null ? value : keyOf(ref, value, m));
    }
    Long id = m.hasId() ? rs.getLong("id") : null;
    return CanonicalRow.of(id, values, m);
  }

  private Map<String, Object> keyOf(Reference ref, Object id, DatasetModel m) {
    Map<String, Object> key = keysById(ref.dataset()).get(Long.valueOf(id.toString()));
    if (key == null) {
      throw new PackageException(
          m.dataset().name()
              + ": column "
              + ref.column()
              + " refers to record "
              + id
              + " of "
              + model.model(ref.dataset()).dataset().name()
              + ", which does not exist");
    }
    return key;
  }

  /**
   * The natural keys of a dataset's rows by id.
   *
   * @param code dataset code
   * @return id to key
   */
  public Map<Long, Map<String, Object>> keysById(String code) {
    Map<Long, Map<String, Object>> cached = keysById.get(code);
    if (cached != null) {
      return cached;
    }
    DatasetModel m = model.model(code);
    if (!m.hasId()) {
      throw new IllegalStateException(code + " has no id; it cannot be referenced by id");
    }
    List<String> keyColumns = m.dataset().key();
    String sql =
        "select id, "
            + keyColumns.stream().map(Sql::quote).collect(Collectors.joining(", "))
            + " from "
            + Sql.quote(m.table().name());
    Map<Long, Map<String, Object>> keys = new HashMap<>();
    jdbc.query(
        sql,
        rs -> {
          Map<String, Object> key = new LinkedHashMap<>();
          for (String column : keyColumns) {
            Object value = CanonicalValues.read(rs, column, m.table().column(column).type());
            Reference ref = m.references().get(column);
            key.put(column, ref == null || value == null ? value : keyOf(ref, value, m));
          }
          keys.put(rs.getLong("id"), key);
        });
    keysById.put(code, keys);
    return keys;
  }

  /**
   * The id of the row of a dataset with a natural key.
   *
   * @param code dataset code
   * @param key natural key (as exported)
   * @return id, empty when the database has no such row
   */
  public Optional<Long> idOf(String code, Object key) {
    Map<String, Long> ids =
        idsByKey.computeIfAbsent(
            code,
            c -> {
              Map<String, Long> byKey = new HashMap<>();
              keysById(c).forEach((id, k) -> byKey.put(CanonicalJson.text(k), id));
              return byKey;
            });
    return Optional.ofNullable(ids.get(CanonicalJson.text(key)));
  }

  /**
   * Drops what is known about a dataset's rows after they changed.
   *
   * @param code dataset code
   */
  public void forget(String code) {
    keysById.remove(code);
    idsByKey.remove(code);
  }
}
