package com.iortatechnxt.brokerverse.finreport.report;

import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Row and cell helpers shared by the finance reports. */
public final class FinRows {

  /** Debit suffix. */
  public static final String DR = "Dr";

  /** Credit suffix. */
  public static final String CR = "Cr";

  private FinRows() {}

  /**
   * Dr / Cr suffix of a net (debit positive) balance (rule R-BAL).
   *
   * @param net net balance
   * @return "Dr" when zero or positive, else "Cr"
   */
  public static String drCr(BigDecimal net) {
    return net.signum() < 0 ? CR : DR;
  }

  /**
   * Builds an ordered cell map from key/value pairs; null values are skipped.
   *
   * @param keyValues alternating keys and values
   * @return cells
   */
  public static Map<String, Object> cells(Object... keyValues) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i + 1 < keyValues.length; i += 2) {
      if (keyValues[i + 1] != null) {
        m.put((String) keyValues[i], keyValues[i + 1]);
      }
    }
    return m;
  }

  /**
   * Non-detail row.
   *
   * @param kind kind
   * @param level level
   * @param label label
   * @param cells cells
   * @return row
   */
  public static ReportRow row(RowKind kind, int level, String label, Map<String, Object> cells) {
    return new ReportRow(kind, level, label, cells);
  }

  /**
   * Labelled row without values.
   *
   * @param kind kind
   * @param level level
   * @param label label
   * @return row
   */
  public static ReportRow label(RowKind kind, int level, String label) {
    return new ReportRow(kind, level, label, Map.of());
  }

  /**
   * Returns a copy of a result with extra rows and notes appended.
   *
   * @param result result
   * @param extraRows rows to append
   * @param extraNotes notes to append
   * @return extended result
   */
  public static ReportResult append(
      ReportResult result, List<ReportRow> extraRows, List<String> extraNotes) {
    List<ReportRow> rows = new ArrayList<>(result.rows());
    rows.addAll(extraRows);
    List<String> notes = new ArrayList<>(result.notes());
    notes.addAll(extraNotes);
    return new ReportResult(
        result.code(), result.title(), result.parameterEcho(), result.columns(), rows, notes);
  }

  /**
   * Null-safe text.
   *
   * @param value value
   * @return value or empty string
   */
  public static String text(String value) {
    return value == null ? "" : value;
  }
}
