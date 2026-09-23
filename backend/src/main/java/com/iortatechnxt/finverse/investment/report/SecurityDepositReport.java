package com.iortatechnxt.finverse.investment.report;

import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.report.gl.GlReportSupport;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * FIN-INV-SECDEP: securities deposited with the Insurance Commission (security fund deposit
 * required of non-life insurers) on a date.
 */
@Component
public class SecurityDepositReport implements ReportDefinition {

  private final InvestmentPositions positions;

  /**
   * Creates the report.
   *
   * @param positions position query
   */
  public SecurityDepositReport(InvestmentPositions positions) {
    this.positions = positions;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-INV-SECDEP",
        "Security Deposit Schedule",
        ReportCategory.FINANCIAL_STATEMENTS,
        "Securities deposited with the Insurance Commission on a date",
        List.of(
            GlReportSupport.companyParam(),
            GlReportSupport.branchParam(),
            GlReportSupport.asOfParam()),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters params) {
    return TabularReportBuilder.of(params)
        .columns(
            ReportColumn.text("holdingNo", "Holding No"),
            ReportColumn.text("securityCode", "Security"),
            ReportColumn.text("description", "Description"),
            ReportColumn.text("issuer", "Issuer"),
            ReportColumn.text("custodian", "Custodian"),
            ReportColumn.date(InvestmentPositions.MATURITY, "Maturity"),
            ReportColumn.amount(InvestmentPositions.FACE, "Face Value"),
            ReportColumn.amount(InvestmentPositions.CARRYING, "Carrying Amount"))
        .groupBy("instrumentType", "Instrument")
        .rows(positions.asOf(params, true))
        .build();
  }
}
