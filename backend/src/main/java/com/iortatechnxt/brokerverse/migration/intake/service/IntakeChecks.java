package com.iortatechnxt.brokerverse.migration.intake.service;

import com.iortatechnxt.brokerverse.bulk.service.ParsedFile;
import com.iortatechnxt.brokerverse.migration.mapping.domain.Layout;
import java.util.LinkedHashSet;
import java.util.List;
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
    Optional<Failure> amounts = AmountTotals.check(control, parsed);
    return amounts.isPresent() ? amounts : HashTotals.check(control, layout, parsed);
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
