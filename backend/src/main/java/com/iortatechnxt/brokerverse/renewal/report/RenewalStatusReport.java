package com.iortatechnxt.brokerverse.renewal.report;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Renewal status (RNW-STATUS; FR-RN-101, BRD 1.009, 2.008, 3.010, 4.008, BRRN.019): per expiring
 * invoice, the 37 columns of the Marketing variant or the 40 of the Processing variant; or the
 * 34-line summary of counters (the project's proposal of RQ10; counters without a BRD source show
 * 0).
 */
@Component
public class RenewalStatusReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "RNW-STATUS";

  private static final String VARIANT = "variant";
  private static final String VIEW = "view";
  private static final String PROCESSING = "PROCESSING";
  private static final String SUMMARY = "SUMMARY";

  private static final String SQL =
      "select c.unit_head, c.business_origin, c.account_officer, c.renewal_ref,"
          + " c.expiring_invoice_no, c.assured_name, c.line_code, c.inception_date, c.expiry_date,"
          + " ra.period_from as renewal_from, ra.period_to as renewal_to, c.expiring_policy_no,"
          + " c.account_type, c.basic_premium, c.gross_premium, c.premium_rate, c.commission_rate,"
          + " c.total_sum_insured, c.product_name, c.client_email, c.region_code, c.department_code,"
          + " c.branch_code, c.insurer_code,"
          + " case when c.mortgaged then 'Yes' else 'No' end as mortgaged, c.mortgagee_bank, c.pn_nos,"
          + " c.stage, c.nonrenewal_reason, c.disposition_remarks, c.assigned_po, c.assigned_ao"
          + " from rnw_candidate c left join acc_account ra on ra.arn = c.renewal_arn where 1 = 1"
          + RenewalReportSupport.FILTERS
          + " order by c.expiry_date, c.renewal_ref";

  private static final String COUNTER_PAIRS =
      " count(*) filter (where c.mortgaged and c.disposition = 'LOST_BUSINESS') as m_lost,"
          + " count(*) filter (where c.mortgaged and c.disposition = 'FOR_PROPOSAL'"
          + " and c.stage <> 'RENEWED') as m_proposal,"
          + " count(*) filter (where c.mortgaged and c.transferred) as m_transferred,"
          + " count(*) filter (where c.mortgaged and c.nonrenewal_reason = 'LOAN_FULLY_PAID') as m_paid,"
          + " count(*) filter (where c.mortgaged and c.stage = 'RA_SENT') as m_awaiting,"
          + " count(*) filter (where c.mortgaged and c.nonrenewal_reason = 'BOOKED_TO_NEW_INVOICE')"
          + " as m_new_invoice,"
          + " count(*) filter (where c.mortgaged and c.disposition is null) as m_none,"
          + " count(*) filter (where not c.mortgaged and c.disposition = 'LOST_BUSINESS') as n_lost,"
          + " count(*) filter (where not c.mortgaged and c.disposition = 'FOR_PROPOSAL'"
          + " and c.stage <> 'RENEWED') as n_proposal,"
          + " count(*) filter (where not c.mortgaged and c.transferred) as n_transferred,"
          + " count(*) filter (where not c.mortgaged and c.nonrenewal_reason = 'LOAN_FULLY_PAID') as n_paid,"
          + " count(*) filter (where not c.mortgaged and c.stage = 'RA_SENT') as n_awaiting,"
          + " count(*) filter (where not c.mortgaged and c.nonrenewal_reason = 'BOOKED_TO_NEW_INVOICE')"
          + " as n_new_invoice,"
          + " count(*) filter (where not c.mortgaged and c.disposition is null) as n_none,";

  private static final String COUNTERS =
      "select count(*) filter (where c.stage = 'RENEWED') as booked,"
          + " count(*) filter (where ra.status = 'CANCELLED') as cancelled,"
          + " count(*) filter (where c.stage = 'FOR_PLACEMENT_BOOKING') as for_placement,"
          + " count(*) filter (where c.stage = 'NB_PATH' and c.proposal_ref is not null) as for_proposal,"
          + " count(*) filter (where c.stage = 'NB_PATH' and c.quotation_ref is not null) as for_arf,"
          + " count(*) filter (where c.stage in ('RA_SENT', 'ACCEPTED', 'FOR_PLACEMENT_BOOKING', 'RENEWED'))"
          + " as ra_processed,"
          + " count(*) filter (where c.stage in ('RA_READY', 'RA_GENERATED')) as for_ra,"
          + " count(*) filter (where c.returned) as returned,"
          + " count(*) filter (where c.stage = 'FOR_TL_REVIEW' and c.disposition = 'FOR_RENEWAL') as tl_renewal,"
          + " count(*) filter (where c.stage = 'FOR_TL_REVIEW' and c.disposition = 'FOR_QUOTATION') as tl_quotation,"
          + " count(*) filter (where c.stage = 'FOR_TL_REVIEW' and c.disposition = 'NOT_FOR_RENEWAL') as tl_nfr,"
          + " count(*) filter (where c.stage = 'FOR_TL_REVIEW' and c.disposition = 'LOST_BUSINESS') as tl_lost,"
          + " count(*) filter (where exists (select 1 from wf_case wc join wf_case_history h on h.case_id = wc.id"
          + " where wc.entity_type = 'RenewalCandidate' and wc.entity_id = cast(c.id as varchar)"
          + " and h.reason_code = 'DISAPPROVED')) as disapproved,"
          + " count(*) filter (where c.disposition = 'NOT_FOR_RENEWAL') as nfr,"
          + COUNTER_PAIRS
          + " count(*) filter (where c.nonrenewal_reason = 'NON_RENEWABLE_ACCOUNT') as non_renewable,"
          + " count(*) filter (where c.stage in ('UNASSIGNED', 'FOR_DISPOSITION') and c.disposition is null)"
          + " as no_disposition,"
          + " count(*) as total"
          + " from rnw_candidate c left join acc_account ra on ra.arn = c.renewal_arn where 1 = 1"
          + RenewalReportSupport.FILTERS;

  private static final String[][] SUMMARY_LINES = {
    {"booked", "Booked"},
    {"cancelled", "Cancelled"},
    {"for_placement", "Renewal - For Placement"},
    {"for_proposal", "Renewal - For Proposal"},
    {"for_arf", "Renewal - For ARF"},
    {"ra_processed", "RA Processed"},
    {"for_ra", "Renewal - For RA Processing"},
    {null, "Renew to TSU"},
    {"returned", "Returned to Marketing"},
    {"tl_renewal", "Awaiting Marketing TL Approval - For Renewal"},
    {"tl_quotation", "Awaiting Marketing TL Approval - For Quotation"},
    {"tl_nfr", "Awaiting Marketing TL Approval - Not For Renewal"},
    {"tl_lost", "Awaiting Marketing TL Approval - Lost Business"},
    {"disapproved", "Disapproved Accounts"},
    {"nfr", "Not For Renewal Accounts"},
    {"m_lost", "Mortgaged - Lost Business"},
    {"m_proposal", "Mortgaged - Unrenewed For Proposal"},
    {"m_transferred", "Mortgaged - Transferred to Other Unit"},
    {"m_paid", "Mortgaged - Loan Fully Paid"},
    {"m_awaiting", "Mortgaged - Awaiting Confirmation"},
    {"m_new_invoice", "Mortgaged - Booked under New Invoice"},
    {"m_none", "Mortgaged - No Disposition"},
    {null, "Mortgaged - to Other Bank"},
    {"n_lost", "Non-Mortgaged - Lost Business"},
    {"n_proposal", "Non-Mortgaged - Unrenewed For Proposal"},
    {"n_transferred", "Non-Mortgaged - Transferred to Other Unit"},
    {"n_paid", "Non-Mortgaged - Loan Fully Paid"},
    {"n_awaiting", "Non-Mortgaged - Awaiting Confirmation"},
    {"n_new_invoice", "Non-Mortgaged - Booked under New Invoice"},
    {"n_none", "Non-Mortgaged - No Disposition"},
    {null, "Non-Mortgaged - to Other Bank"},
    {"non_renewable", "Non-Renewable Accounts"},
    {"no_disposition", "No Disposition"},
    {"total", "Total No. of Accounts"}
  };

  private final NbReportJdbc jdbc;
  private final RenewalReportSupport support;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   * @param support filters and scope
   */
  public RenewalStatusReport(NbReportJdbc jdbc, RenewalReportSupport support) {
    this.jdbc = jdbc;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return RenewalReportSupport.metadata(
        CODE,
        "Renewal Status",
        "Status of every renewal per expiring invoice, or the summary of counters",
        ParameterSpec.select(VARIANT, "Variant", List.of("MARKETING", PROCESSING), "MARKETING"),
        ParameterSpec.select(VIEW, "View", List.of("DETAIL", SUMMARY), "DETAIL"));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    if (SUMMARY.equals(p.optionalText(VIEW).orElse(""))) {
      return summary(p);
    }
    boolean processing = PROCESSING.equals(p.optionalText(VARIANT).orElse(""));
    var rows =
        jdbc.rows(SQL, support.args(p).map()).stream().map(RenewalReportSupport::labels).toList();
    return TabularReportBuilder.of(p).columns(columns(processing)).rows(rows).presorted().build();
  }

  private ReportResult summary(ReportParameters p) {
    Map<String, Object> counts = jdbc.rows(COUNTERS, support.args(p).map()).get(0);
    List<Map<String, Object>> rows = new ArrayList<>();
    int no = 1;
    for (String[] line : SUMMARY_LINES) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("no", no++);
      row.put("counter", line[1]);
      row.put("accounts", line[0] == null ? 0 : counts.get(line[0]));
      rows.add(row);
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.count("no", "#"),
            ReportColumn.text("counter", "Counter"),
            ReportColumn.count("accounts", "Accounts"))
        .rows(rows)
        .presorted()
        .withoutGrandTotal()
        .build();
  }

  private static List<ReportColumn> columns(boolean processing) {
    List<ReportColumn> cols =
        new ArrayList<>(
            List.of(
                ReportColumn.text("unit_head", "Unit Head"),
                ReportColumn.text("business_origin", "Business Origin"),
                ReportColumn.text("account_officer", "Bank Officer"),
                ReportColumn.text("renewal_ref", "Renewal Reference"),
                ReportColumn.text("expiring_invoice_no", "Expiring Invoice No."),
                ReportColumn.text("assured_name", "Assured"),
                ReportColumn.text("line_code", "Type of Risk"),
                ReportColumn.date("inception_date", "Expiring Inception Date"),
                ReportColumn.date("expiry_date", "Expiring Expiry Date"),
                ReportColumn.date("renewal_from", "Renewal Inception Date"),
                ReportColumn.date("renewal_to", "Renewal Expiry Date"),
                ReportColumn.text("expiring_policy_no", "Expiring Policy Number"),
                ReportColumn.text("account_type", "Account Type"),
                ReportColumn.amount("basic_premium", "Basic Premium"),
                ReportColumn.amount("gross_premium", "Gross Premium"),
                ReportColumn.amountNoTotal("premium_rate", "Premium Rate"),
                ReportColumn.amountNoTotal("commission_rate", "Commission Rate"),
                ReportColumn.amount("total_sum_insured", "Total Sum Insured"),
                ReportColumn.text("product_name", "Risk Description"),
                ReportColumn.text("client_email", "Email Address"),
                ReportColumn.text("region_code", "Region"),
                ReportColumn.text("department_code", "Area"),
                ReportColumn.text("branch_code", "Branch"),
                ReportColumn.text("insurer_code", "Insurer"),
                ReportColumn.text("mortgaged", "Mortgaged To Group Bank"),
                ReportColumn.text("mortgagee_bank", "Mortgagee Bank"),
                ReportColumn.text("pn_nos", "PN Number"),
                ReportColumn.text("stage", "Renewal Status"),
                ReportColumn.text("nonrenewal_reason", "Reason for Not for Renewal"),
                ReportColumn.text("disposition_remarks", "Remarks of Acct Officer")));
    if (processing) {
      cols.add(ReportColumn.text("assigned_po", "Name of Renewal Processor"));
      cols.add(ReportColumn.text("assigned_ao", "Name of Assigned Marketing AO"));
    }
    return cols;
  }
}
