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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FIN-MIS-IE-SCH – Schedule to Income and Expense Statement (PREMIA FMI12): account-level income
 * and expense under each format line. This Period = From → To; This Year = fiscal year start → To;
 * Last Year = the same From → To window one year earlier. Income is shown credit positive, expenses
 * debit positive (format line signs).
 */
@Component
public class MisIncomeExpenseScheduleReport implements ReportDefinition {

  private static final String PERIOD = "thisPeriod";
  private static final String YEAR = "thisYear";
  private static final String LAST = "lastYear";

  private final MisStatements mis;

  /**
   * Creates the report.
   *
   * @param mis MIS statement engine
   */
  public MisIncomeExpenseScheduleReport(MisStatements mis) {
    this.mis = mis;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-MIS-IE-SCH",
        "Schedule to Income and Expense Statement",
        ReportCategory.FINANCIAL_STATEMENTS,
        "Account-level income and expense: this period, this year and last year (FMI12)",
        List.of(
            FinParams.company(),
            FinParams.division(),
            MisStatements.formatParam(),
            FinParams.from(),
            FinParams.to()),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    AccountHierarchy h = mis.hierarchy(p);
    StatementFormat f = mis.format(p, h, StatementFormatService.INCOME_EXPENSE);
    LocalDate from = p.date(FinParams.FROM);
    LocalDate to = p.date(FinParams.TO);
    LocalDate yearStart = mis.yearStart(p, to);
    List<Map<Long, BigDecimal>> nets =
        List.of(
            mis.net(p, from, to),
            mis.net(p, yearStart, to),
            mis.net(p, from.minusYears(1), to.minusYears(1)));
    List<Evaluation> evaluations = nets.stream().map(n -> f.evaluate(h, n, Rounding.NONE)).toList();
    List<String> notes = new ArrayList<>();
    notes.add(
        "This Year = "
            + yearStart
            + " to "
            + to
            + ". Last Year = "
            + from.minusYears(1)
            + " to "
            + to.minusYears(1)
            + ".");
    notes.addAll(MisStatements.unmappedNotes(evaluations));
    return new ReportResult(
        "FIN-MIS-IE-SCH",
        metadata().title(),
        p.echo(),
        List.of(
            ReportColumn.text(MisStatements.PARTICULARS, "Particulars"),
            ReportColumn.amountNoTotal(PERIOD, "This Period"),
            ReportColumn.amountNoTotal(YEAR, "This Year"),
            ReportColumn.amountNoTotal(LAST, "Last Year")),
        MisStatements.scheduleRows(f, h, List.of(PERIOD, YEAR, LAST), nets, evaluations),
        notes);
  }
}
