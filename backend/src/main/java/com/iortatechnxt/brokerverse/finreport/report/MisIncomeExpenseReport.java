package com.iortatechnxt.brokerverse.finreport.report;

import com.iortatechnxt.brokerverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.brokerverse.finreport.service.StatementFormat;
import com.iortatechnxt.brokerverse.finreport.service.StatementFormat.Evaluation;
import com.iortatechnxt.brokerverse.finreport.service.StatementFormat.Rounding;
import com.iortatechnxt.brokerverse.finreport.service.StatementFormatService;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * FIN-MIS-IE – Income and Expense Statement (PREMIA FMI11): format lines for this period and year
 * to date, against the same windows one year earlier ("last year" basis: the From → To and fiscal
 * year start → To windows shifted back one year). Surplus = income − expenses.
 */
@Component
public class MisIncomeExpenseReport implements ReportDefinition {

  private static final String LY_PERIOD = "lastYearPeriod";
  private static final String LY_YTD = "lastYearYtd";
  private static final String TY_PERIOD = "thisYearPeriod";
  private static final String TY_YTD = "thisYearYtd";

  private final MisStatements mis;

  /**
   * Creates the report.
   *
   * @param mis MIS statement engine
   */
  public MisIncomeExpenseReport(MisStatements mis) {
    this.mis = mis;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-MIS-IE",
        "Income and Expense Statement (MIS)",
        ReportCategory.FINANCIAL_STATEMENTS,
        "Surplus or deficit for the period and year to date against last year (FMI11)",
        List.of(
            FinParams.company(),
            FinParams.division(),
            MisStatements.formatParam(),
            FinParams.from(),
            FinParams.to(),
            MisStatements.roundingParam()),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    AccountHierarchy h = mis.hierarchy(p);
    StatementFormat f = mis.format(p, h, StatementFormatService.INCOME_EXPENSE);
    LocalDate from = p.date(FinParams.FROM);
    LocalDate to = p.date(FinParams.TO);
    LocalDate yearStart = mis.yearStart(p, to);
    Rounding r = MisStatements.rounding(p);
    List<Evaluation> evaluations =
        List.of(
            f.evaluate(h, mis.net(p, from.minusYears(1), to.minusYears(1)), r),
            f.evaluate(h, mis.net(p, yearStart.minusYears(1), to.minusYears(1)), r),
            f.evaluate(h, mis.net(p, from, to), r),
            f.evaluate(h, mis.net(p, yearStart, to), r));
    List<String> notes = new ArrayList<>();
    notes.add("Format " + f.code() + " - " + f.description() + ". Rounding: " + r + ".");
    notes.add(
        "Year to date from "
            + yearStart
            + ". Last year columns = the same windows one year earlier.");
    notes.addAll(MisStatements.unmappedNotes(evaluations));
    return new ReportResult(
        "FIN-MIS-IE",
        metadata().title(),
        p.echo(),
        List.of(
            ReportColumn.amountNoTotal(LY_PERIOD, "Last Year This Period"),
            ReportColumn.amountNoTotal(LY_YTD, "Last Year Year-To-Date"),
            ReportColumn.text(MisStatements.SCHEDULE, "Schedule"),
            ReportColumn.text(MisStatements.PARTICULARS, "Particulars"),
            ReportColumn.amountNoTotal(TY_PERIOD, "This Year This Period"),
            ReportColumn.amountNoTotal(TY_YTD, "This Year Year-To-Date")),
        MisStatements.lineRows(f, List.of(LY_PERIOD, LY_YTD, TY_PERIOD, TY_YTD), evaluations),
        notes);
  }
}
