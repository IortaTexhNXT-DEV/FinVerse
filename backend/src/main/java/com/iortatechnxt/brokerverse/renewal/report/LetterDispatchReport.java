package com.iortatechnxt.brokerverse.renewal.report;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import org.springframework.stereotype.Component;

/**
 * Letter dispatch (RNW-RA-DISPATCH; FR-RN-081, BRRN.001/010/025/037): the RA, NAL, NFR, NRNS and
 * non-acceptance letters with the client e-mail used, the status and the failures.
 */
@Component
public class LetterDispatchReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "RNW-RA-DISPATCH";

  private static final String SQL =
      "select l.letter_type, l.letter_no, c.renewal_ref, c.client_name, c.client_email, l.notice,"
          + " l.status, l.recipients, l.failure, l.min_notice_confirmed_by,"
          + " cast(l.generated_at at time zone '"
          + BusinessClock.zoneId()
          + "' as date) as generated_on,"
          + " cast(l.sent_at at time zone '"
          + BusinessClock.zoneId()
          + "' as date) as sent_on, c.expiry_date"
          + " from rnw_letter l join rnw_candidate c on c.id = l.candidate_id where 1 = 1"
          + RenewalReportSupport.FILTERS
          + " order by l.letter_type, l.generated_at, l.letter_no";

  private final NbReportJdbc jdbc;
  private final RenewalReportSupport support;

  /**
   * Creates the report.
   *
   * @param jdbc report SQL
   * @param support filters and scope
   */
  public LetterDispatchReport(NbReportJdbc jdbc, RenewalReportSupport support) {
    this.jdbc = jdbc;
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return RenewalReportSupport.metadata(
        CODE,
        "Renewal Letter Dispatch",
        "Renewal Advices and letters with their recipients and delivery status");
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("letter_no", "Letter No."),
            ReportColumn.text("renewal_ref", "Renewal Reference"),
            ReportColumn.text("client_name", "Client"),
            ReportColumn.text("client_email", "Client E-mail"),
            ReportColumn.text("notice", "Notice"),
            ReportColumn.text("status", "Status"),
            ReportColumn.text("recipients", "Sent To"),
            ReportColumn.text("failure", "Failure"),
            ReportColumn.text("min_notice_confirmed_by", "Late RA Confirmed By"),
            ReportColumn.date("generated_on", "Generated"),
            ReportColumn.date("sent_on", "Sent"),
            ReportColumn.date("expiry_date", "Expiry Date"))
        .groupBy("letter_type", "Letter")
        .rows(jdbc.rows(SQL, support.args(p).map()))
        .presorted()
        .build();
  }
}
