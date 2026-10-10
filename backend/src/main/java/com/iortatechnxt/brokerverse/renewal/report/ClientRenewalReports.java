package com.iortatechnxt.brokerverse.renewal.report;

import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Renewal reports listed by the client's FRS (FRRN.044.01) next to the Renewal Status Report,
 * the Expiring List and the operational reports: pipeline, production, audit log, CLPC billing,
 * outcomes per stage, placement, e-policy sending, Renewal Advices, penetration, hold cover, booked
 * non-bank and PEP clients, accounts without policy number, insurer scorecard, unapplied payments
 * and the LAMD reconciliations.
 */
@Configuration
public class ClientRenewalReports {

  private static final String FROM_C = " from rnw_candidate c";
  private static final String WHERE = " where 1 = 1";
  private static final String REF = "renewal_ref";
  private static final String REF_LABEL = "Reference Number";
  private static final String CLIENT = "client_name";
  private static final String CLIENT_LABEL = "Assured's Name";
  private static final String EXPIRY = "expiry_date";
  private static final String EXPIRY_LABEL = "Expiry Date";
  private static final String STAGE = "stage";
  private static final String STAGE_LABEL = "Renewal Status";
  private static final String PREMIUM = "gross_premium";
  private static final String PREMIUM_LABEL = "Total Premium";
  private static final String INSURER = "insurer_code";
  private static final String INSURER_LABEL = "Insurer";
  private static final String ORDER_REF = " order by c.renewal_ref";
  private static final String BASE =
      "select c.renewal_ref, c.client_name, c.segment, c.product_code, c.line_code, c.insurer_code,"
          + " c.expiry_date, c.stage, c.disposition, c.gross_premium, c.account_officer, c.owner_unit";

  private final NbReportJdbc jdbc;
  private final RenewalReportSupport support;

  /**
   * Creates the reports.
   *
   * @param jdbc report SQL
   * @param support filters and scope
   */
  public ClientRenewalReports(NbReportJdbc jdbc, RenewalReportSupport support) {
    this.jdbc = jdbc;
    this.support = support;
  }

  private ReportDefinition report(SqlRenewalReport.Spec spec) {
    return new SqlRenewalReport(spec, jdbc, support);
  }

  private static List<ReportColumn> base() {
    return List.of(
        ReportColumn.text(REF, REF_LABEL),
        ReportColumn.text(CLIENT, CLIENT_LABEL),
        ReportColumn.text("segment", "Market Segment"),
        ReportColumn.text("product_code", "Risk Code"),
        ReportColumn.text(INSURER, INSURER_LABEL),
        ReportColumn.date(EXPIRY, EXPIRY_LABEL),
        ReportColumn.text(STAGE, STAGE_LABEL),
        ReportColumn.text("disposition", "Disposition"),
        ReportColumn.amount(PREMIUM, PREMIUM_LABEL),
        ReportColumn.text("account_officer", "Account Officer"),
        ReportColumn.text("owner_unit", "Unit"));
  }

