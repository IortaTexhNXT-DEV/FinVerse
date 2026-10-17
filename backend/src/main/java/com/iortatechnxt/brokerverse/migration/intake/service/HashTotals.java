package com.iortatechnxt.brokerverse.migration.intake.service;

import com.iortatechnxt.brokerverse.bulk.service.ParsedFile;
import com.iortatechnxt.brokerverse.migration.intake.service.IntakeChecks.Failure;
import com.iortatechnxt.brokerverse.migration.mapping.domain.Layout;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The hash total of an extract (DATA_MIGRATION_DESIGN section 5.2): the count of distinct values of
 * the key columns, the row count, or the sum of the numeric part of a column, as the layout says.
 */
final class HashTotals {

  private HashTotals() {}

  /**
   * Compares the hash total of the control file with the file.
   *
   * @param control control file
   * @param layout layout in force
   * @param parsed data file
   * @return the difference, empty when equal
   */
  static Optional<Failure> check(ControlFile control, Layout layout, ParsedFile parsed) {
    if (control.hashTotal() == null || control.hashTotal().isBlank()) {
      return IntakeChecks.fail(IntakeChecks.HASH_TOTAL, "The control file has no hash total");
    }
    String computed = computeHash(control, layout, parsed);
    if (!normalise(control.hashTotal()).equals(computed)) {
      return IntakeChecks.fail(
          IntakeChecks.HASH_TOTAL,
          "The hash total of the key column is "
              + computed
              + "; the control file says "
              + control.hashTotal());
    }
    return Optional.empty();
  }

  /**
   * The hash total of a parsed file: the count of distinct values of the columns named in the
   * control file (or of the layout's hash columns), the row count, or the sum of the numeric part.
   *
   * @param control control file
   * @param layout layout
   * @param parsed file
   * @return hash total as text
   */
  static String computeHash(ControlFile control, Layout layout, ParsedFile parsed) {
    List<String> cols = new ArrayList<>();
    if (control.hashColumns() != null && !control.hashColumns().isBlank()) {
      for (String c : control.hashColumns().split("[+,]")) {
        cols.add(c.strip());
      }
    }
    Layout.HashRule rule = layout.getHashRule();
    if (!cols.isEmpty() && rule == Layout.HashRule.ROW_COUNT) {
      rule = Layout.HashRule.DISTINCT_COUNT;
    }
    List<String> keys = cols.isEmpty() ? layout.hashCols() : cols;
    return switch (rule) {
      case ROW_COUNT -> String.valueOf(parsed.rows().size());
      case NUMERIC_SUM -> numericSum(parsed, keys.get(0));
      default -> String.valueOf(distinct(parsed, keys));
    };
  }

  private static int distinct(ParsedFile parsed, List<String> keys) {
    Set<String> seen = new HashSet<>();
    for (ParsedFile.RawRow row : parsed.rows()) {
      seen.add(
          keys.stream()
              .map(k -> row.values().getOrDefault(k, ""))
              .collect(Collectors.joining("|")));
    }
    return seen.size();
  }

  private static String numericSum(ParsedFile parsed, String column) {
    BigDecimal sum = BigDecimal.ZERO;
    for (ParsedFile.RawRow row : parsed.rows()) {
      String digits = row.values().getOrDefault(column, "").replaceAll("\\D", "");
      if (!digits.isEmpty()) {
        sum = sum.add(new BigDecimal(digits));
      }
    }
    return sum.toPlainString();
  }

  private static String normalise(String value) {
    String v = value.strip();
    try {
      return new BigDecimal(v).stripTrailingZeros().toPlainString();
    } catch (NumberFormatException e) {
      return v;
    }
  }
}
