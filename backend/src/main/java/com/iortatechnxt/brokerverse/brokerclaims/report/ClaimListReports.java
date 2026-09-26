package com.iortatechnxt.brokerverse.brokerclaims.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The Claims lists of the BRD report list (p.42-43; BRCLM.029/031, FR-CM-061): outstanding claims,
 * outstanding claims past due, and settled claims.
 */
public final class ClaimListReports {

  /** List of all Outstanding Claims. */
  public static final String OUTSTANDING = "BCL-OUTSTANDING";

  /** Outstanding Claims 90 Days Past Due. */
  public static final String PAST_DUE = "BCL-OUTSTANDING-PAST-DUE";

  /** List of all Settled Claims. */
  public static final String SETTLED = "BCL-SETTLED";

  private static final String DAYS = "days";

  private static final String OLDEST_FIRST = " order by c.reported_date, c.claim_no";

  private static final String OUTSTANDING_SQL =
      BclReportSql.CLAIM_SELECT + BclReportSql.OUTSTANDING_ON + OLDEST_FIRST;

  private static final String PAST_DUE_SQL =
      BclReportSql.CLAIM_SELECT
          + BclReportSql.OUTSTANDING_ON
          + " and (cast(:asOf as date) - c.reported_date) > :days"
          + OLDEST_FIRST;

  private static final String SETTLED_SQL =
      BclReportSql.CLAIM_SELECT
          + " and c.phase = 'CLOSED' and c.date_settled between :settledFrom and :settledTo"
          + " and exists (select 1 from bcl_lov_attribute a where a.type_code = 'BCL_SETTLEMENT_TYPE'"
          + " and a.code = c.settlement_type_code and a.attribute = 'outcome'"
          + " and a.value = 'SETTLED') order by c.date_settled, c.claim_no";

  private static final String TEMP_NOTE =
      "Outstanding = phases New, In progress and Temporarily closed on the as-of date (CLQ06);"
          + " ages in calendar days, the reported date is day zero.";

  private ClaimListReports() {}

  /** List of all Outstanding Claims (BRCLM.031, p.42). */
  @Component
  public static class Outstanding implements ReportDefinition {

    private final BclReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public Outstanding(BclReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return ReportMetadata.claimsHandling(
          OUTSTANDING,
          "List of all Outstanding Claims",
          "Open and temporarily closed claims with ages, follow-up and insurer claim numbers"
              + " (BRCLM.031)",
          BclReportSql.asOfFilters());
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      LocalDate asOf = sql.asOf(p);
      return TabularReportBuilder.of(p)
          .columns(ClaimColumns.outstanding())
          .rows(sql.claimRows(OUTSTANDING_SQL, BclReportSql.args(p), asOf))
          .presorted()
          .note(TEMP_NOTE)
          .build();
    }
  }

  /** Outstanding Claims 90 Days Past Due (BRCLM.031, p.43). */
  @Component
  public static class PastDue implements ReportDefinition {

    private final BclReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public PastDue(BclReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      List<ParameterSpec> params = BclReportSql.asOfFilters();
      params.add(2, ParameterSpec.optional(DAYS, "Past due after (days)", ParameterType.NUMBER));
      return ReportMetadata.claimsHandling(
          PAST_DUE,
          "Outstanding Claims 90 Days Past Due",
          "Outstanding claims older than BCL_PAST_DUE_DAYS from the reported date (BRCLM.031)",
          params);
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      LocalDate asOf = sql.asOf(p);
      Map<String, Object> args = BclReportSql.args(p);
      int days = args.get(DAYS) instanceof Integer d ? d : sql.pastDueDays();
      args.put(DAYS, days);
      return TabularReportBuilder.of(p)
          .columns(ClaimColumns.outstanding())
          .rows(sql.claimRows(PAST_DUE_SQL, args, asOf))
          .presorted()
          .note("Claims outstanding for more than " + days + " days on the as-of date.")
          .build();
    }
  }

  /** List of all Settled Claims (BRCLM.029, p.42). */
  @Component
  public static class Settled implements ReportDefinition {

    private final BclReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public Settled(BclReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return ReportMetadata.claimsHandling(
          SETTLED,
          "List of all Settled Claims",
          "Claims closed with a settled outcome, by date settled (BRCLM.029)",
          BclReportSql.rangeFilters("settledFrom", "settledTo", "Date settled", true));
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      Map<String, Object> args = BclReportSql.args(p);
      LocalDate to = (LocalDate) args.get("settledTo");
      return TabularReportBuilder.of(p)
          .columns(ClaimColumns.settled())
          .rows(sql.claimRows(SETTLED_SQL, args, to))
          .presorted()
          .note("Settled outcome = settlement type attribute outcome SETTLED (CLQ05, CLQ09).")
          .build();
    }
  }
}
