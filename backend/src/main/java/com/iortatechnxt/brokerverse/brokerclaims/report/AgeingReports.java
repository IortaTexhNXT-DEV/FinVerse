package com.iortatechnxt.brokerverse.brokerclaims.report;

import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimAgeing;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The claims ageing reports (BRCLM.025-028, p.43; FR-CL-060): outstanding claims with the age
 * overall and the bucket of {@code BCL_AGEING_BUCKETS}, totals per bucket and insurer; and the
 * ageing per status, grouped by status then insurer, with the age this stage and the time spent in
 * each earlier status (status history).
 */
public final class AgeingReports {

  /** Claims Aging (overall). */
  public static final String AGEING = "BCL-AGEING";

  /** Claims Aging per Status. */
  public static final String AGEING_STATUS = "BCL-AGEING-STATUS";

  private static final String BUCKET = "bucket";
  private static final String INSURER = "insurers";

  private static final String OUTSTANDING_SQL =
      BclReportSql.CLAIM_SELECT + BclReportSql.OUTSTANDING_ON + " order by c.reported_date";

  private static final String HISTORY_SQL =
      "select h.claim_id, string_agg(coalesce(s.label, h.from_status) || ': '"
          + " || h.days_in_previous || ' d', '; ' order by h.changed_at) as earlier"
          + " from bcl_status_history h join bcl_claim c on c.id = h.claim_id"
          + " left join lov_value s on s.type_code = 'BCL_CLAIM_STATUS' and s.code = h.from_status"
          + " where c.company_id = :companyId and h.from_status is not null"
          + " and h.from_status <> h.to_status and h.days_in_previous is not null"
          + " group by h.claim_id";

  private AgeingReports() {}

  private static List<ReportColumn> ageingColumns() {
    List<ReportColumn> columns = new ArrayList<>();
    columns.add(ReportColumn.text(BUCKET, "Age Bracket"));
    columns.add(ClaimColumns.CLAIMS);
    columns.addAll(ClaimColumns.outstanding());
    return columns;
  }

  /** Claims Aging overall (BRCLM.025/026). */
  @Component
  public static class Ageing implements ReportDefinition {

    private final BclReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public Ageing(BclReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return ReportMetadata.claimsHandling(
          AGEING,
          "Claims Aging",
          "Outstanding claims by age bracket from the reported date, per insurer (BRCLM.026)",
          BclReportSql.asOfFilters());
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      LocalDate asOf = sql.asOf(p);
      List<Integer> bounds = sql.bounds();
      List<String> order = ClaimAgeing.buckets(bounds);
      List<Map<String, Object>> rows = new ArrayList<>();
      for (Map<String, Object> row : sql.claimRows(OUTSTANDING_SQL, BclReportSql.args(p), asOf)) {
        Map<String, Object> out = new LinkedHashMap<>(row);
        out.put(BUCKET, ClaimAgeing.bucket((Integer) row.get("age_overall"), bounds));
        out.put(ClaimColumns.CLAIMS.key(), 1);
        rows.add(out);
      }
      rows.sort(
          Comparator.<Map<String, Object>>comparingInt(r -> order.indexOf((String) r.get(BUCKET)))
              .thenComparing(r -> String.valueOf(r.get(INSURER))));
      return TabularReportBuilder.of(p)
          .columns(ageingColumns())
          .groupBy(BUCKET, "Age Bracket")
          .groupBy(INSURER, "Insurer")
          .rows(rows)
          .presorted()
          .note("Brackets from parameter BCL_AGEING_BUCKETS; temporarily closed claims keep ageing.")
          .build();
    }
  }

  /** Claims Aging per Status (BRCLM.027/028). */
  @Component
  public static class AgeingStatus implements ReportDefinition {

    private final BclReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public AgeingStatus(BclReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return ReportMetadata.claimsHandling(
          AGEING_STATUS,
          "Claims Aging per Status",
          "Outstanding claims grouped by status and insurer with the age this stage and the days"
              + " spent in earlier statuses (BRCLM.028)",
          BclReportSql.asOfFilters());
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      LocalDate asOf = sql.asOf(p);
      Map<String, Object> args = BclReportSql.args(p);
      Map<Object, Object> earlier = new LinkedHashMap<>();
      sql.rows(HISTORY_SQL, args).forEach(r -> earlier.put(r.get("claim_id"), r.get("earlier")));
      List<Map<String, Object>> rows = new ArrayList<>();
      for (Map<String, Object> row : sql.claimRows(OUTSTANDING_SQL, args, asOf)) {
        Map<String, Object> out = new LinkedHashMap<>(row);
        out.put("earlier", earlier.get(row.get("id")));
        out.put(ClaimColumns.CLAIMS.key(), 1);
        rows.add(out);
      }
      List<ReportColumn> columns = new ArrayList<>();
      columns.add(ClaimColumns.CLAIMS);
      columns.addAll(ClaimColumns.outstanding());
      columns.add(ReportColumn.text("earlier", "Days in Earlier Statuses"));
      return TabularReportBuilder.of(p)
          .columns(columns)
          .groupBy("status", "Claim Status")
          .groupBy(INSURER, "Insurer")
          .rows(rows)
          .note("Age this stage = days since the current status was set (BRCLM.027).")
          .build();
    }
  }
}
