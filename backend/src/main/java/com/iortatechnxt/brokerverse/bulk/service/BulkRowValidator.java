package com.iortatechnxt.brokerverse.bulk.service;

import com.iortatechnxt.brokerverse.bulk.service.ParsedFile.RawRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Framework checks of one row: sanitisation, mandatory values and value types. */
final class BulkRowValidator {

  private static final Pattern YES = Pattern.compile("\\s*(y|yes)\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern NO = Pattern.compile("\\s*(n|no)\\s*", Pattern.CASE_INSENSITIVE);

  private BulkRowValidator() {}

  /**
   * Sanitises the raw values of a row with the handler's rules.
   *
   * @param handler handler
   * @param raw raw row
   * @return clean row (blank values removed), in template column order
   */
  static BulkRow sanitize(BulkImportHandler handler, RawRow raw) {
    Map<String, String> clean = new LinkedHashMap<>();
    for (BulkColumn col : handler.columns()) {
      String v = raw.values().get(col.header());
      if (v != null) {
        String s = handler.sanitize(col.header(), v);
        if (!s.isBlank()) {
          clean.put(col.header(), normalize(col.type(), s));
        }
      }
    }
    return new BulkRow(raw.rowNo(), clean);
  }

  private static String normalize(BulkColumn.Type type, String value) {
    return switch (type) {
      case YES_NO -> yesNo(value);
      case NUMBER -> withoutGrouping(value);
      default -> value;
    };
  }

  private static String yesNo(String value) {
    if (YES.matcher(value).matches()) {
      return "Y";
    }
    return NO.matcher(value).matches() ? "N" : value;
  }

  private static String withoutGrouping(String value) {
    return value.replace(",", "");
  }

  /**
   * Checks mandatory values and types.
   *
   * @param handler handler
   * @param row sanitised row
   * @return errors
   */
  static List<String> check(BulkImportHandler handler, BulkRow row) {
    List<String> errors = new ArrayList<>();
    for (BulkColumn col : handler.columns()) {
      String v = row.values().get(col.header());
      if (v == null) {
        if (col.required()) {
          errors.add(col.header() + " is mandatory");
        }
      } else if (!typeOk(col.type(), v)) {
        errors.add(col.header() + " '" + v + "' is not a valid " + typeName(col.type()));
      }
    }
    return errors;
  }

  private static boolean typeOk(BulkColumn.Type type, String value) {
    try {
      switch (type) {
        case NUMBER -> new BigDecimal(value);
        case DATE -> LocalDate.parse(value);
        case YES_NO -> {
          return "Y".equals(value) || "N".equals(value);
        }
        default -> {
          return true;
        }
      }
      return true;
    } catch (NumberFormatException | DateTimeParseException e) {
      return false;
    }
  }

  private static String typeName(BulkColumn.Type type) {
    return switch (type) {
      case NUMBER -> "number";
      case DATE -> "date (yyyy-mm-dd)";
      case YES_NO -> "Y/N value";
      case TEXT -> "text";
    };
  }
}
