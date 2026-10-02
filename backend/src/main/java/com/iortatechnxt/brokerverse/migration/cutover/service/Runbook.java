package com.iortatechnxt.brokerverse.migration.cutover.service;

import com.iortatechnxt.brokerverse.migration.cutover.domain.CutoverTask;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The standard runbook of a cutover plan (DATA_MIGRATION_DESIGN 17.2-17.5): the tasks planned
 * relative to the go-live date T - pre-load of reference data and clients, freezes, the last legacy
 * business day and EOD, the final extracts and the loads in load order, reconciliation, sign-offs,
 * screening, smoke test, the go / no-go meeting and go-live - and the twelve go / no-go criteria.
 */
final class Runbook {

  private static final String MANUAL = "MANUAL";
  private static final String VERIFICATION = "Verification";

  private static final String LEAD = "DATA_MIGRATION_LEAD";
  private static final String OPERATOR = "MIGRATION_OPERATOR";
  private static final String LOADS = "Final loads";

  /** The go / no-go criteria: number, name, threshold and measure (MANUAL when recorded). */
  static final List<Criterion> CRITERIA =
      List.of(
          new Criterion(1, "Day-1 objects accepted (G6)", "100 %", "DAY1_ACCEPTED"),
          new Criterion(
              2,
              "Count reconciliation per object",
              "loaded + skipped + rejected + excluded = staged; received = control",
              "COUNTS"),
          new Criterion(
              3,
              "Financial rejects (open invoices, unapplied payments, trial balance)",
              "0, or each excluded item approved by the owner",
              "FINANCIAL_REJECTS"),
          new Criterion(
              4,
              "Amount reconciliation of open items, unapplied payments and the provisional TB",
              "0.00 per currency",
              "AMOUNTS"),
          new Criterion(5, "Migration Clearing", "0.00 per branch and currency", "CLEARING"),
          new Criterion(
              6,
              "Legacy control accounts against the legacy sub-ledgers",
              "0.00",
              "LEGACY_CONTROL"),
          new Criterion(7, "Client review queue", "empty", "CLIENT_QUEUE"),
          new Criterion(
              8,
              "Business smoke test (OTC payment, automatch, remittance preview, invoice 360)",
              "passed",
              MANUAL),
          new Criterion(9, "Rollback point (database snapshot)", "taken and verified", MANUAL),
          new Criterion(10, "Hypercare roster and support channels", "in place", MANUAL),
          new Criterion(
              11,
              "Preliminary TB signed as the provisional opening; legacy GL access list signed",
              "signed",
              MANUAL),
          new Criterion(
              12,
              "Renewal: headers of every expiry to May loaded, RA-sent file loaded, staffing plan",
              "complete; confirmed",
              MANUAL));

  private static final int PHASE = 2;
  private static final int TASK = 3;
  private static final int OWNER = 4;
  private static final int OBJECT = 5;
  private static final int AFTER = 6;

  private static final String[][] TASKS = {
    // days from T, hour, phase, task, owner, object, depends on
    {
      "-14",
      "9",
      "Pre-load",
      "Pre-load reference data (lists, branches, sales, insurers, products)",
      OPERATOR,
      "R01",
      ""
    },
    {"-14", "14", "Pre-load", "Pre-load clients and payout accounts", OPERATOR, "C01", "1"},
    {"-13", "9", "Pre-load", "Daily client delta loads until the freeze", OPERATOR, "C01", "2"},
    {"-7", "17", "Freeze", "Code map freeze; legacy reference-data change freeze", LEAD, "", ""},
    {
      "-5",
      "17",
      "Freeze",
      "Last legacy business day; final RA-sent file compiled (maker-checker)",
      "RENEWAL_PROCESSING",
      "P03",
      ""
    },
    {
      "-3",
      "22",
      "Freeze",
      "Last legacy EOD; business freeze; legacy GL restricted to FY2027 adjustments",
      LEAD,
      "",
      "4,5"
    },
    {
      "-2",
      "6",
      LOADS,
      "Final extracts after EOD with control files; intake checks",
      OPERATOR,
      "",
      "6"
    },
    {"-2", "7", LOADS, "Database snapshot (rollback point) verified", "IT_OPERATIONS", "", "7"},
    {"-2", "8", LOADS, "Load in-force policy headers and the RA-sent file", OPERATOR, "P01", "8"},
    {"-2", "11", LOADS, "Load open legacy invoices", OPERATOR, "F01", "9"},
    {"-2", "13", LOADS, "Load legacy unapplied payments", OPERATOR, "F02", "10"},
    {"-2", "14", LOADS, "Load the open collection follow-up", OPERATOR, "F03", "10"},
    {"-2", "16", LOADS, "Post the provisional opening trial balance", OPERATOR, "G01", "10,11"},
    {
      "-1",
      "8",
      VERIFICATION,
      "Reconciliation L1-L5 of every object; Migration Clearing at zero",
      "MIGRATION_RECON_APPROVER",
      "",
      "13"
    },
    {"-1", "10", VERIFICATION, "Sign-offs G5 and G6 of every day-1 object", "DATA_OWNER", "", "14"},
    {
      "-1",
      "12",
      VERIFICATION,
      "Corrected RA-sent rows resubmitted and approved (deadline)",
      "RENEWAL_PROCESSING",
      "P03",
      "9"
    },
    {"-1", "13", VERIFICATION, "Full screening run of the migrated clients", "COMPLIANCE", "", "2"},
    {"-1", "15", VERIFICATION, "Business smoke test on legacy invoices", LEAD, "", "15"},
    {"-1", "18", "Go / no-go", "Go / no-go meeting", "MIGRATION_GONOGO", "", "15,17,18"},
    {"0", "4", "Go-live", "Go-live renewal extraction", "RENEWAL_PROCESSING", "", "19"},
    {"0", "8", "Go-live", "BIBS open for business; hypercare starts", LEAD, "", "19"},
  };

  private Runbook() {}

  /**
   * The tasks of a plan.
   *
   * @param goLive go-live date T
   * @return tasks
   */
  static List<CutoverTask.Data> tasks(LocalDate goLive) {
    List<CutoverTask.Data> out = new ArrayList<>();
    for (int i = 0; i < TASKS.length; i++) {
      String[] t = TASKS[i];
      LocalDateTime start =
          LocalDateTime.of(
              goLive.plusDays(Integer.parseInt(t[0])), LocalTime.of(Integer.parseInt(t[1]), 0));
      out.add(
          new CutoverTask.Data(
              i + 1,
              t[PHASE],
              t[TASK],
              t[OWNER],
              t[OBJECT].isEmpty() ? null : t[OBJECT],
              t[AFTER].isEmpty() ? null : t[AFTER],
              start,
              start.plusHours(2)));
    }
    return out;
  }

  /**
   * A go / no-go criterion.
   *
   * @param no number
   * @param name name
   * @param threshold threshold
   * @param measure measure code, MANUAL when recorded by the lead
   */
  record Criterion(int no, String name, String threshold, String measure) {}
}
