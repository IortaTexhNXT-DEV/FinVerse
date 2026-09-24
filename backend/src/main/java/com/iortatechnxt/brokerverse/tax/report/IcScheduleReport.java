package com.iortatechnxt.brokerverse.tax.report;

import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.tax.domain.IcSchedule;
import com.iortatechnxt.brokerverse.tax.domain.TaxPeriod;
import com.iortatechnxt.brokerverse.tax.service.IcScheduleLine;
import com.iortatechnxt.brokerverse.tax.service.IcScheduleResult;
import com.iortatechnxt.brokerverse.tax.service.IcScheduleResult.RbcSummary;
import com.iortatechnxt.brokerverse.tax.service.IcScheduleService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An Insurance Commission statutory schedule read from the ledger through the IC mapping. Per-LOB
 * schedules are grouped by line of business with a subtotal per LOB; the "Contribution" column is
 * the amount with the line's sign (deductions negative), so totals are the schedule result. The RBC
 * summary adds the factor and requirement columns and the ratio as footnotes.
 */
abstract class IcScheduleReport implements ReportDefinition {

  private static final String LINE = "line";
  private static final String MAPPING = "mapping";
  private static final String SIGNED = "signed";
  private static final String FACTOR = "factor";
  private static final String REQUIREMENT = "requirement";
  private static final String LOB = "lob";

  private final IcScheduleService schedules;
  private final String code;
  private final IcSchedule schedule;

  IcScheduleReport(IcScheduleService schedules, String code, IcSchedule schedule) {
    this.schedules = schedules;
    this.code = code;
    this.schedule = schedule;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        code,
        "IC " + schedule.title(),
        ReportCategory.TAX_STATUTORY,
        "Insurance Commission schedule from the ledger via the IC mapping ("
            + (schedule.byLineOfBusiness() ? "period movement by line of business" : "balances")
            + ")",
        List.of(TaxReportSupport.company(), TaxReportSupport.from(), TaxReportSupport.to()),
        Permission.TAX_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    TaxPeriod period = TaxReportSupport.period(p);
    IcScheduleResult r =
        schedules.compute(TaxReportSupport.companyId(p), schedule, period.from(), period.to());
    List<Map<String, Object>> rows = new ArrayList<>();
    for (IcScheduleLine l : r.lines()) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put(LOB, l.businessLine());
      row.put(LINE, l.lineCode());
      row.put(TaxReportSupport.DESCRIPTION, l.description());
      row.put(MAPPING, l.mapping());
      row.put(TaxReportSupport.AMOUNT, l.amount());
      row.put(SIGNED, l.signedAmount());
      row.put(FACTOR, l.rbcFactor());
      row.put(REQUIREMENT, l.requirement());
      rows.add(row);
    }
    TabularReportBuilder b = TabularReportBuilder.of(p).columns(columns()).rows(rows).presorted();
    if (schedule.byLineOfBusiness()) {
      b.groupBy(LOB, "Line of business");
    }
    b.note("Schedule total: " + r.total().toPlainString());
    if (r.rbc() != null) {
      rbcNotes(r.rbc()).forEach(b::note);
    }
    return b.build();
  }

  private List<ReportColumn> columns() {
    List<ReportColumn> cols = new ArrayList<>();
    cols.add(ReportColumn.text(LINE, "Line"));
    cols.add(ReportColumn.text(TaxReportSupport.DESCRIPTION, "Description"));
    cols.add(ReportColumn.text(MAPPING, "Accounts"));
    cols.add(ReportColumn.amountNoTotal(TaxReportSupport.AMOUNT, "Amount"));
    cols.add(ReportColumn.amount(SIGNED, "Contribution"));
    if (schedule == IcSchedule.RBC) {
      cols.add(ReportColumn.percent(FACTOR, "RBC factor"));
      cols.add(ReportColumn.amount(REQUIREMENT, "RBC requirement"));
    }
    return cols;
  }

  private static List<String> rbcNotes(RbcSummary rbc) {
    List<String> notes = new ArrayList<>();
    notes.add("Available capital (net worth): " + rbc.netWorth().toPlainString());
    notes.add("Total RBC requirement: " + rbc.requirement().toPlainString());
    notes.add(
        "RBC ratio: "
            + (rbc.ratio() == null ? "n/a" : rbc.ratio().toPlainString() + "%")
            + " (hurdle "
            + rbc.hurdle().toPlainString()
            + "%)");
    notes.add(
        "Simplified template: requirement = sum of amount x factor per line, without the"
            + " correlation (square-root) aggregation of the IC RBC framework.");
    return notes;
  }
}
