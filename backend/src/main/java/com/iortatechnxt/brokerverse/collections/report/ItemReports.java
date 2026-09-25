package com.iortatechnxt.brokerverse.collections.report;

import org.springframework.stereotype.Component;

/** The Collections reports listing items (BDOI_CLXN_BRD_SPEC section 7; COLLECTIONS_DESIGN 11). */
public final class ItemReports {

  /** Outstanding PR List code. */
  public static final String OUTSTANDING_PR = "CLX-OUTSTANDING-PR";

  /** Full Production Report code. */
  public static final String FULL_PRODUCTION = "CLX-FULL-PRODUCTION";

  /** Completed collections code. */
  public static final String COMPLETED = "CLX-COMPLETED-COLLECTIONS";

  /** Invoices with disposition code. */
  public static final String WITH_DISPOSITION = "CLX-INVOICES-WITH-DISPOSITION";

  private static final String BY_AGING = " order by i.aging_days desc, i.invoice_no";

  private ItemReports() {}

  /**
   * Outstanding PR List (BRCLXN.045, p.60): the open items of the worklist, oldest first; the daily
   * file of {@code CLX_DAILY_FILES} and the worklist export.
   */
  @Component
  public static class OutstandingPr extends ItemListReport {

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public OutstandingPr(ClxReportSql sql) {
      super(
          sql,
          ClxReportSql.metadata(
              OUTSTANDING_PR,
              "Outstanding PR List",
              "Open collection items with the outstanding premium (BRCLXN.001-012, 045)",
              false),
          " and i.status = 'OPEN'" + BY_AGING);
    }
  }

  /**
   * Full Production Report (BRCLXN.045, p.61): every item of the invoices booked in the period,
   * whatever their status, with categories and remarks.
   */
  @Component
  public static class FullProduction extends ItemListReport {

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public FullProduction(ClxReportSql sql) {
      super(
          sql,
          ClxReportSql.metadata(
              FULL_PRODUCTION,
              "Full Production Report",
              "Collection items of the invoices booked in the period, any status (BRCLXN.045)",
              true),
          " and i.booking_date between :from and :to order by i.booking_date, i.invoice_no");
    }
  }

  /**
   * Completed collections (p.43-45, BRCLXN.022): items closed as paid or below the threshold in the
   * period; their history is kept.
   */
  @Component
  public static class Completed extends ItemListReport {

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public Completed(ClxReportSql sql) {
      super(
          sql,
          ClxReportSql.metadata(
              COMPLETED,
              "Completed Collections",
              "Items completed (paid or below the threshold) in the period (BRCLXN.008, 022)",
              true),
          " and i.status = 'COMPLETED' and i.completed_on between :from and :to"
              + " order by i.completed_on, i.invoice_no");
    }
  }

  /** Invoices with disposition (p.43): the open items with a current collector disposition. */
  @Component
  public static class WithDisposition extends ItemListReport {

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public WithDisposition(ClxReportSql sql) {
      super(
          sql,
          ClxReportSql.metadata(
              WITH_DISPOSITION,
              "Invoices with Disposition",
              "Open items with their current collector disposition (BRCLXN.016-021)",
              false),
          " and i.status = 'OPEN' and i.disposition_code is not null"
              + " order by i.disposition_code, i.invoice_no");
    }
  }
}
