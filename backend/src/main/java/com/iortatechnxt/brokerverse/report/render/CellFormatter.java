package com.iortatechnxt.brokerverse.report.render;

import com.iortatechnxt.brokerverse.report.core.ColumnType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formats cell values for text based outputs (PDF, CSV). */
public final class CellFormatter {

  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
  private static final String AMOUNT_PATTERN = "#,##0.00;(#,##0.00)";
  private static final String NUMBER_PATTERN = "#,##0.##";

  private CellFormatter() {}

  /**
   * Formats a value for display.
   *
   * @param value cell value
   * @param type column type
   * @return text (empty for null)
   */
  public static String format(Object value, ColumnType type) {
    if (value == null) {
      return "";
    }
    return switch (type) {
      case AMOUNT ->
          decimal(AMOUNT_PATTERN).format(toDecimal(value).setScale(2, RoundingMode.HALF_EVEN));
      case NUMBER -> decimal(NUMBER_PATTERN).format(toDecimal(value));
      case PERCENT -> decimal("#,##0.00").format(toDecimal(value)) + "%";
      case DATE -> value instanceof LocalDate d ? d.format(DATE) : value.toString();
      case TEXT -> value.toString();
    };
  }

  /**
   * Converts a numeric cell to BigDecimal.
   *
   * @param value numeric value
   * @return decimal
   */
  public static BigDecimal toDecimal(Object value) {
    if (value instanceof BigDecimal bd) {
      return bd;
    }
    return new BigDecimal(value.toString());
  }

  private static DecimalFormat decimal(String pattern) {
    return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
  }
}
