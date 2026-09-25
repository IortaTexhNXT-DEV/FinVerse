package com.iortatechnxt.brokerverse.screening.config.service;

import com.iortatechnxt.brokerverse.screening.config.domain.StrFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The STR extraction layout of one configuration version (SNSRP-105, 706; FR-SS-017). The AMLC
 * format is parked (SQ09): the delivered layout is a placeholder with the case fields.
 *
 * @param version the STR_LAYOUT configuration version
 * @param format the file format
 * @param delimiter the delimiter for CSV, may be {@code null}
 * @param encoding the character encoding (e.g. UTF-8)
 * @param columns the columns, sorted by order
 */
public record StrLayout(
    ConfigVersionRef version,
    StrFormat format,
    String delimiter,
    String encoding,
    List<Column> columns) {

  /** Sorted copy. */
  public StrLayout {
    columns = columns.stream().sorted(Comparator.comparingInt(Column::order)).toList();
  }

  /**
   * One file column: an STR field or a fixed value (FR-SS-017 R2).
   *
   * @param order the position in the file
   * @param fieldCode the STR template field code, {@code null} when a fixed value is used
   * @param fixedValue the fixed value, {@code null} when a field is used
   * @param header the column header, may be {@code null}
   * @param length the width for FIXED, otherwise may be {@code null}
   * @param pad LEFT or RIGHT for FIXED, may be {@code null}
   * @param codeMap BIBS code to AMLC code
   */
  public record Column(
      int order,
      String fieldCode,
      String fixedValue,
      String header,
      Integer length,
      String pad,
      Map<String, String> codeMap) {

    /** Defensive copy. */
    public Column {
      codeMap = Map.copyOf(codeMap);
    }
  }
}
