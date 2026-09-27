package com.iortatechnxt.brokerverse.migration.intake.service;

import com.iortatechnxt.brokerverse.bulk.service.ParsedFile;
import com.iortatechnxt.brokerverse.migration.mapping.domain.Layout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The intake checks of an extract against its control file and its layout (DATA_MIGRATION_DESIGN
 * section 5.2; FR-DM-010): SHA-256, header, row count, amount totals per column and currency, and
 * the hash total of the key. The first failing check rejects the extract; nothing is staged.
 */
final class IntakeChecks {

  /** Checksum differs. */
  static final String CHECKSUM = "MIG_CHECKSUM";

  /** Header differs from the layout. */
  static final String LAYOUT = "MIG_LAYOUT";

  /** Row count differs. */
  static final String ROW_COUNT = "MIG_ROW_COUNT";

  /** Amount total differs. */
  static final String CONTROL_TOTAL = "MIG_CONTROL_TOTAL";

  /** Hash total differs. */
  static final String HASH_TOTAL = "MIG_HASH_TOTAL";

  private static final String CURRENCY = "currency";
  private static final int SCALE = 2;

  private IntakeChecks() {}

  /**
   * Runs the file checks.
   *
   * @param sha256 computed SHA-256 of the data file
   * @param control control file
   * @param layout layout in force
   * @param columns column names of the layout
   * @param parsed parsed data file
   * @return the first failure, empty when every check passes
   */
  static Optional<Failure> run(
      String sha256, ControlFile control, Layout layout, List<String> columns, ParsedFile parsed) {
    if (control.sha256() == null || !control.sha256().equalsIgnoreCase(sha256)) {
      return fail(CHECKSUM, "The file does not match the checksum in the control file");
    }
    Optional<Failure> header = header(layout, columns, parsed.headers());
    if (header.isPresent()) {
      return header;
    }
    int rows = parsed.rows().size();
    if (control.rowCount() == null || control.rowCount() != rows) {
      return fail(
          ROW_COUNT, "The file has " + rows + " rows; the control file says " + control.rowCount());
    }
    Optional<Failure> amounts = amounts(control, parsed);
    return amounts.isPresent() ? amounts : hash(control, layout, parsed);
  }

  private static Optional<Failure> header(
      Layout layout, List<String> columns, List<String> headers) {
    Set<String> expected = new LinkedHashSet<>(columns);
    Set<String> given =
        headers.stream()
            .filter(h -> !h.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    Set<String> missing = new LinkedHashSet<>(expected);
    missing.removeAll(given);
    Set<String> extra = new LinkedHashSet<>(given);
    extra.removeAll(expected);
    if (missing.isEmpty() && extra.isEmpty()) {
      return Optional.empty();
    }
    return fail(
        LAYOUT,
        "The file columns do not match layout "
            + layout.getCode()
            + " version "
            + layout.getVersionNo()
            + ": missing "
            + (missing.isEmpty() ? "none" : String.join(", ", missing))
            + ", extra "
            + (extra.isEmpty() ? "none" : String.join(", ", extra)));
  }

  private static Optional<Failure> amounts(ControlFile control, ParsedFile parsed) {
    boolean hasCurrency = parsed.headers().contains(CURRENCY);
    for (ControlFile.AmountTotal total : control.amounts()) {
      BigDecimal sum = BigDecimal.ZERO;
      for (ParsedFile.RawRow row : parsed.rows()) {
        if (applies(total, row.values(), hasCurrency)) {
          sum = sum.add(amount(row.values().get(total.column())));
        }
      }
      BigDecimal declared = total.value().setScale(SCALE, RoundingMode.HALF_UP);
      if (sum.setScale(SCALE, RoundingMode.HALF_UP).compareTo(declared) != 0
          && !multiCurrency(control, total, hasCurrency)) {
        return fail(
            CONTROL_TOTAL,
            "The total of "
                + total.label()
                + " is "
                + sum.setScale(SCALE, RoundingMode.HALF_UP)
                + "; the control file says "
                + declared);
      }
    }
    return multiCurrencyTotals(control, parsed, hasCurrency);
  }

  /**
   * A layout without a currency column (for example the invoice components) cannot split its totals
   * by currency: the totals of the same column and filter are compared across currencies.
   */
  private static boolean multiCurrency(
      ControlFile control, ControlFile.AmountTotal total, boolean hasCurrency) {
    return !hasCurrency
        && control.amounts().stream().filter(t -> sameMeasure(t, total)).count() > 1;
  }

  private static Optional<Failure> multiCurrencyTotals(
      ControlFile control, ParsedFile parsed, boolean hasCurrency) {
    if (hasCurrency) {
      return Optional.empty();
    }
    Set<String> done = new HashSet<>();
    for (ControlFile.AmountTotal total : control.amounts()) {
      String key = total.column() + "|" + total.filterColumn() + "|" + total.filterValue();
      if (!done.add(key) || !multiCurrency(control, total, false)) {
        continue;
      }
      BigDecimal declared =
          control.amounts().stream()
              .filter(t -> sameMeasure(t, total))
              .map(ControlFile.AmountTotal::value)
              .reduce(BigDecimal.ZERO, BigDecimal::add)
              .setScale(SCALE, RoundingMode.HALF_UP);
      BigDecimal sum = BigDecimal.ZERO;
      for (ParsedFile.RawRow row : parsed.rows()) {
        if (applies(total, row.values(), false)) {
          sum = sum.add(amount(row.values().get(total.column())));
        }
      }
      if (sum.setScale(SCALE, RoundingMode.HALF_UP).compareTo(declared) != 0) {
        return fail(
            CONTROL_TOTAL,
            "The total of "
                + total.column()
                + " in all currencies is "
                + sum.setScale(SCALE, RoundingMode.HALF_UP)
                + "; the control file says "
                + declared);
      }
    }
    return Optional.empty();
  }

  private static boolean sameMeasure(ControlFile.AmountTotal a, ControlFile.AmountTotal b) {
    return a.column().equals(b.column())
        && String.valueOf(a.filterColumn()).equals(String.valueOf(b.filterColumn()))
        && String.valueOf(a.filterValue()).equals(String.valueOf(b.filterValue()));
  }

  private static boolean applies(
      ControlFile.AmountTotal total, Map<String, String> values, boolean hasCurrency) {
    if (total.filterColumn() != null
        && !total.filterValue().equalsIgnoreCase(values.getOrDefault(total.filterColumn(), ""))) {
      return false;
    }
    return !hasCurrency
        || total.currency() == null
        || total.currency().equalsIgnoreCase(values.getOrDefault(CURRENCY, ""));
  }

  private static BigDecimal amount(String raw) {
    if (raw == null || raw.isBlank()) {
      return BigDecimal.ZERO;
    }
    try {
      return new BigDecimal(raw.strip());
    } catch (NumberFormatException e) {
      return BigDecimal.ZERO;
    }
  }

  private static Optional<Failure> hash(ControlFile control, Layout layout, ParsedFile parsed) {
    if (control.hashTotal() == null || control.hashTotal().isBlank()) {
      return fail(HASH_TOTAL, "The control file has no hash total");
    }
    String computed = computeHash(control, layout, parsed);
    if (!normalise(control.hashTotal()).equals(computed)) {
      return fail(
          HASH_TOTAL,
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

  private static Optional<Failure> fail(String code, String message) {
    return Optional.of(new Failure(code, message));
  }

  /**
   * A failed check.
   *
   * @param code check code
   * @param message message with the difference
   */
  record Failure(String code, String message) {}
}
