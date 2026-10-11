package com.iortatechnxt.brokerverse.migration.intake.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Writes staged rows in pages of 5,000 (DATA_MIGRATION_DESIGN section 5.2: the 5,000-row limit of
 * the bulk upload does not apply; the intake streams the file into staging page by page). Staging
 * is a table of this module, written directly.
 */
@Component
public class StagingWriter {

  /** Rows per insert page. */
  public static final int PAGE = 5000;

  private static final String INSERT =
      "insert into mig_stage_row (version, extract_id, object_code, layout_code, row_no, legacy_key,"
          + " raw_payload, row_hash, status) values (0, ?, ?, ?, ?, ?, cast(? as jsonb), ?, 'STAGED')";

  private final JdbcTemplate jdbc;
  private final ObjectMapper json;

  /**
   * Creates the writer.
   *
   * @param jdbc JDBC
   * @param json JSON mapper
   */
  public StagingWriter(JdbcTemplate jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  /**
   * The SHA-256 of a row's values (sorted by column, so the order of the columns does not count).
   *
   * @param values row values
   * @return hex digest
   */
  public String rowHash(Map<String, String> values) {
    return Sha256.hex(toJson(new TreeMap<>(values)).getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Inserts rows.
   *
   * @param target extract, object and layout
   * @param rows rows to stage
   * @return rows written
   */
  public int write(Target target, List<Row> rows) {
    int written = 0;
    for (int from = 0; from < rows.size(); from += PAGE) {
      List<Object[]> args = new ArrayList<>();
      for (Row row : rows.subList(from, Math.min(rows.size(), from + PAGE))) {
        args.add(
            new Object[] {
              target.extractId(),
              target.objectCode(),
              target.layoutCode(),
              row.rowNo(),
              row.legacyKey(),
              toJson(new TreeMap<>(row.values())),
              row.hash()
            });
      }
      written += jdbc.batchUpdate(INSERT, args).length;
    }
    return written;
  }

  private String toJson(Map<String, String> values) {
    try {
      return json.writeValueAsString(values);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("A staged row cannot be written", e);
    }
  }

  /**
   * Where rows are staged.
   *
   * @param extractId extract
   * @param objectCode object
   * @param layoutCode layout
   */
  public record Target(Long extractId, String objectCode, String layoutCode) {}

  /**
   * A row to stage.
   *
   * @param rowNo row number in the file
   * @param legacyKey legacy key
   * @param values values (masked outside production)
   * @param hash row hash
   */
  public record Row(int rowNo, String legacyKey, Map<String, String> values, String hash) {}
}
