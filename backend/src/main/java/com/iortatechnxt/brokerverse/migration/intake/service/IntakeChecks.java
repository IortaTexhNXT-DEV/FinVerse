package com.iortatechnxt.brokerverse.migration.intake.service;

import com.iortatechnxt.brokerverse.bulk.service.ParsedFile;
import com.iortatechnxt.brokerverse.migration.mapping.domain.Layout;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The intake checks of an extract against its control file and its layout (DATA_MIGRATION_DESIGN
 * section 5.2; FR-DM-010): SHA-256, header, row count, amount totals per column and currency, and
 * the hash total of the key. Every check runs; any failure rejects the extract and the rejection
 * lists each failed check with its own reason. Nothing is staged.
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

  /** Room of the rejection message. */
  private static final int MESSAGE_LENGTH = 2000;

  private static final String ELLIPSIS = "...";

  private IntakeChecks() {}

  /**
   * Runs the file checks.
   *
   * @param sha256 computed SHA-256 of the data file
   * @param control control file
   * @param layout layout in force
   * @param columns column names of the layout
   * @param parsed parsed data file
   * @return every failed check in check order, empty when every check passes
   */
  static List<Failure> run(
      String sha256, ControlFile control, Layout layout, List<String> columns, ParsedFile parsed) {
    List<Failure> failures = new ArrayList<>();
    if (control.sha256() == null || !control.sha256().equalsIgnoreCase(sha256)) {
      failures.add(
          new Failure(CHECKSUM, "The file does not match the checksum in the control file"));
    }
    header(layout, columns, parsed.headers()).ifPresent(failures::add);
    int rows = parsed.rows().size();
    if (control.rowCount() == null || control.rowCount() != rows) {
      failures.add(
          new Failure(
              ROW_COUNT,
              "The file has "
                  + rows
                  + (rows == 1 ? " row" : " rows")
                  + "; the control file says "
                  + control.rowCount()));
    }
    AmountTotals.check(control, parsed).ifPresent(failures::add);
    HashTotals.check(control, layout, parsed).ifPresent(failures::add);
    return List.copyOf(failures);
  }

  /**
   * The name of a check as the rejection lists it.
   *
   * @param code check code
   * @return name
   */
  static String checkName(String code) {
    return switch (code) {
      case CHECKSUM -> "Checksum";
      case LAYOUT -> "Columns";
      case ROW_COUNT -> "Row count";
      case CONTROL_TOTAL -> "Amount total";
      case HASH_TOTAL -> "Hash total";
      default -> "Intake check";
    };
  }

  /**
   * The rejection of several failed checks: the code of the first, and one line per check ("Row
   * count: The file has 3 rows; the control file says 4").
   *
   * @param failures failed checks, not empty
   * @param names name of each check code
   * @return the rejection
   */
  static Failure combine(List<Failure> failures, Function<String, String> names) {
    String lines =
        failures.stream()
            .map(f -> names.apply(f.code()) + ": " + f.message())
            .collect(Collectors.joining("\n"));
    String message =
        lines.length() > MESSAGE_LENGTH
            ? lines.substring(0, MESSAGE_LENGTH - ELLIPSIS.length()) + ELLIPSIS
            : lines;
    return new Failure(failures.get(0).code(), message);
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

  static Optional<Failure> fail(String code, String message) {
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
