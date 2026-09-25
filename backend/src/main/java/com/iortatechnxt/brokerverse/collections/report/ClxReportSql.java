package com.iortatechnxt.brokerverse.collections.report;

import com.iortatechnxt.brokerverse.report.core.ColumnType;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared parameters, SQL and columns of the Collections reports (BDOI_CLXN_BRD_SPEC section 7;
 * COLLECTIONS_DESIGN 11): every report is a Collections report (view CLX_REPORT_VIEW, export
 * CLX_EXPORT, archived) for the company of the header, with optional segment, sales unit and
 * invoicing branch filters (the weekly files per unit per branch). The item columns follow the
 * p.59-61 field lists where BrokerVerse holds the data; legacy-only fields (cover number, QPS
 * reference, EBIX invoice number) wait for their mapping (CQ22).
 */
@Component
@Transactional(readOnly = true)
public class ClxReportSql {

  /** Company parameter. */
  public static final String COMPANY = "companyId";

  /** Period start. */
  public static final String FROM = "from";

  /** Period end. */
  public static final String TO = "to";

  /** Segment filter. */
  public static final String SEGMENT = "segment";

  /** Sales unit filter. */
  public static final String UNIT = "salesUnit";

  /** Invoicing branch filter. */
  public static final String BRANCH = "branchId";

  /** Note on the draft layouts. */
  static final String DRAFT_NOTE =
      "Layout of BRD p.59-61 with the fields BrokerVerse holds; legacy fields to map (CQ22).";

  /** Item columns selected by the item reports. */
  static final String ITEM_SELECT =
      "select i.client_code, i.assured_name, i.invoice_no, i.policy_no, i.arn, i.booking_date,"
          + " i.aging_days, i.aging_bracket, b.code as branch_code, i.insurer_code,"
          + " i.product_line, i.inception_date, i.expiry_date, i.currency, i.gross_premium,"
          + " i.net_outstanding, i.outstanding_pr2307, i.segment, i.sales_unit,"
          + " i.unit_head_username, i.ao_username, i.current_handler, i.payment_status,"
          + " i.invoice_category, i.disposition_code, i.category, i.tagging_owner,"
          + " i.last_effort_at, i.last_effort_code, i.remarks, i.status, i.completed_on";

  /** Items with their branch. */
  static final String ITEM_FROM =
      " from clx_item i left join org_branch b on b.id = i.branch_id where i.company_id = :companyId";

  /** Optional segment, unit and branch filters. */
  static final String ITEM_FILTERS =
      " and (cast(:segment as varchar) is null or i.segment = :segment)"
          + " and (cast(:salesUnit as varchar) is null or i.sales_unit = :salesUnit)"
          + " and (cast(:branchId as bigint) is null or i.branch_id = :branchId)";

