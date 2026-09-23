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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FIN-MIS-BS-SCH – Schedule to Balance Sheet (PREMIA FMI10): under each schedule (format line) the
 * GL accounts mapped to it, with this year and last year balances and the schedule total.
 */
@Component
public class MisBalanceSheetScheduleReport implements ReportDefinition {

  private static final String LAST = "lastYear";
  private static final String THIS = "thisYear";

  private final MisStatements mis;

  /**
   * Creates the report.
   *
   * @param mis MIS statement engine
   */
  public MisBalanceSheetScheduleReport(MisStatements mis) {
    this.mis = mis;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "FIN-MIS-BS-SCH",
        "Schedule to Balance Sheet",
        ReportCategory.FINANCIAL_STATEMENTS,
        "Account-level schedules behind each MIS balance sheet line (FMI10)",
        List.of(
            FinParams.company(),
            FinParams.division(),
            MisStatements.formatParam(),
            FinParams.asOf(),
            MisStatements.lastYearParam()),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    AccountHierarchy h = mis.hierarchy(p);
    StatementFormat f = mis.format(p, h, StatementFormatService.BALANCE_SHEET);
    LocalDate asOf = p.date(FinParams.AS_OF);
    LocalDate lastYear = mis.lastYearDate(p, asOf);
    List<Map<Long, BigDecimal>> nets = List.of(mis.net(p, null, lastYear), mis.net(p, null, asOf));
    List<Evaluation> evaluations = nets.stream().map(n -> f.evaluate(h, n, Rounding.NONE)).toList();
    List<String> notes = new ArrayList<>();
    notes.add("Format " + f.code() + " - " + f.description() + ". Last year = " + lastYear + ".");
    notes.addAll(MisStatements.unmappedNotes(evaluations));
    return new ReportResult(
        "FIN-MIS-BS-SCH",
        metadata().title(),
        p.echo(),
        List.of(
            ReportColumn.text(MisStatements.PARTICULARS, "Particulars"),
            ReportColumn.amountNoTotal(LAST, "Balance Last Year"),
            ReportColumn.amountNoTotal(THIS, "Balance This Year")),
        MisStatements.scheduleRows(f, h, List.of(LAST, THIS), nets, evaluations),
        notes);
  }
}
