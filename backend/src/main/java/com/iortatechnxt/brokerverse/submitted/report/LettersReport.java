package com.iortatechnxt.brokerverse.submitted.report;

import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import java.util.List;
import org.springframework.stereotype.Component;

/** Letters Register ({@value #CODE}): letters sent, printed and refused, with their print batch. */
@Component
public class LettersReport extends SbmSqlReport {

  /** Report code. */
  public static final String CODE = "SBM-LETTERS";

  private static final String SQL =
      "select l.letter_no, p.sbm_no, p.assured_name, l.letter_type, l.channel, "
          + "l.status, l.recipient, l.template_code, l.template_version, b.batch_no, l.error, "
          + "l.created_at, l.sent_at from sbm_letter l join sbm_policy p on p.id = "
          + "l.policy_id left join sbm_print_batch b on b.id = l.print_batch_id where "
          + "l.company_id = :company"
          + SbmReportSupport.SCOPE
          + String.format(SbmReportSupport.RANGE, "l.created_at")
          + " order by l.letter_no";

  /**
   * Creates the report.
   *
   * @param support scope and labels
   */
  public LettersReport(SbmReportSupport support) {
    super(
        support,
        new Spec(
            CODE,
            "Letters Register",
            "Letters sent, printed and refused, with their print batch",
            "Created",
            SQL,
            List.of(
                ReportColumn.text("letter_no", "Letter No."),
                ReportColumn.text("sbm_no", "Masterlist No."),
                ReportColumn.text("assured_name", "Assured"),
                ReportColumn.text("letter_type", "Letter"),
                ReportColumn.text("channel", "Channel"),
                ReportColumn.text("status", "Status"),
                ReportColumn.text("recipient", "Recipient"),
                ReportColumn.text("template_code", "Template"),
                ReportColumn.count("template_version", "Version"),
                ReportColumn.text("batch_no", "Print Batch"),
                ReportColumn.text("error", "Error"),
                ReportColumn.date("created_at", "Created"),
                ReportColumn.date("sent_at", "Sent"))));
  }
}
