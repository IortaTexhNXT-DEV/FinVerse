package com.iortatechnxt.brokerverse.investment.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.report.gl.GlReportSupport;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.List;
import org.springframework.stereotype.Component;

/** FIN-INV-PORT: holdings on a date by instrument type or PFRS 9 classification. */
@Component
public class InvestmentPortfolioReport implements ReportDefinition {

  private static final String GROUP = "groupBy";
  private static final String BY_TYPE = "INSTRUMENT_TYPE";
  private static final String BY_CLASSIFICATION = "CLASSIFICATION";

  private final InvestmentPositions positions;

  /**
   * Creates the report.
   *
   * @param positions position query
   */
  public InvestmentPortfolioReport(InvestmentPositions positions) {
    this.positions = positions;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-INV-PORT",
        "Investment Portfolio",
        ReportCategory.FINANCIAL_STATEMENTS,
        "Investments held on a date with carrying amount and accrued interest",
        List.of(
            GlReportSupport.companyParam(),
            GlReportSupport.branchParam(),
            GlReportSupport.asOfParam(),
            ParameterSpec.select(GROUP, "Group By", List.of(BY_TYPE, BY_CLASSIFICATION), BY_TYPE)),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters params) {
    boolean byType = BY_TYPE.equals(params.text(GROUP));
    return TabularReportBuilder.of(params)
        .columns(
            ReportColumn.text("holdingNo", "Holding No"),
            ReportColumn.text("description", "Description"),
            ReportColumn.text(
                byType ? "classification" : "instrumentType", byType ? "Class" : "Type"),
            ReportColumn.text("issuer", "Issuer"),
            ReportColumn.text("currency", "Ccy"),
            ReportColumn.percent("couponRate", "Coupon %"),
            ReportColumn.date(InvestmentPositions.MATURITY, "Maturity"),
            ReportColumn.amount(InvestmentPositions.FACE, "Face Value"),
            ReportColumn.amount(InvestmentPositions.CARRYING, "Carrying Amount"),
            ReportColumn.amount(InvestmentPositions.ACCRUED, "Accrued Interest"),
            ReportColumn.amount("total", "Total"))
        .groupBy(byType ? "instrumentType" : "classification", byType ? "Instrument" : "Class")
        .rows(positions.asOf(params, false))
        .note("Carrying amount = amortized cost plus fair value adjustments (FVOCI / FVPL).")
        .build();
  }
}
