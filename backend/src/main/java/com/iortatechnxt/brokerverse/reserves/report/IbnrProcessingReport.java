package com.iortatechnxt.brokerverse.reserves.report;

import static com.iortatechnxt.brokerverse.reserves.report.ReserveReportSupport.K_BRANCH;
import static com.iortatechnxt.brokerverse.reserves.report.ReserveReportSupport.K_CLASS;
import static com.iortatechnxt.brokerverse.reserves.report.ReserveReportSupport.K_PRODUCT;
import static com.iortatechnxt.brokerverse.reserves.report.ReserveReportSupport.K_SOURCE;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveType;
import com.iortatechnxt.brokerverse.reserves.service.Percent;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PGIBR079 IBNR Processing Report: per branch, line of business, channel and product the earned
 * premium base, the IBNR rate, the IBNR amount and the reinsurers' portion (IBNR × ceded share),
 * with the method configured for the line. The IBNR rate shown is IBNR ÷ base (for chain-ladder
 * lines the implied rate). Summary = per branch and line of business.
 */
@Component
public class IbnrProcessingReport implements ReportDefinition {

  private static final String SUMMARY = "summary";
  private static final String BASE = "base";
  private static final String IBNR = "ibnr";
  private static final String RATE = "rate";
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final int RATE_SCALE = 4;

  private final ReserveReportSupport support;

  /**
   * Creates the report.
   *
   * @param support report support
   */
  public IbnrProcessingReport(ReserveReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    List<ParameterSpec> params = ReserveReportSupport.baseParams("IBNR Processed Date");
    params.add(ParameterSpec.select(SUMMARY, "Summary", List.of("N", "Y"), "N"));
    return new ReportMetadata(
        "PGIBR079",
        "IBNR Processing Report",
        ReportCategory.ACTUARIAL,
        "IBNR per class and product: earned premium base, rate, IBNR and reinsurers' portion",
        params,
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    boolean summary = "Y".equals(p.text(SUMMARY));
    List<Map<String, Object>> rows =
        ReserveReportSupport.unitRows(
            support.lines(p),
            ReserveType.IBNR,
            summary,
            support.branchCodes(p.longValue(ReserveReportSupport.COMPANY)),
            l -> {
              Map<String, BigDecimal> m = new LinkedHashMap<>();
              m.put(BASE, l.base() == null ? BigDecimal.ZERO : l.base());
              m.put(IBNR, l.gross());
              m.put("ri", l.ri());
              m.put("net", l.net());
              return m;
            });
    rows.forEach(
        r ->
            r.put(
                RATE,
                Percent.ratio((BigDecimal) r.get(IBNR), (BigDecimal) r.get(BASE))
                    .multiply(HUNDRED)
                    .setScale(RATE_SCALE, RoundingMode.HALF_EVEN)));
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text(K_SOURCE, "Source Type"),
            ReportColumn.text(K_PRODUCT, "Product"),
            ReportColumn.percent(RATE, "IBNR Rate %"),
            ReportColumn.amount(BASE, "Base (Earned Premium)"),
            ReportColumn.amount(IBNR, "IBNR Amount"),
            ReportColumn.amount("ri", "RI Portion"),
            ReportColumn.amount("net", "Net IBNR"))
        .groupBy(K_BRANCH, "Branch")
        .groupBy(K_CLASS, "Line of Business")
        .rows(rows)
        .note(
            "Base = earned premium (company share, gross of reinsurance) of the twelve months to"
                + " the processed date. RI portion = IBNR x ceded share of earned premium.")
        .build();
  }
}
