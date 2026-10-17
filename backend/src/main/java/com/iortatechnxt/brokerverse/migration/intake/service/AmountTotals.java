package com.iortatechnxt.brokerverse.migration.intake.service;

import com.iortatechnxt.brokerverse.bulk.service.ParsedFile;
import com.iortatechnxt.brokerverse.migration.intake.service.IntakeChecks.Failure;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The amount totals of an extract against its control file (DATA_MIGRATION_DESIGN section 5.2): per
 * column, filter and currency, or across currencies when the layout has no currency column.
 */
final class AmountTotals {

  private static final String CURRENCY = "currency";
  private static final int SCALE = 2;

  private AmountTotals() {}

  /**
   * Compares the amount totals of the control file with the file.
   *
   * @param control control file
   * @param parsed data file
   * @return the first difference
   */
  static Optional<Failure> check(ControlFile control, ParsedFile parsed) {
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
        return IntakeChecks.fail(
            IntakeChecks.CONTROL_TOTAL,
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
        return IntakeChecks.fail(
            IntakeChecks.CONTROL_TOTAL,
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
}
