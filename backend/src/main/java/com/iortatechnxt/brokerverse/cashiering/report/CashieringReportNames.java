package com.iortatechnxt.brokerverse.cashiering.report;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * The file names of the Cashiering reports (FRS.CSH.09.01.02, 09.02): {@code <Report Name>_<date of
 * extraction MMDDYYYY>}, with the report names of the client's list.
 */
final class CashieringReportNames {

  private static final DateTimeFormatter MMDDYYYY =
      DateTimeFormatter.ofPattern("MMddyyyy", Locale.ROOT);

  private static final Map<String, String> NAMES =
      Map.ofEntries(
          Map.entry("CSH-APPLIED-PREM", "Manually Applied Payments"),
          Map.entry("CSH-APPLIED-COMM", "Applied Commission Reports"),
          Map.entry("CSH-MINBAL-EXCESS", "Minimal Balance of Unapplied Payments"),
          Map.entry("CSH-CANCELLED-OR", "Cancelled Official Receipts"),
          Map.entry("CSH-CANCELLED-AR", "Cancelled Acknowledgment Receipts"),
          Map.entry(
              "CSH-UNAPPLIED-COMM-MANCOM",
              "Unapplied Commission Receivable Extract for Mancom Reports"),
          Map.entry(
              "CSH-UNAPPLIED-COMM-YTD",
              "Unapplied Commission Receivable Payments YTD balance per Criteria"),
          Map.entry("CSH-MINBAL-PREMIUM", "Unpplied Premium Minimal Balance"),
          Map.entry("CSH-MINBAL-COMMISSION", "Unapplied Commission Minimal Balance"),
          Map.entry("CSH-DAILY-CASH-REC", "Daily Cash Reconciliation Reports"),
          Map.entry("CSH-ADVANCE-PAYMENT", "Unapplied Premium Payment Transaction"),
          Map.entry("CSH-DIRECT-PAYMENT", "Direct Payment"),
          Map.entry("CSH-REINSTATEMENT-MON", "Reinstatement Monitoring Report"),
          Map.entry("CSH-REAPPLICATION", "Re-Application Report"),
          Map.entry("CSH-CERT-OF-PAYMENT", "Certification of Payment"),
          Map.entry("CSH-REINSTATEMENT", "Reinstatement"),
          Map.entry("CSH-CWT", "CWT Monitoring Report (Premium)"),
          Map.entry("CSH-DISPOSITION-SUMMARY", "Summary Per Disposition - Remittance"),
          Map.entry("CSH-PDC-WAREHOUSE", "Post-dated Checks Warehousing"),
          Map.entry("CSH-PRIORITY-POSTED", "Priority Posted Accounts"),
          Map.entry("CSH-PAYMENT-REVERSAL", "Payment Reversals"),
          Map.entry("CSH-AR-OUTSTANDING", "AR Outstanding with Ageing"),
          Map.entry("CSH-BATCH-RUN", "Payment Batch Run Report"));

  private CashieringReportNames() {}

  /**
   * The file name of a report extracted on a date.
   *
   * @param code report code
   * @param date date of extraction
   * @return name without extension, null for a report without a convention
   */
  static String fileName(String code, LocalDate date) {
    String name = NAMES.get(code);
    return name == null ? null : name + "_" + date.format(MMDDYYYY);
  }
}
