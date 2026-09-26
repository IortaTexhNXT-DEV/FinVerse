package com.iortatechnxt.brokerverse.screening.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The client reports of the Compliance category (p.7-8 "Extract list of High-risk Clients", "List
 * of approved PEP clients"; capability 4): high-risk, PEP and watchlist-tagged clients with their
 * category, rating, tags, open case, active policy and marketing unit, and the approved PEP
 * clients.
 */
public final class ClientReports {

  /** High-risk clients. */
  public static final String HIGH_RISK = "SCR-HIGH-RISK-CLIENTS";

  /** Approved PEP clients. */
  public static final String PEP = "SCR-PEP-CLIENTS";

  /** As-of date parameter. */
  static final String AS_OF = "asOfDate";

  private static final String CLIENT_CODE = "client_code";
  private static final String NAME = "display_name";
  private static final String UNIT = "unit";
  private static final String RISK_CATEGORY = "riskCategory";
  private static final String CLIENT_TYPE = "clientType";

  /** The latest risk-profile change of a client on or before the as-of date. */
  static final String LAST_PROFILE =
      " left join lateral (select p.category_code, p.risk_version_id, p.effective_at, p.source"
          + " from scr_client_risk_profile p where p.client_id = c.id"
          + " and cast(p.effective_at at time zone 'Asia/Manila' as date) <= :asOfDate"
          + " order by p.effective_at desc, p.id desc limit 1) lp on true";

  /** The latest case of a client (marketing unit, unit head, account officer). */
  static final String LAST_CASE =
      " left join lateral (select k.marketing_unit, k.unit_head, k.account_officer from scr_case k"
          + " where k.client_id = c.id order by k.id desc limit 1) lc on true";

  private ClientReports() {}

  private static List<ParameterSpec> base() {
    return List.of(
        ScrReportSql.company(),
        ParameterSpec.required(AS_OF, "As-of Date", ParameterType.DATE).withDefault("TODAY"),
        ParameterSpec.optional(ScrReportSql.UNIT, "Marketing Unit", ParameterType.TEXT));
  }

  private static Map<String, Object> args(ReportParameters p) {
    Map<String, Object> args = ScrReportSql.args(p);
    args.put(AS_OF, p.date(AS_OF));
    return args;
  }

  /** High-risk Clients (FR-SS-045; FRS 6.1.1). */
  @Component
  public static class HighRisk implements ReportDefinition {

    private static final String SQL =
        "select * from (select coalesce(c.client_code, c.prospect_code) as client_code,"
            + " c.display_name, c.client_type, lp.category_code as risk_category, rc.tier,"
            + " c.risk_rating, tg.tags, cast(lp.effective_at at time zone 'Asia/Manila' as date)"
            + " as tagged_on, lp.source, coalesce(oc.open_case, 'None') as open_case,"
            + " case when exists (select 1 from acc_account a where a.client_id = c.id"
            + " and a.status in ('POLICY_ISSUED', 'BOOKED')) then 'Yes' else 'No' end as active_policy,"
            + " lc.marketing_unit, lc.unit_head,"
            + " coalesce(lc.marketing_unit, '') || coalesce(' / ' || lc.unit_head, '') as unit"
            + " from crm_client c"
            + LAST_PROFILE
            + " left join scr_risk_category rc on rc.version_id = lp.risk_version_id"
            + " and rc.code = lp.category_code"
            + " left join lateral (select string_agg(t.tag_code, ', ' order by t.tag_code) as tags"
            + " from crm_client_tag t where t.client_id = c.id and t.active"
            + " and t.tag_code in ('PEP', 'WATCHLIST_REVIEW')) tg on true"
            + " left join lateral (select k.case_no || ' (' || k.stage || ')' as open_case"
            + " from scr_case k where k.client_id = c.id and k.status = 'OPEN'"
            + " order by k.id desc limit 1) oc on true"
            + LAST_CASE
            + " where c.company_id = :companyId and c.status <> 'INACTIVE'"
            + " and (c.risk_rating = 'HIGH' or tg.tags is not null)) r"
            + " where (cast(:riskCategory as varchar) is null or r.risk_category = :riskCategory)"
            + " and (cast(:marketingUnit as varchar) is null or r.marketing_unit = :marketingUnit)"
            + " and (cast(:unitHead as varchar) is null or lower(r.unit_head) = lower(:unitHead))"
            + " and (cast(:clientType as varchar) is null or r.client_type = :clientType)"
            + " order by r.tier nulls last, r.display_name";

    private final ScrReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public HighRisk(ScrReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      List<ParameterSpec> params = new ArrayList<>(base());
      params.add(ParameterSpec.optional(ScrReportSql.HEAD, "Unit Head", ParameterType.TEXT));
      params.add(ParameterSpec.optional(RISK_CATEGORY, "Risk Category", ParameterType.TEXT));
      params.add(
          new ParameterSpec(
              CLIENT_TYPE,
              "Client Type",
              ParameterType.SELECT,
              false,
              List.of("INDIVIDUAL", "CORPORATE"),
              null));
      return ReportMetadata.compliance(
          HIGH_RISK,
          "High-risk Clients",
          "High-risk, PEP and watchlist-tagged clients with their open case (capability 4)",
          params);
    }

    /**
     * The high-risk clients (High-risk Clients screen, FR-SS-045).
     *
     * @param filter company, as-of date and filters
     * @return rows by column key
     */
    public List<Map<String, Object>> rows(Filter filter) {
      Map<String, Object> args = new HashMap<>();
      args.put(ScrReportSql.COMPANY, filter.companyId());
      args.put(AS_OF, filter.asOf());
      args.put(ScrReportSql.UNIT, filter.marketingUnit());
      args.put(ScrReportSql.HEAD, filter.unitHead());
      args.put(RISK_CATEGORY, filter.riskCategory());
      args.put(CLIENT_TYPE, filter.clientType());
      return sql.rows(SQL, args);
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      Map<String, Object> args = args(p);
      args.put(RISK_CATEGORY, ScrReportSql.text(p, RISK_CATEGORY));
      args.put(CLIENT_TYPE, ScrReportSql.text(p, CLIENT_TYPE));
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text(CLIENT_CODE, "Client Code"),
              ReportColumn.text(NAME, "Client Name"),
              ReportColumn.text("client_type", "Client Type"),
              ReportColumn.text("risk_category", "Risk Category"),
              ReportColumn.text("risk_rating", "Risk Rating"),
              ReportColumn.text("tags", "Tags"),
              ReportColumn.date("tagged_on", "Tagged On"),
              ReportColumn.text("source", "Source"),
              ReportColumn.text("open_case", "Open Case"),
              ReportColumn.text("active_policy", "Active Policy"),
              ReportColumn.text(UNIT, "Marketing Unit / Unit Head"))
          .rows(sql.rows(SQL, args))
          .presorted()
          .withoutGrandTotal()
          .note(ScrReportSql.LAYOUT_NOTE)
          .build();
    }
  }

  /**
   * The filters of the high-risk list.
   *
   * @param companyId company
   * @param asOf as-of date
   * @param riskCategory risk category, may be null
   * @param marketingUnit marketing unit, may be null
   * @param unitHead unit head, may be null
   * @param clientType INDIVIDUAL or CORPORATE, may be null
   */
  public record Filter(
      Long companyId,
      LocalDate asOf,
      String riskCategory,
      String marketingUnit,
      String unitHead,
      String clientType) {}

  /** List of Approved PEP Clients (p.7; FRS 6.1.5). */
  @Component
  public static class PepClients implements ReportDefinition {

    private static final String SQL =
        "select * from (select coalesce(c.client_code, c.prospect_code) as client_code,"
            + " c.display_name, cast(t.created_at at time zone 'Asia/Manila' as date) as pep_since,"
            + " (select m.entry_name || ' (' || m.source_code || ')' from scr_match m"
            + " where m.client_id = c.id and m.list_type = 'PEP' and m.status <> 'FALSE_POSITIVE'"
            + " order by m.id desc limit 1) as matched_entry,"
            + " (select k.case_no || coalesce(' / ' || k.disposition, '') from scr_case k"
            + " where k.client_id = c.id order by k.id desc limit 1) as case_outcome,"
            + " case when rc.requires_edd then 'Yes' else 'No' end as edd_required,"
            + " (select cast(k.closed_at at time zone 'Asia/Manila' as date) from scr_case k"
            + " where k.client_id = c.id and k.template_type = 'EDD' and k.status = 'CLOSED'"
            + " order by k.closed_at desc limit 1) as last_edd,"
            + " lc.marketing_unit,"
            + " coalesce(lc.marketing_unit, '') || coalesce(' / ' || lc.account_officer, '') as unit"
            + " from crm_client c join crm_client_tag t on t.client_id = c.id and t.active"
            + " and t.tag_code = 'PEP'"
            + LAST_PROFILE
            + " left join scr_risk_category rc on rc.version_id = lp.risk_version_id"
            + " and rc.code = lp.category_code"
            + LAST_CASE
            + " where c.company_id = :companyId and c.status <> 'INACTIVE'"
            + " and cast(t.created_at at time zone 'Asia/Manila' as date) <= :asOfDate) r"
            + " where (cast(:marketingUnit as varchar) is null or r.marketing_unit = :marketingUnit)"
            + " order by r.display_name";

    private final ScrReportSql sql;

    /**
     * Creates the report.
     *
     * @param sql report SQL
     */
    public PepClients(ScrReportSql sql) {
      this.sql = sql;
    }

    @Override
    public ReportMetadata metadata() {
      return ReportMetadata.compliance(
          PEP,
          "List of Approved PEP Clients",
          "Clients tagged PEP with the matched entry, case outcome and EDD (p.7)",
          base());
    }

    @Override
    public ReportResult generate(ReportParameters p) {
      return TabularReportBuilder.of(p)
          .columns(
              ReportColumn.text(CLIENT_CODE, "Client Code"),
              ReportColumn.text(NAME, "Client Name"),
              ReportColumn.date("pep_since", "PEP Since"),
              ReportColumn.text("matched_entry", "Matched Entry"),
              ReportColumn.text("case_outcome", "Case No. / Outcome"),
              ReportColumn.text("edd_required", "EDD Required"),
              ReportColumn.date("last_edd", "Last EDD"),
              ReportColumn.text(UNIT, "Marketing Unit / Account Officer"))
          .rows(sql.rows(SQL, args(p)))
          .presorted()
          .withoutGrandTotal()
          .note(ScrReportSql.LAYOUT_NOTE)
          .build();
    }
  }
}
