package com.iortatechnxt.brokerverse.renewal.dashboard.service;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.nbreport.service.SqlArgs;
import com.iortatechnxt.brokerverse.renewal.report.RenewalReportSupport;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Reads the accounts of the Renewal dashboard (FRRN.002.02) within the user's data scope and the
 * filters: the renewal accounts with their renewal account, latest insurer disposition, return,
 * adjustments and delivery; and the New Business accounts of the pipeline and the closing ratio.
 */
@Component
public class DashboardItems {

  /** Commission of a renewal: that of its renewal account, else the expiring premium times rate. */
  static final String COMMISSION =
      "coalesce(a.commission, round(coalesce(c.basic_premium, 0) * coalesce(c.commission_rate, 0)"
          + " / 100, 2))";

  private static final String RENEWALS =
      "select 'RENEWAL' as kind, c.id, c.renewal_ref as ref, c.renewal_arn as arn, c.expiring_arn,"
          + " c.expiring_invoice_no, coalesce(c.renewed_invoice_no, c.new_invoice_no) as invoice_no,"
          + " coalesce(c.assured_name, c.client_name) as assured, c.client_name, c.line_code,"
          + " c.product_code, c.segment, c.owner_unit as team, c.insurer_code, c.expiry_date,"
          + " c.inception_date, c.stage, c.disposition, c.nonrenewal_reason, c.returned, c.closed_as,"
          + " c.closed_at, c.hold_cover_status, c.hold_cover_until, c.claims_flag, c.bucket,"
          + " c.total_sum_insured as sum_insured, c.premium_rate, c.commission_rate, c.mortgaged,"
          + " coalesce(c.assigned_po, c.assigned_ao) as assigned_user, c.assigned_ao, c.assigned_po,"
          + " c.unit_head, c.branch_code, c.account_officer, c.marketing_locked_at, c.kyc_due,"
          + " coalesce(a.net_premium, c.basic_premium, 0) as premium, "
          + COMMISSION
          + " as commission, a.status as account_status, a.booked_at, a.cancelled_at,"
          + " a.cancellation_reason, a.net_premium as booked_premium, a.commission as booked_commission,"
          + " r.response as insurer_response, r.remarks as insurer_remarks, r.received_on as insurer_on,"
          + " ret.occurred_at as returned_at, ret.reason_code as return_reason, ret.comment as return_remarks,"
          + " adj.classes as adjustment_types, adj.basic as adjustment_basic, adj.comm as adjustment_commission,"
          + " exists (select 1 from iss_epolicy e where e.arn = c.renewal_arn and e.dispatched_at is not null)"
          + " as delivered, "
          + labels("c.line_code", "c.insurer_code", "c.segment", "c.company_id")
          + ", (select v.label from lov_value v where v.type_code = 'RNW_RETURN_REASON'"
          + " and v.code = ret.reason_code limit 1) as return_reason_name"
          + " from rnw_candidate c"
          + " left join acc_account a on a.arn = c.renewal_arn and a.company_id = c.company_id"
          + " left join lateral (select x.response, x.remarks, x.received_on from rnw_insurer_response x"
          + " where x.candidate_id = c.id and x.latest_valid order by x.id desc limit 1) r on true"
          + " left join lateral (select h.occurred_at, h.reason_code, h.comment from wf_case w"
          + " join wf_case_history h on h.case_id = w.id where w.entity_type = 'RenewalCandidate'"
          + " and w.entity_id = cast(c.id as varchar) and h.action like 'return%'"
          + " order by h.id desc limit 1) ret on true"
          + " left join lateral (select string_agg(distinct q.request_class, ',') as classes,"
          + " sum(coalesce(q.in_basic, 0)) as basic, sum(coalesce(q.in_commission, 0)) as comm"
          + " from adj_request q where q.company_id = c.company_id and q.posted_at is not null"
          + " and q.arn in (c.renewal_arn, c.expiring_arn)) adj on true"
          + " where (c.expiry_date between :wfrom and :wto or c.stage not in ('RENEWED', 'CLOSED')"
          + " or a.booked_at between :wfrom and :wto)"
          + " and (cast(:seg as varchar) is null or c.segment = :seg)"
          + " and (cast(:ao as varchar) is null or c.assigned_ao = :ao or c.account_officer = :ao)"
          + RenewalReportSupport.FILTERS;

