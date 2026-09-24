package com.iortatechnxt.brokerverse.nbreport.service;

import com.iortatechnxt.brokerverse.nbreport.domain.UnitLevel;
import com.iortatechnxt.brokerverse.nbreport.service.NbDashboard.Booked;
import com.iortatechnxt.brokerverse.nbreport.service.NbDashboard.StageAgeing;
import com.iortatechnxt.brokerverse.nbreport.service.NbDashboard.StatusCount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the New Business dashboard (BRNB.012): requests and quotations by status, quotations sent,
 * accounts by stage, SLA breaches, bookings of the month, the quotation-to-booking funnel, the
 * ageing of open accounts by stage and production against target per team. Every figure is an SQL
 * aggregate over the broking tables.
 */
@Service
@Transactional(readOnly = true)
public class NbDashboardService {

  private static final String COUNT = "n";
  private static final String CODE = "code";
  private static final String LABEL = "label";
  private static final String GROUP = "grp";
  private static final String NOW = "now";
  private static final String FROM = "from";
  private static final String TO = "to";
  private static final String SENT = "SENT_TO_CLIENT";

  private static final String REQUESTS =
      "select 'REQUEST' as grp, r.status as code, initcap(r.status) as label, 0 as sort,"
          + " count(*) as n from quo_request r where r.company_id = :company group by r.status"
          + " union all select 'QUOTATION', q.status, min(s.name), min(s.sort_order), count(*)"
          + " from quo_quotation q join wf_stage s on s.workflow_code = 'NB_QUOTATION'"
          + " and s.stage_code = q.status where q.company_id = :company and not s.terminal"
          + " group by q.status"
          + " union all select 'PROPOSAL', p.status, min(s.name), min(s.sort_order), count(*)"
          + " from npk_proposal p join wf_stage s on s.workflow_code = 'NB_PROPOSAL'"
          + " and s.stage_code = p.status where p.company_id = :company and not s.terminal"
          + " group by p.status"
          + " order by 1 desc, 4, 2";

  private static final String SENT_IN_PERIOD =
      "select count(*) from wf_case_history h join wf_case c on c.id = h.case_id"
          + " where c.company_id = :company and c.workflow_code in ('NB_QUOTATION', 'NB_PROPOSAL')"
          + " and h.to_stage = 'SENT_TO_CLIENT'"
          + " and cast(h.occurred_at at time zone 'Asia/Manila' as date) between :from and :to";

  private static final String AWAITING_CLIENT =
      "select (select count(*) from quo_quotation where company_id = :company and status = :sent)"
          + " + (select count(*) from npk_proposal where company_id = :company and status = :sent)";

  private static final String ACCOUNTS =
      "select 'NB_ACCOUNT' as grp, a.status as code, min(s.name) as label, count(*) as n"
          + " from acc_account a join wf_stage s on s.workflow_code = 'NB_ACCOUNT'"
          + " and s.stage_code = a.status where a.company_id = :company and a.status <> 'VOIDED'"
          + " group by a.status order by min(s.sort_order)";

  private static final String OVERDUE =
      "select c.workflow_code as grp, c.workflow_code as code, c.workflow_code as label,"
          + " count(*) as n from wf_case c join wf_stage s on s.workflow_code = c.workflow_code"
          + " and s.stage_code = c.stage_code where c.company_id = :company and not c.closed"
          + " and not s.terminal and s.owner_permission is not null"
          + " and c.due_at < cast(:now as timestamptz)"
          + " group by c.workflow_code order by c.workflow_code";

  private static final String BOOKED =
      "select count(*) filter (where kind = 'BOOKING') as n,"
          + " coalesce(sum(basic_premium), 0) as premium, coalesce(sum(commission), 0) as commission"
          + " from bkg_invoice where company_id = :company and status = 'BOOKED'"
          + " and booking_date between :from and :to";

  private static final String CREATED = " and cast(created_at at time zone 'Asia/Manila' as date)";

  private static final String FUNNEL =
      "with q as (select status from quo_quotation where company_id = :company"
          + CREATED
          + " between :from and :to and status <> 'VOIDED'"
          + " union all select status from npk_proposal where company_id = :company"
          + CREATED
          + " between :from and :to and status <> 'VOIDED'),"
          + " a as (select status from acc_account where company_id = :company"
          + CREATED
          + " between :from and :to and status <> 'VOIDED')"
          + " select (select count(*) from q) as quoted,"
          + " (select count(*) from q where status in"
          + " ('SENT_TO_CLIENT', 'ACCEPTED', 'CONVERTED', 'NOT_PROCEEDED')) as sent,"
          + " (select count(*) from q where status in ('ACCEPTED', 'CONVERTED')) as accepted,"
          + " (select count(*) from a) as accounts,"
          + " (select count(*) from a where status in"
          + " ('PLACED', 'POLICY_ISSUED', 'BOOKED', 'CANCELLED')) as placed,"
          + " (select count(*) from a where status in"
          + " ('POLICY_ISSUED', 'BOOKED', 'CANCELLED')) as issued,"
          + " (select count(*) from a where status in ('BOOKED', 'CANCELLED')) as booked";

