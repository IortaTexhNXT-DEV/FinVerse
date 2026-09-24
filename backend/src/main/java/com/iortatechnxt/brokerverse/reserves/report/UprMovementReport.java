package com.iortatechnxt.brokerverse.reserves.report;

import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveLineValues;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveType;
import com.iortatechnxt.brokerverse.reserves.domain.UprItem;
import com.iortatechnxt.brokerverse.reserves.domain.ValuationRun;
import com.iortatechnxt.brokerverse.reserves.service.GrossRi;
import com.iortatechnxt.brokerverse.reserves.service.ReserveAnalysisService;
import com.iortatechnxt.brokerverse.reserves.service.ValuationRunService;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

/**
 * RSV-UPR-MOVE UPR Movement: per line of business, opening UPR (previous posted valuation) +
 * premium written in the month − premium earned = closing UPR, gross and net of reinsurance. The
 * earned premium is derived: opening + written − closing.
 */
@Component
public class UprMovementReport implements ReportDefinition {

  private static final String[] STAGES = {"opening", "written", "earned", "closing"};
  private static final int OPENING = 0;
  private static final int WRITTEN = 1;
  private static final int EARNED = 2;
  private static final int CLOSING = 3;

  private final ValuationRunService runs;

  /**
   * Creates the report.
   *
   * @param runs valuation runs
   */
  public UprMovementReport(ValuationRunService runs) {
    this.runs = runs;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "RSV-UPR-MOVE",
        "UPR Movement",
        ReportCategory.ACTUARIAL,
        "Opening UPR, premium written, premium earned and closing UPR per line of business",
        List.of(
            ParameterSpec.required(ReserveReportSupport.COMPANY, "Company", ParameterType.COMPANY),
            ParameterSpec.required(ReserveReportSupport.DATE, "Valuation Date", ParameterType.DATE)
                .withDefault("TODAY")),
        Permission.REPORT_FINANCIAL);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(ReserveReportSupport.COMPANY);
    LocalDate date = p.date(ReserveReportSupport.DATE);
    Optional<ValuationRun> current =
        runs.forMonth(companyId, date)
            .map(r -> runs.get(r.getId()))
            .or(() -> runs.latestPosted(companyId, ValuationRunService.monthEnd(date)));
    Map<String, GrossRi[]> byLine = new TreeMap<>();
    String note = "No valuation run on or before " + date;
    if (current.isPresent()) {
      ValuationRun run = current.get();
      Optional<ValuationRun> opening = runs.baseline(run);
      opening.ifPresent(o -> addUpr(byLine, ReserveAnalysisService.values(o), OPENING));
      LocalDate monthStart = YearMonth.from(run.getValuationDate()).atDay(1);
      for (UprItem i : runs.uprItems(run.getId())) {
        if (!i.approvalDate().isBefore(monthStart)) {
          GrossRi[] v = stages(byLine, i.key().businessLine());
          v[WRITTEN] =
              v[WRITTEN].plus(new GrossRi(i.written().premium(), i.written().cededPremium()));
        }
      }
      addUpr(byLine, ReserveAnalysisService.values(run), CLOSING);
      note =
          "Closing: run "
              + run.getPeriodName()
              + " ("
              + run.getStatus()
              + "); opening: "
              + opening.map(ValuationRun::getPeriodName).orElse("none (first valuation)");
    }
    return TabularReportBuilder.of(p)
        .columns(columns())
        .rows(rows(byLine))
        .presorted()
        .note(note + ". Earned = opening + written - closing.")
        .build();
  }

  private static List<ReportColumn> columns() {
    List<ReportColumn> cols = new ArrayList<>();
    cols.add(ReportColumn.text(ReserveReportSupport.K_CLASS, "Line of Business"));
    for (String s : STAGES) {
      cols.add(ReportColumn.amount(s, "Gross " + label(s)));
    }
    for (String s : STAGES) {
      cols.add(ReportColumn.amount("net" + s, "Net " + label(s)));
    }
    return cols;
  }

  private static String label(String stage) {
    return switch (stage) {
      case "opening" -> "Opening UPR";
      case "written" -> "Written";
      case "earned" -> "Earned";
      default -> "Closing UPR";
    };
  }

  private static List<Map<String, Object>> rows(Map<String, GrossRi[]> byLine) {
    List<Map<String, Object>> rows = new ArrayList<>();
    byLine.forEach(
        (line, v) -> {
          v[EARNED] =
              new GrossRi(
                  v[OPENING].gross().add(v[WRITTEN].gross()).subtract(v[CLOSING].gross()),
                  v[OPENING].ri().add(v[WRITTEN].ri()).subtract(v[CLOSING].ri()));
          Map<String, Object> row = new LinkedHashMap<>();
          row.put(ReserveReportSupport.K_CLASS, line);
          for (int n = 0; n < STAGES.length; n++) {
            row.put(STAGES[n], v[n].gross());
            row.put("net" + STAGES[n], v[n].net());
          }
          rows.add(row);
        });
    return rows;
  }

  private static void addUpr(Map<String, GrossRi[]> byLine, List<ReserveLineValues> lines, int at) {
    lines.stream()
        .filter(l -> l.type() == ReserveType.UPR)
        .forEach(
            l -> {
              GrossRi[] v = stages(byLine, l.key().businessLine());
              v[at] = v[at].plus(new GrossRi(l.gross(), l.ri()));
            });
  }

  private static GrossRi[] stages(Map<String, GrossRi[]> byLine, String line) {
    return byLine.computeIfAbsent(
        line, k -> new GrossRi[] {GrossRi.zero(), GrossRi.zero(), GrossRi.zero(), GrossRi.zero()});
  }
}
