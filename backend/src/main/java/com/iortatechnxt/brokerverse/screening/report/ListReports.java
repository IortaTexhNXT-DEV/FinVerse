package com.iortatechnxt.brokerverse.screening.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The watchlist reports of the Compliance category (p.6 "List of Sanctioned Names"; SNSRP-202): the
 * list entries by source, list type and status with their aliases and last change, and the records
 * that failed ingestion. Watchlists are platform reference data (no company).
 */
public final class ListReports {

  /** List of sanctioned names. */
  public static final String SANCTIONED_NAMES = "SCR-SANCTIONED-NAMES";

  /** Unsuccessful ingestion records. */
  public static final String INGEST_ERRORS = "SCR-INGEST-ERRORS";

  private static final String SOURCE = "source";
  private static final String LIST_TYPE = "listType";
  private static final String OPTIONAL_PERIOD =
      " and (cast(:fromDate as date) is null or %s >= :fromDate)"
          + " and (cast(:toDate as date) is null or %s <= :toDate)";

  private ListReports() {}

  private static List<ParameterSpec> period() {
    return List.of(
        ParameterSpec.optional(ScrReportSql.FROM, "Date From", ParameterType.DATE),
        ParameterSpec.optional(ScrReportSql.TO, "Date To", ParameterType.DATE));
  }

  private static Map<String, Object> args(ReportParameters p, String... names) {
    Map<String, Object> args = new HashMap<>();
    args.put(ScrReportSql.FROM, p.optionalDate(ScrReportSql.FROM).orElse(null));
    args.put(ScrReportSql.TO, p.optionalDate(ScrReportSql.TO).orElse(null));
    for (String name : names) {
      args.put(name, ScrReportSql.text(p, name));
    }
    return args;
  }

  /** List of Sanctioned Names (p.6; FRS 6.1.4). */
  @Component
  public static class SanctionedNames implements ReportDefinition {

    private static final String SQL =
        "select s.code || ' / ' || e.list_type as source_list, e.external_ref, e.primary_name,"
            + " (select string_agg(a.alias_name, '; ' order by a.alias_name)"
            + " from scr_watchlist_alias a where a.entry_id = e.id) as aliases, e.entity_type,"
            + " concat_ws(' / ', cast(e.birth_date as varchar), e.nationality) as birth_nationality,"
            + " e.listed_on, e.delisted_on, e.status,"
            + " (select w.change_type || ' ' || coalesce(r.run_no, 'manual') || ' '"
            + " || to_char(coalesce(w.decided_at, w.created_at) at time zone 'Asia/Manila', 'YYYY-MM-DD')"
            + " from scr_watchlist_change w left join scr_ingestion_run r on r.id = w.run_id"
            + " where w.entry_id = e.id order by w.id desc limit 1) as last_change"
            + " from scr_watchlist_entry e join scr_watchlist_source s on s.id = e.source_id"
            + " where (cast(:source as varchar) is null or s.code = :source)"
            + " and (cast(:listType as varchar) is null or e.list_type = :listType)"
            + " and (cast(:status as varchar) is null or e.status = :status)"
            + " and ((cast(:fromDate as date) is null and cast(:toDate as date) is null)"
            + " or (e.listed_on between coalesce(cast(:fromDate as date), date '1900-01-01')"
            + " and coalesce(cast(:toDate as date), date '9999-12-31'))"
            + " or (e.delisted_on between coalesce(cast(:fromDate as date), date '1900-01-01')"
            + " and coalesce(cast(:toDate as date), date '9999-12-31')))"
            + " order by e.primary_name";

    private final ScrReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public SanctionedNames(ScrReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      List<ParameterSpec> params = new ArrayList<>();
      params.add(ParameterSpec.optional(SOURCE, "Source", ParameterType.TEXT));
      params.add(ParameterSpec.optional(LIST_TYPE, "List Type", ParameterType.TEXT));
      params.add(ParameterSpec.optional("status", "Status", ParameterType.TEXT));
      params.addAll(period());
      return ReportMetadata.compliance(
          SANCTIONED_NAMES,
          "List of Sanctioned Names",
          "Watchlist entries by source and list type, listed or delisted in the period (p.6)",
          params);
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text("source_list", "Source / List Type"),
              ReportColumn.text("external_ref", "External Reference"),
              ReportColumn.text("primary_name", "Name"),
              ReportColumn.text("aliases", "Aliases"),
              ReportColumn.text("entity_type", "Entity Type"),
              ReportColumn.text("birth_nationality", "Birth Date / Nationality"),
              ReportColumn.date("listed_on", "Listed On"),
              ReportColumn.date("delisted_on", "Delisted On"),
              ReportColumn.text("status", "Status"),
              ReportColumn.text("last_change", "Last Change"))
          .rows(sql.rows(SQL, args(p, SOURCE, LIST_TYPE, "status")))
          .presorted()
          .withoutGrandTotal()
          .note(ScrReportSql.LAYOUT_NOTE)
          .build();
    }
  }

  /** Unsuccessful Ingestion Records (SNSRP-202; FRS 6.1.6). */
  @Component
  public static class IngestErrors implements ReportDefinition {

    private static final String SQL =
        "select cast(r.started_at at time zone 'Asia/Manila' as date) as run_date, r.run_no,"
            + " s.code || ' - ' || s.name as source, r.file_name, x.line_no,"
            + " left(x.raw_record, 200) as raw_record, x.reason"
            + " from scr_ingestion_error x join scr_ingestion_run r on r.id = x.run_id"
            + " join scr_watchlist_source s on s.id = r.source_id"
            + " where (cast(:runNo as varchar) is null or r.run_no = :runNo)"
            + " and (cast(:source as varchar) is null or s.code = :source)"
            + String.format(
                OPTIONAL_PERIOD,
                "cast(r.started_at at time zone 'Asia/Manila' as date)",
                "cast(r.started_at at time zone 'Asia/Manila' as date)")
            + " order by r.run_no, x.line_no";

    private final ScrReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public IngestErrors(ScrReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      List<ParameterSpec> params = new ArrayList<>();
      params.add(ParameterSpec.optional("runNo", "Run No.", ParameterType.TEXT));
      params.add(ParameterSpec.optional(SOURCE, "Source", ParameterType.TEXT));
      params.addAll(period());
      return ReportMetadata.compliance(
          INGEST_ERRORS,
          "Unsuccessful Ingestion Records",
          "Watchlist records that failed ingestion, by run and line (SNSRP-202)",
          params);
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.date("run_date", "Run Date"),
              ReportColumn.text("run_no", "Run No."),
              ReportColumn.text(SOURCE, "Source"),
              ReportColumn.text("file_name", "File"),
              ScrReportSql.number("line_no", "Line"),
              ReportColumn.text("raw_record", "Record"),
              ReportColumn.text("reason", "Reason"))
          .rows(sql.rows(SQL, args(p, "runNo", SOURCE)))
          .presorted()
          .withoutGrandTotal()
          .note(ScrReportSql.LAYOUT_NOTE)
          .build();
    }
  }
}
