package com.iortatechnxt.brokerverse.prodrecon.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The list reports of production reconciliation: unbooked accounts and their status (PRC-UNBOOKED,
 * PRCID.019), the extraction log with item counts and timestamps (PRC-EXTRACT-LOG, PRCID.034) and
 * the unmatched accounts with their feedback and disposition (PRC-UNMATCHED-FEEDBACK, PRCID.038).
 * Draft layouts until BDOI confirms them (OQ42).
 */
@Configuration(proxyBeanMethods = false)
public class ReconListReports {

  private static final String MONTH = "to_char(c.production_month, 'YYYY-MM') as month";
  private static final String MONTH_LABEL = "Production Month";

  /**
   * Unbooked accounts and their status (PRCID.019).
   *
   * @param support report SQL
   * @return report
   */
  @Bean
  public ReportDefinition unbookedAccountsReport(ReconReportSupport support) {
    return new SqlReport(
        support,
        ReconReportSupport.metadata(
            "PRC-UNBOOKED",
            "Unbooked Accounts and Status",
            "Insurer production without a BDOI booking and its resolution (PRCID.019)"),
        "select c.insurer_code as insurer, "
            + MONTH
            + ", i.ins_reference_no as reference, i.ins_policy_no as policy,"
            + " i.ins_assured_name as assured, i.ins_gross_premium as gross, i.status,"
            + " i.unbooked_status as resolution, i.prebooked_arn as arn, i.disposition,"
            + " i.ins_remarks as remarks"
            + ReconReportSupport.ITEMS_OF_PERIOD
            + " and i.unbooked_status is not null order by 1, 2, i.id",
        SqlReport.Group.INSURER,
        List.of(
            ReportColumn.text("month", MONTH_LABEL),
            ReportColumn.text("reference", "Reference / Invoice No."),
            ReportColumn.text("policy", "Policy No."),
            ReportColumn.text("assured", "Assured Name"),
            ReportColumn.amount("gross", "Gross Premium"),
            ReportColumn.text("status", "Status"),
            ReportColumn.text("resolution", "Resolution"),
            ReportColumn.text("arn", "Pre-booked ARN"),
            ReportColumn.text("disposition", "Disposition"),
            ReportColumn.text("remarks", "Insurer Remarks")));
  }

  /**
   * Item count and extraction timestamp per extract (PRCID.034).
   *
   * @param support report SQL
   * @return report
   */
  @Bean
  public ReportDefinition extractLogReport(ReconReportSupport support) {
    return new SqlReport(
        support,
        ReconReportSupport.metadata(
            "PRC-EXTRACT-LOG",
            "Production Register Extraction Log",
            "Extracts with item count, extraction and sending timestamps (PRCID.005/034)"),
        "select c.insurer_code as insurer, "
            + MONTH
            + ", e.extract_no, e.trigger_type as trigger, e.booking_from, e.booking_to,"
            + " e.file_name, e.row_count, e.new_count,"
            + " to_char(e.created_at at time zone 'Asia/Manila', 'YYYY-MM-DD HH24:MI') as extracted,"
            + " e.created_by,"
            + " to_char(e.sent_at at time zone 'Asia/Manila', 'YYYY-MM-DD HH24:MI') as sent,"
            + " e.recipients"
            + " from prc_extract e join prc_cycle c on c.id = e.cycle_id"
            + " where c.company_id = :companyId and c.production_month between :from and :to"
            + " and (cast(:insurer as varchar) is null or c.insurer_code = :insurer)"
            + " order by 1, e.id",
        SqlReport.Group.INSURER,
        List.of(
            ReportColumn.text("month", MONTH_LABEL),
            ReportColumn.text("extract_no", "Extract No."),
            ReportColumn.text("trigger", "Trigger"),
            ReportColumn.date("booking_from", "Booked From"),
            ReportColumn.date("booking_to", "Booked To"),
            ReportColumn.text("file_name", "File"),
            ReportColumn.count("row_count", "Items"),
            ReportColumn.count("new_count", "New Items"),
            ReportColumn.text("extracted", "Extracted At"),
            ReportColumn.text("created_by", "Extracted By"),
            ReportColumn.text("sent", "Sent At"),
            ReportColumn.text("recipients", "Sent To")));
  }

  /**
   * Unmatched accounts with feedback and disposition (PRCID.038).
   *
   * @param support report SQL
   * @return report
   */
  @Bean
  public ReportDefinition unmatchedFeedbackReport(ReconReportSupport support) {
    return new SqlReport(
        support,
        ReconReportSupport.metadata(
            "PRC-UNMATCHED-FEEDBACK",
            "Unmatched Accounts with Feedback and Disposition",
            "Unmatched and discrepant accounts with company concerned, feedback and disposition"
                + " (PRCID.038)"),
        "select c.insurer_code as insurer, "
            + MONTH
            + ", coalesce(i.invoice_no, i.ins_reference_no) as reference,"
            + " coalesce(i.bdoi_policy_no, i.ins_policy_no) as policy,"
            + " coalesce(i.bdoi_assured_name, i.ins_assured_name) as assured, i.status,"
            + " i.discrepancies, i.company_concerned, i.instruction, i.insurer_feedback,"
            + " i.marketing_feedback, i.disposition,"
            + " case when i.for_closure then 'Yes' else 'No' end as closure"
            + ReconReportSupport.ITEMS_OF_PERIOD
            + " and i.status <> 'MATCHED' order by 1, 2, i.id",
        SqlReport.Group.INSURER,
        List.of(
            ReportColumn.text("month", MONTH_LABEL),
            ReportColumn.text("reference", "Invoice / Reference No."),
            ReportColumn.text("policy", "Policy No."),
            ReportColumn.text("assured", "Assured Name"),
            ReportColumn.text("status", "Status"),
            ReportColumn.text("discrepancies", "Differences"),
            ReportColumn.text("company_concerned", "Company Concerned"),
            ReportColumn.text("instruction", "Instruction"),
            ReportColumn.text("insurer_feedback", "Insurer Feedback"),
            ReportColumn.text("marketing_feedback", "Marketing Feedback"),
            ReportColumn.text("disposition", "Disposition"),
            ReportColumn.text("closure", "For Closure")));
  }
}