  private static final String NEW_BUSINESS =
      "select 'NEW_BUSINESS' as kind, a.id, a.arn as ref, a.arn, a.client_name as assured,"
          + " a.client_name, a.line_code, a.product_code, a.market_segment as segment,"
          + " a.sales_team as team, a.insurer_code, a.period_from as inception_date,"
          + " a.period_to as expiry_date, a.status as stage, a.origin, a.account_officer,"
          + " a.account_officer as assigned_user, coalesce(a.net_premium, 0) as premium,"
          + " coalesce(a.commission, 0) as commission, a.booked_at,"
          + " exists (select 1 from acc_account p where p.company_id = a.company_id"
          + " and p.client_id = a.client_id and p.id <> a.id and p.status = 'BOOKED'"
          + " and p.booked_at < coalesce(a.period_from, p.booked_at + 1)) as existing_client,"
          + " exists (select 1 from iss_epolicy e where e.account_id = a.id and e.dispatched_at is not null)"
          + " as delivered, "
          + labels("a.line_code", "a.insurer_code", "a.market_segment", "a.company_id")
          + " from acc_account a where a.company_id = :company and a.business_type = 'NEW_BUSINESS'"
          + " and a.period_from between :pfrom and :pto"
          + " and (cast(:seg as varchar) is null or a.market_segment = :seg)"
          + " and (cast(:ao as varchar) is null or a.account_officer = :ao)"
          + " and (cast(:scope_all as boolean) or a.sales_team = any(cast(:scope_units as varchar[]))"
          + " or a.account_officer = :scope_user)";

  private static final int HISTORY_MONTHS = 13;

  /**
   * The names of the product line, the insurer and the market segment of a row.
   *
   * @param line line column
   * @param insurer insurer column
   * @param segment segment column
   * @param company company column
   * @return select items line_name, insurer_name, segment_name
   */
  static String labels(String line, String insurer, String segment, String company) {
    return "(select pl.name from cat_product_line pl where pl.code = "
        + line
        + ") as line_name, (select ci.name from cat_insurer ci where ci.company_id = "
        + company
        + " and ci.party_code = "
        + insurer
        + " limit 1) as insurer_name, (select sv.label from lov_value sv"
        + " where sv.type_code = 'MARKET_SEGMENT' and sv.code = "
        + segment
        + " limit 1) as segment_name";
  }

  private static final int AHEAD_MONTHS = 3;

  private final NbReportJdbc jdbc;
  private final RenewalReportSupport support;

  /**
   * Creates the reader.
   *
   * @param jdbc SQL
   * @param support data scope of the user
   */
  public DashboardItems(NbReportJdbc jdbc, RenewalReportSupport support) {
    this.jdbc = jdbc;
    this.support = support;
  }

  /**
   * The renewal accounts of the scope and filters: those expiring or booked in a window around the
   * period (the persistency months and the year to date included) and every open one.
   *
   * @param f filters with the period
   * @param today business date
   * @return items
   */
  public List<DashboardItem> renewals(DashboardFilter f, LocalDate today) {
    LocalDate start = f.from().minusMonths(HISTORY_MONTHS).withDayOfMonth(1);
    LocalDate end = f.to().isAfter(today) ? f.to() : today.plusMonths(AHEAD_MONTHS);
    Map<String, Object> args =
        args(f).with("wfrom", start).with("wto", end.plusMonths(AHEAD_MONTHS)).map();
    return jdbc.rows(RENEWALS, args).stream().map(DashboardItem::new).toList();
  }

  /**
   * The New Business accounts of the scope and filters whose inception is in the period.
   *
   * @param f filters with the period
   * @return items
   */
  public List<DashboardItem> newBusiness(DashboardFilter f) {
    Map<String, Object> args = args(f).with("pfrom", f.from()).with("pto", f.to()).map();
    return jdbc.rows(NEW_BUSINESS, args).stream().map(DashboardItem::new).toList();
  }

  private SqlArgs args(DashboardFilter f) {
    return support.allOf(f.companyId()).with("seg", f.segment()).with("ao", f.officer());
  }
}
