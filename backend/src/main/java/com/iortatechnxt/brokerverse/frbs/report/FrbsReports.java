package com.iortatechnxt.brokerverse.frbs.report;

import com.iortatechnxt.brokerverse.frbs.report.FrbsSqlReport.Dates;
import com.iortatechnxt.brokerverse.frbs.report.FrbsSqlReport.Spec;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * The broker reports of the BDOI report pack (FRBS 3.2.0, Appendix A IV-VI; report list #10, #15,
 * #16, #17, #169; design section 10): service fee (summary and detail), Mancom market performance,
 * branch production (detail and summary), expense grouping per cost centre, GAP, cash flow and the
 * sustainability production extract. Every one exports to Excel and PDF through the Report Centre.
 * Layouts are drafts to confirm with FRBS (AQ05).
 */
@Configuration(proxyBeanMethods = false)
public class FrbsReports {

  private static final String SEGMENT = "segment";
  private static final String SEGMENT_LABEL = "Segment";
  private static final String UNIT = "sales_unit";
  private static final String UNIT_LABEL = "Unit / BDO Branch";
  private static final String UNIT_NAME = "unit_name";
  private static final String CURRENCY = "currency";
  private static final String CURRENCY_LABEL = "Currency";
  private static final String PREMIUM = "premium";
  private static final String COMMISSION = "commission";
  private static final String GROSS_PREMIUM = "Gross Premium";
  private static final String COMMISSION_LABEL = "Commission";
  private static final String POLICIES = "policies";
  private static final String COUNT_LABEL = "Invoices";
  private static final String DRAFT = "Draft layout, to be confirmed with FRBS (AQ05).";
  private static final String PRODUCTION_FROM =
      " from ops_invoice i left join cat_sales_unit u"
          + " on u.company_id = i.company_id and u.code = i.sales_unit"
          + " where i.company_id = :companyId and not i.cancelled";

  /**
   * {@code FRBS-SERVICE-FEE}: service fee per market segment and unit, summary (FRBS 2.10.0,
   * Appendix A VI, report list #17).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition frbsServiceFee(NamedParameterJdbcTemplate jdbc) {
    return new FrbsSqlReport(
        new Spec(
            "FRBS-SERVICE-FEE",
            "Service Fee Report - Summary",
            "Service fee of the runs of the period per segment and unit, released and liquidated"
                + " (FRBS 2.10.0, Appendix A VI)",
            Dates.PERIOD,
            "select l.segment, r.run_no, r.stage, l.sales_unit, l.payee_name, l.currency,"
                + " l.invoice_count, l.commission, l.wtax, l.base, l.rate, l.fee, l.status,"
                + " l.released_on, l.liquidated_on"
                + " from frbs_service_fee_line l join frbs_service_fee_run r on r.id = l.run_id"
                + " where r.company_id = :companyId and r.stage <> 'CANCELLED'"
                + " and r.period_to between :from and :to"
                + " order by l.segment, r.run_no, l.line_no",
            List.of(
                ReportColumn.text(SEGMENT, SEGMENT_LABEL),
                ReportColumn.text("run_no", "Run"),
                ReportColumn.text("stage", "Run Stage"),
                ReportColumn.text(UNIT, UNIT_LABEL),
                ReportColumn.text("payee_name", "Recipient"),
                ReportColumn.text(CURRENCY, CURRENCY_LABEL),
                ReportColumn.count("invoice_count", COUNT_LABEL),
                ReportColumn.amount(COMMISSION, COMMISSION_LABEL),
                ReportColumn.amount("wtax", "Withholding Tax"),
                ReportColumn.amount("base", "Base"),
                ReportColumn.percent("rate", "Rate"),
                ReportColumn.amount("fee", "Service Fee"),
                ReportColumn.text("status", "Status"),
                ReportColumn.date("released_on", "Released"),
                ReportColumn.date("liquidated_on", "Liquidated")),
            SEGMENT,
            SEGMENT_LABEL,
            List.of("Runs whose period ends in the report period; cancelled runs excluded.")),
        jdbc);
  }

  /**
   * {@code FRBS-SERVICE-FEE-DETAIL}: the invoices of the service fee (FRBS 2.10.0, Appendix A VI).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition frbsServiceFeeDetail(NamedParameterJdbcTemplate jdbc) {
    return new FrbsSqlReport(
        new Spec(
            "FRBS-SERVICE-FEE-DETAIL",
            "Service Fee Report - Details",
            "Fully paid invoices behind the service fee of the period, per segment (FRBS 2.10.0,"
                + " Appendix A VI)",
            Dates.PERIOD,
            "select l.segment, r.run_no, l.sales_unit, f.invoice_no, f.client_code, f.assured_name,"
                + " f.insurer_code, f.paid_on, l.currency, f.commission, f.wtax, f.base, l.rate,"
                + " f.fee"
                + " from frbs_service_fee_item f join frbs_service_fee_line l on l.id = f.line_id"
                + " join frbs_service_fee_run r on r.id = f.run_id"
                + " where r.company_id = :companyId and f.live and r.period_to between :from and :to"
                + " order by l.segment, r.run_no, l.sales_unit, f.invoice_no",
            List.of(
                ReportColumn.text(SEGMENT, SEGMENT_LABEL),
                ReportColumn.text("run_no", "Run"),
                ReportColumn.text(UNIT, UNIT_LABEL),
                ReportColumn.text("invoice_no", "Invoice"),
                ReportColumn.text("client_code", "Client"),
                ReportColumn.text("assured_name", "Assured"),
                ReportColumn.text("insurer_code", "Insurer"),
                ReportColumn.date("paid_on", "Fully Paid"),
                ReportColumn.text(CURRENCY, CURRENCY_LABEL),
                ReportColumn.amount(COMMISSION, COMMISSION_LABEL),
                ReportColumn.amount("wtax", "Withholding Tax"),
                ReportColumn.amount("base", "Base"),
                ReportColumn.percent("rate", "Rate"),
                ReportColumn.amount("fee", "Service Fee")),
            SEGMENT,
            SEGMENT_LABEL,
            List.of()),
        jdbc);
  }

  /**
   * {@code FRBS-MANCOM-MARKET}: market performance summary, premium and commission per segment and
   * location for the month, year to date and the previous year to date (Appendix A V, list #15).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition frbsMancomMarket(NamedParameterJdbcTemplate jdbc) {
    return new FrbsSqlReport(
            new Spec(
                "FRBS-MANCOM-MARKET",
                "Market Performance Summary - Premium and Commission",
                "Booked premium and commission per segment and location: month, year to date and"
                    + " previous year to date with growth (FRBS 3.2.0, Appendix A V)",
                Dates.AS_OF,
                "select d.*, case when d.comm_prev = 0 then null"
                    + " else round((d.comm_ytd - d.comm_prev) * 100 / d.comm_prev, 2) end growth"
                    + " from (select coalesce(i.segment, '(none)') segment, b.name location, i.currency,"
                    + " coalesce(sum(i.gross_premium) filter (where i.booking_date >= :monthStart), 0)"
                    + " prem_month,"
                    + " coalesce(sum(i.commission) filter (where i.booking_date >= :monthStart), 0)"
                    + " comm_month,"
                    + " coalesce(sum(i.gross_premium) filter (where i.booking_date >= :yearStart), 0)"
                    + " prem_ytd,"
                    + " coalesce(sum(i.commission) filter (where i.booking_date >= :yearStart), 0)"
                    + " comm_ytd,"
                    + " coalesce(sum(i.gross_premium) filter (where i.booking_date <= :prevTo), 0)"
                    + " prem_prev,"
                    + " coalesce(sum(i.commission) filter (where i.booking_date <= :prevTo), 0)"
                    + " comm_prev"
                    + " from ops_invoice i join org_branch b on b.id = i.branch_id"
                    + " where i.company_id = :companyId and not i.cancelled"
                    + " and ((i.booking_date between :yearStart and :to)"
                    + " or (i.booking_date between :prevYearStart and :prevTo))"
                    + " group by coalesce(i.segment, '(none)'), b.name, i.currency) d"
                    + " order by d.segment, d.location, d.currency",
                List.of(
                    ReportColumn.text(SEGMENT, SEGMENT_LABEL),
                    ReportColumn.text("location", "Location"),
                    ReportColumn.text(CURRENCY, CURRENCY_LABEL),
                    ReportColumn.amount("prem_month", "Premium - Month"),
                    ReportColumn.amount("comm_month", "Commission - Month"),
                    ReportColumn.amount("prem_ytd", "Premium - YTD"),
                    ReportColumn.amount("comm_ytd", "Commission - YTD"),
                    ReportColumn.amount("prem_prev", "Premium - Previous YTD"),
                    ReportColumn.amount("comm_prev", "Commission - Previous YTD"),
                    ReportColumn.percent("growth", "Commission Growth")),
                SEGMENT,
                SEGMENT_LABEL,
                List.of(
                    "Budget columns wait for the segment budgets (AQ05); amounts in invoice currency.",
                    DRAFT)),
            jdbc)
        .asDocument();
  }

  /**
   * {@code FRBS-BRANCH-PRODUCTION}: production per BDO branch / unit, invoice detail (Appendix A V,
   * list #16).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition frbsBranchProduction(NamedParameterJdbcTemplate jdbc) {
    return new FrbsSqlReport(
        new Spec(
            "FRBS-BRANCH-PRODUCTION",
            "Branch Production Report - Detailed",
            "Booked invoices of the period per BDO branch / unit (FRBS 3.2.0, Appendix A V)",
            Dates.PERIOD,
            "select coalesce(i.sales_unit, '(none)') || ' ' || coalesce(u.name, '') unit,"
                + " i.booking_date, i.invoice_no, i.policy_no, i.assured_name, i.product_line,"
                + " i.insurer_code, i.segment, i.currency, i.gross_premium premium, i.commission"
                + PRODUCTION_FROM
                + " and i.booking_date between :from and :to"
                + " order by unit, i.booking_date, i.invoice_no",
            List.of(
                ReportColumn.text("unit", UNIT_LABEL),
                ReportColumn.date("booking_date", "Booked"),
                ReportColumn.text("invoice_no", "Invoice"),
                ReportColumn.text("policy_no", "Policy"),
                ReportColumn.text("assured_name", "Assured"),
                ReportColumn.text("product_line", "Line"),
                ReportColumn.text("insurer_code", "Insurer"),
                ReportColumn.text(SEGMENT, SEGMENT_LABEL),
                ReportColumn.text(CURRENCY, CURRENCY_LABEL),
                ReportColumn.amount(PREMIUM, GROSS_PREMIUM),
                ReportColumn.amount(COMMISSION, COMMISSION_LABEL)),
            "unit",
            UNIT_LABEL,
            List.of(
                "Units are the sales units of the booking; BDO branch codes to confirm (AQ05).")),
        jdbc);
  }

  /**
   * {@code FRBS-BRANCH-PRODUCTION-SUM}: production per BDO branch / unit, summary (Appendix A V,
   * list #16).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition frbsBranchProductionSummary(NamedParameterJdbcTemplate jdbc) {
    return new FrbsSqlReport(
        new Spec(
            "FRBS-BRANCH-PRODUCTION-SUM",
            "Branch Production Report - Summary",
            "Invoices, premium and commission of the period per BDO branch / unit (FRBS 3.2.0,"
                + " Appendix A V)",
            Dates.PERIOD,
            "select coalesce(i.sales_unit, '(none)') sales_unit, coalesce(u.name, '') unit_name,"
                + " i.currency, count(*) policies, sum(i.gross_premium) premium,"
                + " sum(i.commission) commission"
                + PRODUCTION_FROM
                + " and i.booking_date between :from and :to"
                + " group by coalesce(i.sales_unit, '(none)'), coalesce(u.name, ''), i.currency"
                + " order by sales_unit, i.currency",
            List.of(
                ReportColumn.text(UNIT, UNIT_LABEL),
                ReportColumn.text(UNIT_NAME, "Name"),
                ReportColumn.text(CURRENCY, CURRENCY_LABEL),
                ReportColumn.count(POLICIES, COUNT_LABEL),
                ReportColumn.amount(PREMIUM, GROSS_PREMIUM),
                ReportColumn.amount(COMMISSION, COMMISSION_LABEL)),
            null,
            null,
            List.of()),
        jdbc);
  }

  /**
   * {@code FRBS-EXPENSE-GROUPING}: expenses of the period per cost centre and account (Appendix A
   * IV expense grouping, list #10).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition frbsExpenseGrouping(NamedParameterJdbcTemplate jdbc) {
    return new FrbsSqlReport(
        new Spec(
            "FRBS-EXPENSE-GROUPING",
            "Expense Allocation per Cost Centre",
            "Posted expenses of the period per cost centre and account (FRBS 3.2.0, Appendix A IV)",
            Dates.PERIOD,
            "select coalesce(e.cost_center, '(none)') || coalesce(' ' || d.name, '') cost_center,"
                + " a.code account, a.name account_name,"
                + " sum(e.debit_base - e.credit_base) amount"
                + " from gl_ledger_entry e join coa_account a on a.id = e.account_id"
                + " left join dim_value d on d.company_id = e.company_id"
                + " and d.dimension_type = 'COST_CENTER' and d.code = e.cost_center"
                + " where e.company_id = :companyId and a.account_class = 'EXPENSE'"
                + " and e.value_date between :from and :to"
                + " group by 1, a.code, a.name having sum(e.debit_base - e.credit_base) <> 0"
                + " order by 1, a.code",
            List.of(
                ReportColumn.text("cost_center", "Cost Centre"),
                ReportColumn.text("account", "Account"),
                ReportColumn.text("account_name", "Account Name"),
                ReportColumn.amount("amount", "Expense")),
            "cost_center",
            "Cost Centre",
            List.of("Amounts in base currency. " + DRAFT)),
        jdbc);
  }

  /**
   * {@code FRBS-GAP}: receivables and payables still open by time to maturity, with the gap and the
   * cumulative gap (Appendix A IV GAP report).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition frbsGap(NamedParameterJdbcTemplate jdbc) {
    return new FrbsSqlReport(
        new Spec(
            "FRBS-GAP",
            "GAP Report",
            "Open receivables and payables of the sub-ledger by time to maturity, gap and"
                + " cumulative gap (FRBS 3.2.0, Appendix A IV)",
            Dates.AS_OF,
            "select g.bucket, g.assets, g.liabilities, g.assets - g.liabilities gap,"
                + " sum(g.assets - g.liabilities) over (order by g.bucket) cumulative"
                + " from (select case when o.due_date <= :to then '0. Past due'"
                + " when o.due_date <= cast(:to as date) + 30 then '1. 1 - 30 days'"
                + " when o.due_date <= cast(:to as date) + 90 then '2. 31 - 90 days'"
                + " when o.due_date <= cast(:to as date) + 180 then '3. 91 - 180 days'"
                + " when o.due_date <= cast(:to as date) + 365 then '4. 181 - 365 days'"
                + " else '5. Over one year' end bucket,"
                + " coalesce(sum(o.base_amount * (o.amount - o.settled_amount) / o.amount)"
                + " filter (where o.direction = 'DEBIT'), 0) assets,"
                + " coalesce(sum(o.base_amount * (o.amount - o.settled_amount) / o.amount)"
                + " filter (where o.direction = 'CREDIT'), 0) liabilities"
                + " from sl_open_item o where o.company_id = :companyId"
                + " and o.status in ('OPEN', 'PARTIALLY_SETTLED') and o.document_date <= :to"
                + " group by 1) g order by g.bucket",
            List.of(
                ReportColumn.text("bucket", "Time to Maturity"),
                ReportColumn.amount("assets", "Receivables"),
                ReportColumn.amount("liabilities", "Payables"),
                ReportColumn.amount("gap", "Gap"),
                ReportColumn.amountNoTotal("cumulative", "Cumulative Gap")),
            null,
            null,
            List.of("Open sub-ledger items in base currency by due date. " + DRAFT)),
        jdbc);
  }

  /**
   * {@code FRBS-CASH-FLOW}: cash and bank opening balance, receipts and payments per journal type
   * and closing balance (Appendix A IV cash flow).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition frbsCashFlow(NamedParameterJdbcTemplate jdbc) {
    String cash =
        " from gl_ledger_entry e join coa_account a on a.id = e.account_id"
            + " join coa_category c on c.id = a.category_id"
            + " where e.company_id = :companyId and c.bank_category";
    return new FrbsSqlReport(
        new Spec(
            "FRBS-CASH-FLOW",
            "Cash Flow Report",
            "Cash and bank: opening balance, receipts and payments per source and closing balance"
                + " (FRBS 3.2.0, Appendix A IV)",
            Dates.PERIOD,
            "select 0 seq, 'Opening cash and bank balance' line, null receipts, null payments,"
                + " coalesce(sum(e.debit_base - e.credit_base), 0) net"
                + cash
                + " and e.value_date < :from"
                + " union all select 1, 'Movements - ' || e.journal_type, sum(e.debit_base),"
                + " sum(e.credit_base), sum(e.debit_base - e.credit_base)"
                + cash
                + " and e.value_date between :from and :to group by e.journal_type"
                + " union all select 2, 'Closing cash and bank balance', null, null,"
                + " coalesce(sum(e.debit_base - e.credit_base), 0)"
                + cash
                + " and e.value_date <= :to"
                + " order by 1, 2",
            List.of(
                ReportColumn.text("line", "Line"),
                ReportColumn.amount("receipts", "Receipts"),
                ReportColumn.amount("payments", "Payments"),
                ReportColumn.amountNoTotal("net", "Net / Balance")),
            null,
            null,
            List.of("Accounts of the bank and cash categories, base currency. " + DRAFT)),
        jdbc);
  }

  /**
   * {@code FRBS-SUSTAINABILITY-PROD}: yearly production per client type, region and line of
   * business for the sustainability report - risk management (report list #169).
   *
   * @param jdbc JDBC
   * @return report
   */
  @Bean
  public ReportDefinition frbsSustainability(NamedParameterJdbcTemplate jdbc) {
    return new FrbsSqlReport(
        new Spec(
            "FRBS-SUSTAINABILITY-PROD",
            "Sustainability Report - Risk Management (Production)",
            "Booked production of the year per client type, region and line of business (report"
                + " list #169)",
            Dates.YEAR,
            "select coalesce(c.client_type, 'UNKNOWN') client_type,"
                + " coalesce(b.region, '(none)') region, coalesce(i.product_line, '(none)') line,"
                + " i.currency, count(distinct i.client_code) clients, count(*) policies,"
                + " sum(i.gross_premium) premium, sum(i.commission) commission"
                + " from ops_invoice i join org_branch b on b.id = i.branch_id"
                + " left join crm_client c on c.company_id = i.company_id"
                + " and c.client_code = i.client_code"
                + " where i.company_id = :companyId and not i.cancelled"
                + " and i.booking_date between :from and :to"
                + " group by 1, 2, 3, i.currency order by 1, 2, 3, i.currency",
            List.of(
                ReportColumn.text("client_type", "Client Type"),
                ReportColumn.text("region", "Region"),
                ReportColumn.text("line", "Line of Business"),
                ReportColumn.text(CURRENCY, CURRENCY_LABEL),
                ReportColumn.count("clients", "Clients"),
                ReportColumn.count(POLICIES, COUNT_LABEL),
                ReportColumn.amount(PREMIUM, GROSS_PREMIUM),
                ReportColumn.amount(COMMISSION, COMMISSION_LABEL)),
            "client_type",
            "Client Type",
            List.of("Yearly extract, first Monday of January (report list #169). " + DRAFT)),
        jdbc);
  }
}
