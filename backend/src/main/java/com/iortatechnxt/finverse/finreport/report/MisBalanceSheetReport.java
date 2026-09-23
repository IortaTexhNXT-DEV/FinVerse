package com.iortatechnxt.finverse.finreport.report;

import com.iortatechnxt.finverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.finverse.finreport.service.StatementFormat;
import com.iortatechnxt.finverse.finreport.service.StatementFormat.Evaluation;
import com.iortatechnxt.finverse.finreport.service.StatementFormat.Rounding;
import com.iortatechnxt.finverse.finreport.service.StatementFormatService;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.security.domain.Permission;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * FIN-MIS-BS – Balance Sheet in MIS format (PREMIA FMI09): the lines of a financial statement
 * format with their schedule numbers, this year against last year. Last year = same date one year
 * earlier (default) or the previous fiscal year end (parameter). Rounded lines; totals recomputed
 * from the rounded lines.
 */
@Component
public class MisBalanceSheetReport implements ReportDefinition {

  private static final String LAST = "lastYear";
  private static final String THIS = "thisYear";

  private final MisStatements mis;

  /**
   * Creates the report.
   *
   * @param mis MIS statement engine
   */
  public MisBalanceSheetReport(MisStatements mis) {
    this.mis = mis;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-MIS-BS",
        "Balance Sheet (MIS)",
        ReportCategory.FINANCIAL_STATEMENTS,
        "Balance sheet by statement format with schedule references, this year vs last (FMI09)",
        List.of(
            FinParams.company(),
            FinParams.division(),
            MisStatements.formatParam(),
            FinParams.asOf(),
            MisStatements.roundingParam(),
            MisStatements.lastYearParam()),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    AccountHierarchy h = mis.hierarchy(p);
    StatementFormat f = mis.format(p, h, StatementFormatService.BALANCE_SHEET);
    LocalDate asOf = p.date(FinParams.AS_OF);
    LocalDate lastYear = mis.lastYearDate(p, asOf);
    Rounding rounding = MisStatements.rounding(p);
    List<Evaluation> evaluations =
        List.of(
            f.evaluate(h, mis.net(p, null, lastYear), rounding),
            f.evaluate(h, mis.net(p, null, asOf), rounding));
    List<String> notes = new ArrayList<>();
    notes.add("Format " + f.code() + " - " + f.description() + ". As on " + asOf + ".");
    notes.add("Last year = balances as of " + lastYear + ". Rounding: " + rounding + ".");
    notes.add("Negative amounts are shown in brackets.");
    notes.addAll(MisStatements.unmappedNotes(evaluations));
    return new ReportResult(
        "FIN-MIS-BS",
        metadata().title(),
        p.echo(),
        List.of(
            ReportColumn.text(MisStatements.SCHEDULE, "Schedule"),
            ReportColumn.text(MisStatements.PARTICULARS, "Particulars"),
            ReportColumn.amountNoTotal(LAST, "Balance Last Year"),
            ReportColumn.amountNoTotal(THIS, "Balance This Year")),
        MisStatements.lineRows(f, List.of(LAST, THIS), evaluations),
        notes);
  }
}
