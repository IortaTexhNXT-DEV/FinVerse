package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkOutcome;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base of the uploads of the configuration screens: the template is the tab of the master data and
 * configuration workbook (same headers, same order), every row is validated first with business
 * messages naming the column, the preview tells whether a row adds or updates a record (natural
 * key, so the same file uploaded twice changes nothing), the rows are applied when a second user
 * approves the upload, and the current data downloads in the same layout.
 */
public abstract class ConfigUploadHandler implements BulkImportHandler {

  /** The database support of the uploads. */
  protected final UploadSupport db;

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  protected ConfigUploadHandler(UploadSupport db) {
    this.db = db;
  }

  /**
   * The tab of the master data and configuration workbook the template follows, e.g. "D0-02".
   *
   * @return template id
   */
  public abstract String templateId();

  /**
   * The configuration screen the upload belongs to, in words (menu path).
   *
   * @return screen
   */
  public abstract String screen();

  /**
   * Applies one valid row.
   *
   * @param row row
   * @param context run context
   * @return reference of the record added or updated
   */
  protected abstract String apply(BulkRow row, BulkContext context);

  @Override
  public BulkOutcome process(BulkRow row, BulkContext context) {
    return BulkOutcome.of(apply(row, context));
  }

  /**
   * The columns whose line breaks are kept (wordings, cells with one entry per line).
   *
   * @return headers
   */
  protected Set<String> multiline() {
    return Set.of();
  }

  @Override
  public String sanitize(String header, String value) {
    if (!multiline().contains(header)) {
      return BulkImportHandler.super.sanitize(header, value);
    }
    return value
        .replace("\r\n", "\n")
        .lines()
        .map(l -> l.trim().replaceAll("[ \\t]+", " "))
        .collect(Collectors.joining("\n"))
        .strip();
  }

  @Override
  public String uploadPath() {
    return screen() + " > Upload";
  }

  @Override
  public boolean exportable() {
    return true;
  }

  @Override
  public void afterCommit(BulkContext context, int committed, int failed) {
    if (committed > 0) {
      db.clearCaches();
    }
  }

  @Override
  public List<String> rules() {
    return List.of(
        "The layout is the tab " + templateId() + " of the master data and configuration workbook.",
        "A row whose code already exists updates that record; uploading the same file again"
            + " changes nothing.",
        "The rows are applied only after a second user approves the upload.");
  }

  // ---------- helpers of the handlers ------------------------------------------------------

  /**
   * An error message naming the column.
   *
   * @param header column
   * @param message message
   * @return text
   */
  protected static String error(String header, String message) {
    return header + ": " + message;
  }

  /**
   * A Y / N value as a boolean.
   *
   * @param row row
   * @param header column
   * @return true for Y
   */
  protected static boolean yes(BulkRow row, String header) {
    return row.yes(header);
  }

  /**
   * An optional number.
   *
   * @param row row
   * @param header column
   * @return number or null
   */
  protected static BigDecimal number(BulkRow row, String header) {
    return row.number(header);
  }

  /**
   * An optional date.
   *
   * @param row row
   * @param header column
   * @return date or null
   */
  protected static LocalDate date(BulkRow row, String header) {
    return row.date(header);
  }

  /**
   * An ordered map of column values (key or values of an upsert).
   *
   * @param pairs column, value, column, value...
   * @return map
   */
  protected static Map<String, Object> columns(Object... pairs) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      map.put((String) pairs[i], pairs[i + 1]);
    }
    return map;
  }

  /**
   * Checks a code against a list of allowed codes.
   *
   * @param errors errors
   * @param header column
   * @param value value, may be null
   * @param allowed allowed codes
   */
  protected static void oneOf(
      List<String> errors, String header, String value, List<String> allowed) {
    if (value != null && !allowed.contains(value)) {
      errors.add(
          error(header, value + " is not allowed; use one of " + String.join(", ", allowed)));
    }
  }

  /**
   * A row of an export: values by header, empty values left out.
   *
   * @param pairs header, value, header, value...
   * @return row
   */
  protected static Map<String, String> exportRow(Object... pairs) {
    Map<String, String> row = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      Object v = pairs[i + 1];
      if (v != null) {
        row.put((String) pairs[i], text(v));
      }
    }
    return row;
  }

  private static String text(Object v) {
    if (v instanceof Boolean b) {
      return b ? "Y" : "N";
    }
    if (v instanceof BigDecimal n) {
      return n.stripTrailingZeros().toPlainString();
    }
    if (v instanceof java.sql.Date d) {
      return d.toLocalDate().toString();
    }
    return v.toString();
  }

  /**
   * A code in words with a capital (for messages).
   *
   * @param code code
   * @return label
   */
  protected static String label(String code) {
    return DisplayFormat.label(code);
  }

  /**
   * A new list of errors.
   *
   * @return list
   */
  protected static List<String> errors() {
    return new ArrayList<>();
  }
}
