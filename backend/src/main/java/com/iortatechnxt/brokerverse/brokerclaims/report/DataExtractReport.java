package com.iortatechnxt.brokerverse.brokerclaims.report;

import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Claims Data Extract (BRCLM.033, FR-CL-064): a flat extract with every claim field, one row per
 * claim, insurer line and location (a claim with two insurers and three locations gives six rows),
 * for analysis outside BIBS. Viewed and exported with {@code BCL_DATA_EXTRACT} only; saved report
 * variants keep the parameter sets.
 */
@Component
public class DataExtractReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "BCL-DATA-EXTRACT";

  private static final String SQL =
      "select c.claim_no, c.source, c.legacy_ref, c.handler, c.unit_code, c.branch_id, c.arn,"
          + " c.policy_year, c.policy_no, c.cover_version_no, c.cover_version_ref, c.product_code,"
          + " c.line_code, c.client_code, c.assured_name, c.lead_insurer_code, c.period_from,"
          + " c.period_to, c.sum_insured, c.sales_region, c.sales_department, c.sales_team,"
          + " c.account_officer, c.cost_center, c.currency, c.premium_status,"
          + " c.authorization_code, c.loss_date, c.reported_date, c.loss_nature, c.claim_type,"
          + " c.loss_description, c.loss_place, c.catastrophe_code, c.catastrophe_event,"
          + " c.claim_amount, c.deductible, c.initial_reserve, c.claimant_name,"
          + " c.claimant_overridden, c.status_code, c.phase, c.closure_kind,"
          + " c.settlement_type_code, c.settlement_amount, c.date_settled, c.closed_on,"
          + " c.adjuster_code, c.next_follow_up_date, c.next_action_plan,"
          + " ic.insurer_code, ic.share_pct, ic.insurer_claim_no, ic.reported_to_insurer_on,"
          + " ic.reserve_amount, ic.settled_amount, ic.adjuster_code as line_adjuster,"
          + " cl.account_item_no, cl.address, cl.city, cl.province, cl.location_key"
          + " from bcl_claim c left join bcl_insurer_claim ic on ic.claim_id = c.id"
          + " left join bcl_claim_location cl on cl.claim_id = c.id"
          + " where c.company_id = :companyId"
          + " and (cast(:reportedFrom as date) is null or c.reported_date >= :reportedFrom)"
          + " and (cast(:reportedTo as date) is null or c.reported_date <= :reportedTo)"
          + BclReportSql.FILTERS
          + " order by c.claim_no, ic.id, cl.account_item_no";

  private final BclReportSql sql;

  /**
   * Creates the report.
   *
   * @param sql report SQL
   */
  public DataExtractReport(BclReportSql sql) {
    this.sql = sql;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        CODE,
        "Claims Data Extract",
        ReportCategory.CLAIMS_HANDLING,
        "Every claim field, one row per claim, insurer line and location (BRCLM.033)",
        BclReportSql.rangeFilters("reportedFrom", "reportedTo", "Date reported", false),
        Permission.BCL_DATA_EXTRACT,
        Permission.BCL_DATA_EXTRACT,
        true);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return TabularReportBuilder.of(p)
        .columns(columns())
        .rows(sql.rows(SQL, BclReportSql.args(p)))
        .presorted()
        .withoutGrandTotal()
        .build();
  }

  private static List<ReportColumn> columns() {
    List<ReportColumn> columns = new ArrayList<>(claimColumns());
    columns.addAll(lossColumns());
    return columns;
  }

  private static List<ReportColumn> claimColumns() {
    return List.of(
        text("claim_no", "Claim Number"),
        text("source", "Source"),
        text("legacy_ref", "Legacy Reference"),
        text("handler", "Claim Handler"),
        text("unit_code", "Claims Unit"),
        number("branch_id", "Branch Id"),
        text("arn", "ARN"),
        number("policy_year", "Policy Year"),
        text("policy_no", "Policy No."),
        number("cover_version_no", "Cover Version"),
        text("cover_version_ref", "Cover Version Ref."),
        text("product_code", "Product"),
        text("line_code", "Product Line"),
        text("client_code", "Client Code"),
        text("assured_name", "Assured"),
        text("lead_insurer_code", "Lead Insurer"),
        ReportColumn.date("period_from", "Period From"),
        ReportColumn.date("period_to", "Period To"),
        ReportColumn.amountNoTotal("sum_insured", "Sum Insured"),
        text("sales_region", "Sales Region"),
        text("sales_department", "Sales Department"),
        text("sales_team", "Marketing Team"),
        text("account_officer", "Account Officer"),
        text("cost_center", "Cost Center"),
        text("currency", "Currency"),
        text("premium_status", "Premium Status"),
        text("authorization_code", "Authorization Code"));
  }

  private static List<ReportColumn> lossColumns() {
    return List.of(
        ReportColumn.date("loss_date", "Date of Loss"),
        ReportColumn.date("reported_date", "Date Reported"),
        text("loss_nature", "Nature of Loss"),
        text("claim_type", "Claim Type"),
        text("loss_description", "Loss Description"),
        text("loss_place", "Place of Loss"),
        text("catastrophe_code", "Catastrophe"),
        text("catastrophe_event", "Catastrophe Event"),
        ReportColumn.amountNoTotal("claim_amount", "Claim Amount"),
        ReportColumn.amountNoTotal("deductible", "Deductible"),
        ReportColumn.amountNoTotal("initial_reserve", "Initial Reserve"),
        text("claimant_name", "Claimant"),
        text("claimant_overridden", "Claimant Overridden"),
        text("status_code", "Status"),
        text("phase", "Phase"),
        text("closure_kind", "Closure"),
        text("settlement_type_code", "Settlement Type"),
        ReportColumn.amountNoTotal("settlement_amount", "Settlement Amount"),
        ReportColumn.date("date_settled", "Date Settled"),
        ReportColumn.date("closed_on", "Closed On"),
        text("adjuster_code", "Adjuster"),
        ReportColumn.date("next_follow_up_date", "Next Follow Up"),
        text("next_action_plan", "Next Action Plan"),
        text("insurer_code", "Insurer"),
        new ReportColumn("share_pct", "Share %", ColumnType.PERCENT, false),
        text("insurer_claim_no", "Insurer Claim No."),
        ReportColumn.date("reported_to_insurer_on", "Reported to Insurer"),
        ReportColumn.amountNoTotal("reserve_amount", "Insurer Reserve"),
        ReportColumn.amountNoTotal("settled_amount", "Insurer Settled"),
        text("line_adjuster", "Line Adjuster"),
        number("account_item_no", "Location Item No."),
        text("address", "Location Address"),
        text("city", "City"),
        text("province", "Province"),
        text("location_key", "Location Key"));
  }

  private static ReportColumn text(String key, String label) {
    return ReportColumn.text(key, label);
  }

  private static ReportColumn number(String key, String label) {
    return new ReportColumn(key, label, ColumnType.NUMBER, false);
  }
}