  /**
   * Pipeline Report: the open renewal accounts by status and disposition.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalPipelineReport() {
    return report(
        new SqlRenewalReport.Spec(
            "RNW-PIPELINE",
            "Pipeline Report",
            "The open renewal accounts by status and disposition with their premium",
            BASE + FROM_C + WHERE + " and c.stage not in ('RENEWED', 'CLOSED')",
            " order by c.stage, c.expiry_date",
            base()));
  }

  /**
   * Full Production Report: the booked renewals.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalProductionReport() {
    return report(
        new SqlRenewalReport.Spec(
            "RNW-PRODUCTION",
            "Full Production Report",
            "The renewal accounts booked, with the invoice and the premium",
            BASE
                + ", c.renewed_invoice_no, c.closed_at"
                + FROM_C
                + WHERE
                + " and c.stage = 'RENEWED'",
            " order by c.closed_at",
            List.of(
                ReportColumn.text(REF, REF_LABEL),
                ReportColumn.text(CLIENT, CLIENT_LABEL),
                ReportColumn.text("segment", "Market Segment"),
                ReportColumn.text(INSURER, INSURER_LABEL),
                ReportColumn.text("renewed_invoice_no", "Invoice"),
                ReportColumn.date(EXPIRY, EXPIRY_LABEL),
                ReportColumn.amount(PREMIUM, PREMIUM_LABEL),
                ReportColumn.text("account_officer", "Account Officer"))));
  }

  /**
   * Audit Log Report: the audit entries of the renewal accounts.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalAuditLogReport() {
    return report(
        new SqlRenewalReport.Spec(
            "RNW-AUDIT-LOG",
            "Audit Log Report",
            "Every action recorded on the renewal accounts with the user and the time",
            "select c.renewal_ref, c.client_name, a.occurred_at, a.username, a.action, a.summary"
                + " from rnw_candidate c join audit_log a on a.entity_type = 'RenewalCandidate'"
                + " and a.entity_id = c.renewal_ref"
                + WHERE,
            " order by a.occurred_at desc",
            List.of(
                ReportColumn.text(REF, REF_LABEL),
                ReportColumn.text(CLIENT, CLIENT_LABEL),
                ReportColumn.date("occurred_at", "Date"),
                ReportColumn.text("username", "User"),
                ReportColumn.text("action", "Action"),
                ReportColumn.text("summary", "Details"))));
  }

  /**
   * CLPC billing reports: Non-Built-In, all, and Built-In (amortized).
   *
   * @return reports
   */
  @Bean
  public ReportDefinition renewalBillingNonBuiltInReport() {
    return billing(
        "RNW-CLPC-NON-BUILT-IN", "CLPC Billing Report (Non-Built-In FIP)", " and not f.built_in");
  }