  private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the helper.
   *
   * @param jdbc named-parameter JDBC
   */
  public ClxReportSql(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Metadata of a Collections report.
   *
   * @param code code
   * @param title title
   * @param description description with its BR ID
   * @param period whether the report takes a date range
   * @return metadata
   */
  static ReportMetadata metadata(String code, String title, String description, boolean period) {
    return ReportMetadata.collections(code, title, description, parameters(period));
  }

  /**
   * The parameters of a Collections report.
   *
   * @param period whether the report takes a date range
   * @return company, dates and filters
   */
  static List<ParameterSpec> parameters(boolean period) {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY));
    if (period) {
      params.add(
          ParameterSpec.required(FROM, "From", ParameterType.DATE).withDefault("MONTH_START"));
      params.add(ParameterSpec.required(TO, "To", ParameterType.DATE).withDefault("TODAY"));
    }
    params.add(ParameterSpec.optional(SEGMENT, "Market segment", ParameterType.TEXT));
    params.add(ParameterSpec.optional(UNIT, "Sales unit", ParameterType.TEXT));
    params.add(ParameterSpec.optional(BRANCH, "Invoicing branch", ParameterType.BRANCH));
    return params;
  }

  /**
   * The bind values of the common parameters.
   *
   * @param p parameters
   * @return company, dates, segment, unit and branch (null when absent)
   */
  static Map<String, Object> args(ReportParameters p) {
    Map<String, Object> args = new HashMap<>();
    args.put(COMPANY, p.longValue(COMPANY));
    args.put(FROM, p.optionalDate(FROM).orElse(null));
    args.put(TO, p.optionalDate(TO).orElse(null));
    args.put(SEGMENT, text(p, SEGMENT));
    args.put(UNIT, text(p, UNIT));
    args.put(BRANCH, p.optionalLong(BRANCH).orElse(null));
    return args;
  }

  private static String text(ReportParameters p, String name) {
    return p.optionalText(name).map(String::strip).filter(s -> !s.isEmpty()).orElse(null);
  }

  /**
   * The columns of the item reports (p.59-61 fields BrokerVerse holds).
   *
   * @return columns
   */
  static List<ReportColumn> itemColumns() {
    return List.of(
        ReportColumn.text("client_code", "Client Code"),
        ReportColumn.text("assured_name", "Name of Assured"),
        ReportColumn.text("invoice_no", "Invoice No."),
        ReportColumn.text("policy_no", "Policy No."),
        ReportColumn.text("arn", "Account (ARN)"),
        ReportColumn.date("booking_date", "Booking Date"),
        new ReportColumn("aging_days", "Aging", ColumnType.NUMBER, false),
        ReportColumn.text("aging_bracket", "Aging Bracket"),
        ReportColumn.text("branch_code", "Invoicing Branch"),
        ReportColumn.text("insurer_code", "Insurer"),
        ReportColumn.text("product_line", "Product Line"),
        ReportColumn.date("inception_date", "Inception"),
        ReportColumn.date("expiry_date", "Expiry"),
        ReportColumn.text("currency", "Currency"),
        ReportColumn.amount("gross_premium", "Total Premium"),
        ReportColumn.amount("net_outstanding", "Outstanding Premium"),
        ReportColumn.amount("outstanding_pr2307", "PR2307 Amount"),
        ReportColumn.text("segment", "Market Segment"),
        ReportColumn.text("sales_unit", "Sales Unit"),
        ReportColumn.text("unit_head_username", "Unit Head"),
        ReportColumn.text("ao_username", "Account Officer"),
        ReportColumn.text("current_handler", "Collection Handler"),
        ReportColumn.text("payment_status", "Client Payment Status"),
        ReportColumn.text("invoice_category", "Invoice Category"),
        ReportColumn.text("disposition_code", "Disposition"),
        ReportColumn.text("category", "Category (A/B/C)"),
        ReportColumn.text("tagging_owner", "Tagging Owner"),
        ReportColumn.date("last_effort_at", "Last Collection Effort Date"),
        ReportColumn.text("last_effort_code", "Last Collection Effort Code"),
        ReportColumn.text("remarks", "Remarks"),
        ReportColumn.text("status", "Status"));
  }

  /**
   * Runs a constant query; SQL dates and timestamps become local dates (Philippine time).
   *
   * @param sql constant SQL
   * @param args bind values
   * @return rows
   */
  public List<Map<String, Object>> rows(String sql, Map<String, Object> args) {
    return jdbc.queryForList(sql, args).stream().map(ClxReportSql::local).toList();
  }

  private static Map<String, Object> local(Map<String, Object> row) {
    Map<String, Object> out = new LinkedHashMap<>();
    row.forEach((column, value) -> out.put(column, localValue(value)));
    return out;
  }

  private static Object localValue(Object value) {
    if (value instanceof Timestamp stamp) {
      return stamp.toInstant().atZone(MANILA).toLocalDate();
    }
    return value instanceof Date day ? day.toLocalDate() : value;
  }
}
