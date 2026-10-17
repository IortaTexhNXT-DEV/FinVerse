package com.iortatechnxt.brokerverse.finreport.report;

import com.iortatechnxt.brokerverse.finreport.service.FinReportQueries;
import com.iortatechnxt.brokerverse.finreport.service.VoucherGapDetector;
import com.iortatechnxt.brokerverse.finreport.service.VoucherGapDetector.VoucherGap;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
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
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FIN-GL-MISSVCH – Missing Voucher Number List (PREMIA FGL015): gaps in the voucher number series
 * of the journals dated in the period. A series is the voucher number without its sequence (e.g.
 * JV-HO-2026), so gaps never span two series. Journals of every status count as present, including
 * cancelled and rejected ones.
 */
@Component
public class MissingVoucherReport implements ReportDefinition {

  private static final String SERIES = "series";
  private static final String FROM_NO = "missingFrom";
  private static final String TO_NO = "missingTo";
  private static final String COUNT = "count";

  private final FinReportQueries queries;

  /**
   * Creates the report.
   *
   * @param queries finance queries
   */
  public MissingVoucherReport(FinReportQueries queries) {
    this.queries = queries;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(FinParams.company());
    params.addAll(FinParams.txnRange());
    params.add(FinParams.from());
    params.add(FinParams.to());
    return new ReportMetadata(
        "FIN-GL-MISSVCH",
        "Missing Voucher Number List",
        ReportCategory.CONTROL,
        "Gaps in voucher numbering per transaction code series (FGL015)",
        params,
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    List<String> numbers =
        queries.batchNumbers(
            p.longValue(FinParams.COMPANY),
            p.date(FinParams.FROM),
            p.date(FinParams.TO),
            FinReportSupport.journalTypes(p));
    List<Map<String, Object>> rows = new ArrayList<>();
    for (VoucherGap gap : VoucherGapDetector.detect(numbers)) {
      String tc = gap.series().split("-")[0];
      rows.add(
          FinRows.cells(
              SERIES,
              FinReportSupport.transactionCodeLabel(tc) + " (" + gap.series() + ")",
              FROM_NO,
              gap.missingFrom(),
              TO_NO,
              gap.missingTo(),
              COUNT,
              gap.count()));
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(FROM_NO, "Missing From Document Number"),
            ReportColumn.text(TO_NO, "Missing To Document Number"),
            ReportColumn.count(COUNT, "No. of Documents"))
        .groupBy(SERIES, "Transaction Code")
        .presorted()
        .rows(rows)
        .note(
            numbers.size()
                + " voucher numbers checked. Cancelled and rejected vouchers count as present.")
        .build();
  }
}