  /**
   * CLPC Billing Report of every billed account.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalBillingAllReport() {
    return billing(
        "RNW-CLPC-ALL", "CLPC Billing Report (Built-In HLS Amortized and Non-Built-In)", "");
  }

  /**
   * CLPC Billing Report of the amortized accounts.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalBillingBuiltInReport() {
    return billing(
        "RNW-CLPC-BUILT-IN", "CLPC Billing Report (HLS Amortized Loan Release)", " and f.built_in");
  }

  private ReportDefinition billing(String code, String title, String filter) {
    return report(
        new SqlRenewalReport.Spec(
            code,
            title,
            "The accounts of the CLPC billing files with their file and delivery",
            "select c.renewal_ref, c.client_name, c.pn_nos, c.expiry_date, c.gross_premium,"
                + " f.file_name, f.created_at, f.delivery_status"
                + " from rnw_candidate c join rnw_billing_file f on f.id = c.billing_file_id"
                + WHERE
                + filter,
            " order by f.created_at desc, c.renewal_ref",
            List.of(
                ReportColumn.text(REF, REF_LABEL),
                ReportColumn.text(CLIENT, CLIENT_LABEL),
                ReportColumn.text("pn_nos", "PN #"),
                ReportColumn.date(EXPIRY, EXPIRY_LABEL),
                ReportColumn.amount(PREMIUM, PREMIUM_LABEL),
                ReportColumn.text("file_name", "Billing File"),
                ReportColumn.date("created_at", "Generated On"),
                ReportColumn.text("delivery_status", "Delivery Status"))));
  }

  /**
   * Successful and Fall-Out Accounts Report per Stage.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalOutcomeReport() {
    return report(
        new SqlRenewalReport.Spec(
            "RNW-OUTCOME-STAGE",
            "Successful and Fall-Out Accounts Report per Stage",
            "Renewed and closed renewal accounts with the stage they reached",
            "select c.stage, case when c.stage = 'RENEWED' then 'Successful' when c.stage = 'CLOSED'"
                + " then 'Fall-Out' else 'Open' end as outcome, count(*) as accounts,"
                + " sum(coalesce(c.gross_premium, 0)) as gross_premium"
                + FROM_C
                + WHERE,
            " group by c.stage order by c.stage",
            List.of(
                ReportColumn.text(STAGE, STAGE_LABEL),
                ReportColumn.text("outcome", "Outcome"),
                ReportColumn.count("accounts", "Accounts"),
                ReportColumn.amount(PREMIUM, PREMIUM_LABEL))));
  }

  /**
   * Placement Summary Report.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalPlacementReport() {
    return report(
        new SqlRenewalReport.Spec(
            "RNW-PLACEMENT-SUMMARY",
            "Placement Summary Report",
            "The placements sent to the insurers with their response and turnaround",
            "select c.renewal_ref, c.client_name, p.insurer_code, p.share_percent, p.premium,"
                + " p.slip_file_name, p.status, p.submitted_at, p.response, p.response_date,"
                + " p.with_issue from rnw_candidate c join rnw_placement p on p.candidate_id = c.id"
                + WHERE,
            " order by p.submitted_at desc nulls last, c.renewal_ref",
            List.of(
                ReportColumn.text(REF, REF_LABEL),
                ReportColumn.text(CLIENT, CLIENT_LABEL),
                ReportColumn.text(INSURER, INSURER_LABEL),
                ReportColumn.percent("share_percent", "Share"),
                ReportColumn.amount("premium", "Premium"),
                ReportColumn.text("slip_file_name", "Placement Slip"),
                ReportColumn.text("status", "Placement Status"),
                ReportColumn.date("submitted_at", "Submitted"),
                ReportColumn.text("response", "Insurer Response"),
                ReportColumn.date("response_date", "Response Date"),
                ReportColumn.text("with_issue", "With Issue"))));
  }

  /**
   * Successfully and Unsuccessfully Sent ePolicy Report.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalEpolicySentReport() {
    return report(
        new SqlRenewalReport.Spec(
            "RNW-EPOLICY-SENT",
            "Successfully and Unsuccessfully Sent ePolicy Report",
            "The e-policies sent to the clients with their delivery status",
            "select c.renewal_ref, c.client_name, c.epolicy_no, m.message_no, m.recipients_to,"
                + " m.status as delivery_status, m.last_error, m.created_at"
                + " from rnw_candidate c join rnw_channel_message m on m.candidate_id = c.id"
                + " and m.doc_kind = 'EPOLICY'"
                + WHERE,
            " order by m.created_at desc",
            List.of(
                ReportColumn.text(REF, REF_LABEL),
                ReportColumn.text(CLIENT, CLIENT_LABEL),
                ReportColumn.text("epolicy_no", "Policy Number"),
                ReportColumn.text("message_no", "Message"),
                ReportColumn.text("recipients_to", "Recipients"),
                ReportColumn.text("delivery_status", "Delivery Status"),
                ReportColumn.text("last_error", "Error"),
                ReportColumn.date("created_at", "Sent On"))));
  }

  /**
   * Generated Renewal Advice Report.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalAdviceReport() {
    return report(
        new SqlRenewalReport.Spec(
            "RNW-RA-GENERATED",
            "Generated Renewal Advice Report",
            "The Renewal Advices generated with their notice and sending",
            "select c.renewal_ref, c.client_name, c.expiry_date, l.letter_no, l.notice,"
                + " l.generated_at, l.sent_at, l.status, l.channel"
                + " from rnw_candidate c join rnw_letter l on l.candidate_id = c.id"
                + " and l.letter_type = 'RA'"
                + WHERE,
            " order by l.generated_at desc",
            List.of(
                ReportColumn.text(REF, REF_LABEL),
                ReportColumn.text(CLIENT, CLIENT_LABEL),
                ReportColumn.date(EXPIRY, EXPIRY_LABEL),
                ReportColumn.text("letter_no", "Letter"),
                ReportColumn.text("notice", "Notice"),
                ReportColumn.date("generated_at", "Generated On"),
                ReportColumn.date("sent_at", "Sent On"),
                ReportColumn.text("status", "Status"),
                ReportColumn.text("channel", "Channel"))));
  }

  /**
   * Penetration Report: renewed against expiring accounts per segment.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalPenetrationReport() {
    return report(
        new SqlRenewalReport.Spec(
            "RNW-PENETRATION",
            "Penetration Report",
            "Expiring and renewed accounts and premium per market segment and product line",
            "select c.segment, c.line_code, count(*) as accounts,"
                + " count(*) filter (where c.stage = 'RENEWED') as renewed,"
                + " round(100.0 * count(*) filter (where c.stage = 'RENEWED') / count(*), 2)"
                + " as penetration, sum(coalesce(c.gross_premium, 0)) as gross_premium"
                + FROM_C
                + WHERE,
            " group by c.segment, c.line_code order by c.segment, c.line_code",
            List.of(
                ReportColumn.text("segment", "Market Segment"),
                ReportColumn.text("line_code", "Product Line"),
                ReportColumn.count("accounts", "Expiring Accounts"),
                ReportColumn.count("renewed", "Renewed Accounts"),
                ReportColumn.percent("penetration", "Penetration"),
                ReportColumn.amount(PREMIUM, PREMIUM_LABEL))));
  }

  /**
   * Hold Cover Report.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalHoldCoverReport() {
    return report(
        new SqlRenewalReport.Spec(
            "RNW-HOLD-COVER",
            "Hold Cover Report",
            "The hold cover requests per insurer with their period and response",
            "select c.renewal_ref, c.client_name, h.request_no, h.kind, h.insurer_code, h.days,"
                + " h.start_date, h.end_date, h.status, h.channel"
                + " from rnw_candidate c join rnw_hold_cover_request h on h.candidate_id = c.id"
                + WHERE,
            " order by h.start_date desc",
            List.of(
                ReportColumn.text(REF, REF_LABEL),
                ReportColumn.text(CLIENT, CLIENT_LABEL),
                ReportColumn.text("request_no", "Request"),
                ReportColumn.text("kind", "Kind"),
                ReportColumn.text(INSURER, INSURER_LABEL),
                ReportColumn.count("days", "Days"),
                ReportColumn.date("start_date", "Start"),
                ReportColumn.date("end_date", "End"),
                ReportColumn.text("status", "Status"),
                ReportColumn.text("channel", "Sent By"))));
  }

  /**
   * Booked Non-Bank Client Report.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalNonBankReport() {
    return report(
        new SqlRenewalReport.Spec(
            "RNW-BOOKED-NON-BANK",
            "Booked Non-Bank Client Report",
            "The booked renewals of clients who are not bank clients",
            BASE
                + FROM_C
                + WHERE
                + " and c.stage = 'RENEWED' and not exists (select 1 from crm_client k"
                + " where k.id = c.client_id and k.bank_client)",
            ORDER_REF,
            base()));
  }

  /**
   * Booked PEP Client Report.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalPepReport() {
    return report(
        new SqlRenewalReport.Spec(
            "RNW-BOOKED-PEP",
            "Booked PEP Client Report",
            "The booked renewals of clients tagged politically exposed",
            BASE
                + FROM_C
                + WHERE
                + " and c.stage = 'RENEWED' and exists (select 1 from scr_client_risk_profile r"
                + " where r.client_id = c.client_id and r.active_tags like '%PEP%')",
            ORDER_REF,
            base()));
  }

  /**
   * Accounts without Policy Number Report.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalNoPolicyReport() {
    return report(
        new SqlRenewalReport.Spec(
            "RNW-NO-POLICY-NO",
            "Accounts without Policy Number Report",
            "The renewal accounts submitted for placement or booked without a policy number",
            BASE
                + FROM_C
                + WHERE
                + " and c.stage in ('FOR_PLACEMENT_BOOKING', 'RENEWED') and c.epolicy_no is null",
            ORDER_REF,
            base()));
  }

  /**
   * Insurer Scorecard Report.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalInsurerScorecardReport() {
    return report(
        new SqlRenewalReport.Spec(
            "RNW-INSURER-SCORECARD",
            "Insurer Scorecard Report",
            "Placements per insurer: sent, approved, rejected, with issue",
            "select p.insurer_code, count(*) as sent,"
                + " count(*) filter (where p.response = 'APPROVED') as approved,"
                + " count(*) filter (where p.response = 'REJECTED') as rejected,"
                + " count(*) filter (where p.with_issue) as with_issue,"
                + " sum(coalesce(p.premium, 0)) as premium"
                + " from rnw_candidate c join rnw_placement p on p.candidate_id = c.id"
                + " and p.submitted_at is not null"
                + WHERE,
            " group by p.insurer_code order by p.insurer_code",
            List.of(
                ReportColumn.text(INSURER, INSURER_LABEL),
                ReportColumn.count("sent", "Placements Sent"),
                ReportColumn.count("approved", "Approved"),
                ReportColumn.count("rejected", "Rejected"),
                ReportColumn.count("with_issue", "With Issue"),
                ReportColumn.amount("premium", "Premium"))));
  }

  /**
   * Unapplied Payment Report with Disposition.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalUnappliedReport() {
    return report(
        new SqlRenewalReport.Spec(
            "RNW-UNAPPLIED",
            "Unapplied Payment Report with Disposition",
            "The unapplied payments of the renewal invoices with the collector disposition",
            "select c.renewal_ref, c.client_name, u.unapplied_ref, u.disposition_code,"
                + " u.cashiering_action, u.amount, u.remarks, u.created_at"
                + " from rnw_candidate c join clx_unapplied_disposition u"
                + " on u.company_id = c.company_id and u.invoice_no in (c.renewed_invoice_no,"
                + " c.new_invoice_no, c.expiring_invoice_no)"
                + WHERE,
            " order by u.created_at desc",
            List.of(
                ReportColumn.text(REF, REF_LABEL),
                ReportColumn.text(CLIENT, CLIENT_LABEL),
                ReportColumn.text("unapplied_ref", "Unapplied Payment"),
                ReportColumn.text("disposition_code", "Disposition"),
                ReportColumn.text("cashiering_action", "Action"),
                ReportColumn.amount("amount", "Amount"),
                ReportColumn.text("remarks", "Remarks"),
                ReportColumn.date("created_at", "Recorded On"))));
  }

  /**
   * Accounts Tagged as RMU from LAMD Report.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalRmuReport() {
    return lamd(
        "RNW-LAMD-RMU",
        "Accounts Tagged as RMU from LAMD Report",
        "The renewal accounts whose loan is RMU in the LAMD report",
        " and l.loan_status = 'RMU'");
  }

  /**
   * Accounts Tagged as RMU but No Booking in System Report.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalRmuNotBookedReport() {
    return lamd(
        "RNW-LAMD-RMU-NOT-BOOKED",
        "Accounts Tagged as RMU but No Booking in System Report",
        "RMU loans whose renewal account is not booked",
        " and l.loan_status = 'RMU' and c.stage <> 'RENEWED'");
  }

  /**
   * Accounts Not Tagged as RMU but with Booking in System Report.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalNotRmuBookedReport() {
    return lamd(
        "RNW-LAMD-NOT-RMU-BOOKED",
        "Accounts Not Tagged as RMU but with Booking in System Report",
        "Loans that are not RMU whose renewal account is booked",
        " and l.loan_status <> 'RMU' and c.stage = 'RENEWED'");
  }

  /**
   * Accounts from LAMD Report Tagged as Submitted in System Report.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalLamdSubmittedReport() {
    return lamd(
        "RNW-LAMD-SUBMITTED",
        "Accounts from LAMD Report Tagged as Submitted in System Report",
        "Loans of the LAMD report whose renewal account comes from a submitted policy",
        " and c.source = 'SUBMITTED'");
  }

  /**
   * Accounts from LAMD Report Not Tagged as RMU and with No Submitted Records in System Report.
   *
   * @return report
   */
  @Bean
  public ReportDefinition renewalLamdNoSubmittedReport() {
    return lamd(
        "RNW-LAMD-NO-SUBMITTED",
        "Accounts from LAMD Report Not Tagged as RMU and with No Submitted Records in System Report",
        "Loans that are not RMU whose renewal account does not come from a submitted policy",
        " and l.loan_status <> 'RMU' and c.source <> 'SUBMITTED'");
  }

  private ReportDefinition lamd(String code, String title, String description, String filter) {
    return report(
        new SqlRenewalReport.Spec(
            code,
            title,
            description,
            "select c.renewal_ref, c.client_name, l.pn_no, l.loan_status, l.ao_code, l.branch,"
                + " c.stage, c.expiry_date"
                + " from rnw_candidate c join rnw_lamd_loan l on l.candidate_id = c.id"
                + WHERE
                + filter,
            ORDER_REF,
            List.of(
                ReportColumn.text(REF, REF_LABEL),
                ReportColumn.text(CLIENT, CLIENT_LABEL),
                ReportColumn.text("pn_no", "PN #"),
                ReportColumn.text("loan_status", "Loan Status"),
                ReportColumn.text("ao_code", "AO Code"),
                ReportColumn.text("branch", "Branch"),
                ReportColumn.text(STAGE, STAGE_LABEL),
                ReportColumn.date(EXPIRY, EXPIRY_LABEL))));
  }
}