  private static final String AGEING =
      "select c.stage_code as stage, min(s.name) as label,"
          + " count(*) filter (where c.stage_entered_at > cast(:now as timestamptz)"
          + " - interval '1 day') as d1,"
          + " count(*) filter (where c.stage_entered_at <= cast(:now as timestamptz)"
          + " - interval '1 day' and c.stage_entered_at > cast(:now as timestamptz)"
          + " - interval '3 days') as d3,"
          + " count(*) filter (where c.stage_entered_at <= cast(:now as timestamptz)"
          + " - interval '3 days' and c.stage_entered_at > cast(:now as timestamptz)"
          + " - interval '7 days') as d7,"
          + " count(*) filter (where c.stage_entered_at <= cast(:now as timestamptz)"
          + " - interval '7 days') as older,"
          + " count(*) filter (where c.due_at < cast(:now as timestamptz)) as overdue"
          + " from wf_case c join wf_stage s on s.workflow_code = c.workflow_code"
          + " and s.stage_code = c.stage_code"
          + " where c.company_id = :company and c.workflow_code = 'NB_ACCOUNT' and not c.closed"
          + " and not s.terminal and s.owner_permission is not null"
          + " group by c.stage_code order by min(s.sort_order)";

  private static final List<String[]> FUNNEL_STEPS =
      List.of(
          new String[] {"quoted", "Quotations & PRFs"},
          new String[] {"sent", "Sent to client"},
          new String[] {"accepted", "Accepted"},
          new String[] {"accounts", "Accounts created"},
          new String[] {"placed", "Placed"},
          new String[] {"issued", "Policy issued"},
          new String[] {"booked", "Booked"});

  private static final Map<String, String> WORKFLOWS =
      Map.of(
          "NB_QUOTATION", "Quotations",
          "NB_PROPOSAL", "Proposal requests",
          "NB_ACCOUNT", "Accounts",
          "NB_CLIENT", "Client onboarding");

  private final NbReportJdbc jdbc;
  private final ProductionService production;
  private final StallRule time;

  /**
   * Creates the service.
   *
   * @param jdbc report SQL
   * @param production production statistics
   * @param time the current time
   */
  public NbDashboardService(NbReportJdbc jdbc, ProductionService production, StallRule time) {
    this.jdbc = jdbc;
    this.production = production;
    this.time = time;
  }

  /**
   * The dashboard of a company.
   *
   * @param companyId company
   * @param asOf date of the figures
   * @return dashboard
   */
  public NbDashboard dashboard(long companyId, LocalDate asOf) {
    LocalDate monthStart = asOf.withDayOfMonth(1);
    Map<String, Object> month =
        SqlArgs.company(companyId).with(FROM, monthStart).with(TO, asOf).map();
    Map<String, Object> company =
        SqlArgs.company(companyId).with(NOW, time.now()).with("sent", SENT).map();
    return new NbDashboard(
        asOf,
        counts(REQUESTS, company),
        jdbc.count(SENT_IN_PERIOD, month),
        jdbc.count(AWAITING_CLIENT, company),
        counts(ACCOUNTS, company),
        counts(OVERDUE, company).stream()
            .map(
                c ->
                    new StatusCount(
                        c.group(), c.code(), WORKFLOWS.getOrDefault(c.code(), c.code()), c.count()))
            .toList(),
        booked(month),
        funnel(companyId, asOf),
        ageing(company),
        production.production(companyId, UnitLevel.TEAM, monthStart, asOf));
  }

  private List<StatusCount> counts(String sql, Map<String, Object> args) {
    return jdbc.rows(sql, args).stream()
        .map(
            r ->
                new StatusCount(
                    (String) r.get(GROUP),
                    (String) r.get(CODE),
                    (String) r.get(LABEL),
                    ((Number) r.get(COUNT)).longValue()))
        .toList();
  }

  private Booked booked(Map<String, Object> month) {
    Map<String, Object> r = jdbc.rows(BOOKED, month).get(0);
    return new Booked(
        ((Number) r.get(COUNT)).longValue(),
        (BigDecimal) r.get("premium"),
        (BigDecimal) r.get("commission"));
  }

  private List<StatusCount> funnel(long companyId, LocalDate asOf) {
    Map<String, Object> year =
        SqlArgs.company(companyId).with(FROM, asOf.withDayOfYear(1)).with(TO, asOf).map();
    Map<String, Object> r = jdbc.rows(FUNNEL, year).get(0);
    return FUNNEL_STEPS.stream()
        .map(s -> new StatusCount("FUNNEL", s[0], s[1], ((Number) r.get(s[0])).longValue()))
        .toList();
  }

  private List<StageAgeing> ageing(Map<String, Object> args) {
    return jdbc.rows(AGEING, args).stream()
        .map(
            r ->
                new StageAgeing(
                    (String) r.get("stage"),
                    (String) r.get(LABEL),
                    number(r, "d1"),
                    number(r, "d3"),
                    number(r, "d7"),
                    number(r, "older"),
                    number(r, "overdue")))
        .toList();
  }

  private static long number(Map<String, Object> row, String key) {
    return ((Number) row.get(key)).longValue();
  }
}
