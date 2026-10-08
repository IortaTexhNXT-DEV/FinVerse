package com.iortatechnxt.brokerverse.configpromo.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/** Reads the tables, columns and foreign keys of the current schema of a database. */
public final class SchemaReader {

  private static final String COLUMNS =
      """
      select table_name, column_name, udt_name, is_nullable,
             column_default is not null or is_identity = 'YES' as has_default
      from information_schema.columns
      where table_schema = current_schema()
        and table_name in (select table_name from information_schema.tables
                           where table_schema = current_schema() and table_type = 'BASE TABLE')
      order by table_name, ordinal_position
      """;

  private static final String FOREIGN_KEYS =
      """
      select src.relname as table_name, tgt.relname as target_table,
             (select string_agg(a.attname, ',' order by k.ord)
                from unnest(con.conkey) with ordinality k(attnum, ord)
                join pg_attribute a on a.attrelid = con.conrelid and a.attnum = k.attnum) as cols,
             (select string_agg(a.attname, ',' order by k.ord)
                from unnest(con.confkey) with ordinality k(attnum, ord)
                join pg_attribute a on a.attrelid = con.confrelid and a.attnum = k.attnum) as target_cols
      from pg_constraint con
      join pg_class src on src.oid = con.conrelid
      join pg_class tgt on tgt.oid = con.confrelid
      join pg_namespace n on n.oid = src.relnamespace
      where con.contype = 'f' and n.nspname = current_schema()
      order by src.relname, con.conname
      """;

  private SchemaReader() {}

  /**
   * Reads every base table of the current schema.
   *
   * @param jdbc database
   * @return tables by name
   */
  public static Map<String, TableSchema> read(JdbcTemplate jdbc) {
    Map<String, Map<String, ColumnInfo>> columns = new LinkedHashMap<>();
    jdbc.query(
        COLUMNS,
        rs -> {
          columns
              .computeIfAbsent(rs.getString(1), t -> new LinkedHashMap<>())
              .put(
                  rs.getString(2),
                  new ColumnInfo(
                      rs.getString(2),
                      rs.getString(3),
                      "YES".equals(rs.getString(4)),
                      rs.getBoolean(5)));
        });
    Map<String, List<ForeignKey>> keys = new HashMap<>();
    jdbc.query(
        FOREIGN_KEYS,
        rs -> {
          keys.computeIfAbsent(rs.getString(1), t -> new ArrayList<>())
              .add(
                  new ForeignKey(
                      Arrays.asList(rs.getString(3).split(",")),
                      rs.getString(2),
                      Arrays.asList(rs.getString(4).split(","))));
        });
    Map<String, TableSchema> tables = new LinkedHashMap<>();
    columns.forEach(
        (table, cols) ->
            tables.put(table, new TableSchema(table, cols, keys.getOrDefault(table, List.of()))));
    return tables;
  }
}
