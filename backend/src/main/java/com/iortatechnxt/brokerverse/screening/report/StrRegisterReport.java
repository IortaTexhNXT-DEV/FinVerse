package com.iortatechnxt.brokerverse.screening.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * STR Register (SNSRP-705, 706; FRS 6.1.7): the STRs prepared in the period with their status,
 * committee decision, extraction batch and AMLC filing reference. A register kept for the
 * regulator, so it is also offered in Word. The AMLC extraction files are archived under this
 * report.
 */
@Component
public class StrRegisterReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "SCR-STR-REGISTER";

  private static final String SQL =
      "select s.str_no, k.case_no, s.subject_name, s.status, k.committee_decision,"
          + " cast(s.committee_decided_at at time zone 'Asia/Manila' as date) as committee_date,"
          + " x.batch_no, cast(s.extracted_at at time zone 'Asia/Manila' as date) as extracted_on,"
          + " s.amlc_reference, s.filed_on, s.created_by as prepared_by"
          + " from scr_str s join scr_case k on k.id = s.case_id"
          + " left join scr_str_extraction x on x.id = s.extraction_id"
          + " where s.company_id = :companyId"
          + " and cast(s.created_at"
          + ScrReportSql.DAY
          + " between :fromDate and :toDate"
          + " and (cast(:strStatus as varchar) is null or s.status = :strStatus)"
          + " order by s.str_no";

  private final ScrReportSql sql;

  /**
   * Creates the report.
   *
   * @param sql report SQL
   */
  public StrRegisterReport(ScrReportSql sql) {
    this.sql = sql;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(ScrReportSql.company());
    params.addAll(ScrReportSql.period());
    params.add(
        new ParameterSpec(
            "strStatus",
            "Status",
            ParameterType.SELECT,
            false,
            List.of("DRAFT", "FOR_APPROVAL", "APPROVED", "EXTRACTED", "FILED"),
            null));
    return ReportMetadata.compliance(
            CODE,
            "STR Register",
            "STRs drafted, approved, extracted and filed with their AMLC reference (SNSRP-705, 706)",
            params)
        .asDocument();
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Map<String, Object> args = ScrReportSql.args(p);
    args.put("strStatus", ScrReportSql.text(p, "strStatus"));
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("str_no", "STR No."),
            ReportColumn.text("case_no", "Case No."),
            ReportColumn.text("subject_name", "Subject"),
            ReportColumn.text("status", "Status"),
            ReportColumn.text("committee_decision", "Committee Decision"),
            ReportColumn.date("committee_date", "Decision Date"),
            ReportColumn.text("batch_no", "Extraction Batch"),
            ReportColumn.date("extracted_on", "Extracted On"),
            ReportColumn.text("amlc_reference", "AMLC Reference"),
            ReportColumn.date("filed_on", "Filed On"),
            ReportColumn.text("prepared_by", "Prepared By"))
        .rows(sql.rows(SQL, args))
        .presorted()
        .withoutGrandTotal()
        .note(ScrReportSql.LAYOUT_NOTE)
        .build();
  }
}
