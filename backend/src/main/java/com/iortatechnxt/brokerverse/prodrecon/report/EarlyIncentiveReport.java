package com.iortatechnxt.brokerverse.prodrecon.report;

import com.iortatechnxt.brokerverse.prodrecon.domain.ReconCycle;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconCycleRepository;
import com.iortatechnxt.brokerverse.prodrecon.service.EarlyIncentiveValidator;
import com.iortatechnxt.brokerverse.prodrecon.service.EarlyIncentiveValidator.Line;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Early Incentive Validation (PRC-EARLY-INCENTIVE, PRCID.028): each booked invoice of the cycles in
 * the period with the rule's rate, the first remittance and its days from the basis date, the
 * expected incentive, the insurer's reported incentive and the outcome (eligible, late, not
 * remitted, no insurer data, no rule). The rule comes from the remittance incentive seam (OQ23).
 */
@Component
public class EarlyIncentiveReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "PRC-EARLY-INCENTIVE";

  private final ReconCycleRepository cycles;
  private final EarlyIncentiveValidator validator;

  /**
   * Creates the report.
   *
   * @param cycles cycles
   * @param validator validation
   */
  public EarlyIncentiveReport(ReconCycleRepository cycles, EarlyIncentiveValidator validator) {
    this.cycles = cycles;
    this.validator = validator;
  }

  @Override
  public ReportMetadata metadata() {
    return ReconReportSupport.metadata(
        CODE,
        "Early Incentive Validation",
        "Incentives on accounts remitted within the window from inception (PRCID.028)");
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Map<String, Object> args = ReconReportSupport.args(p);
    Long companyId = (Long) args.get(ReconReportSupport.COMPANY);
    String insurer = (String) args.get(ReconReportSupport.INSURER);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (ReconCycle cycle :
        cycles.findByCompanyIdAndProductionMonthBetweenOrderByProductionMonthAscInsurerCodeAsc(
            companyId,
            (LocalDate) args.get(ReconReportSupport.FROM),
            (LocalDate) args.get(ReconReportSupport.TO))) {
      if (insurer == null || insurer.equals(cycle.getInsurerCode())) {
        validator.validate(companyId, cycle.getId()).forEach(l -> rows.add(row(cycle, l)));
      }
    }
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("cycle", "Cycle"),
            ReportColumn.text("invoice", "Invoice Number"),
            ReportColumn.text("assured", "Assured Name"),
            ReportColumn.text("segment", "Segment"),
            ReportColumn.text("line", "Product Line"),
            ReportColumn.date("inception", "Inception Date"),
            ReportColumn.date("remitted", "First Remittance"),
            ReportColumn.text("days", "Days"),
            ReportColumn.amountNoTotal("rate", "Rate %"),
            ReportColumn.amount("expected", "Expected Incentive"),
            ReportColumn.amount("reported", "Insurer Incentive"),
            ReportColumn.text("outcome", "Outcome"))
        .groupBy("insurer", "Insurance Company")
        .rows(rows)
        .presorted()
        .build();
  }

  private static Map<String, Object> row(ReconCycle cycle, Line l) {
    Map<String, Object> r = new LinkedHashMap<>();
    r.put("insurer", cycle.getInsurerCode());
    r.put("cycle", cycle.getCycleNo());
    r.put("invoice", l.invoiceNo());
    r.put("assured", l.assuredName());
    r.put("segment", l.segment());
    r.put("line", l.productLine());
    r.put("inception", l.inceptionDate());
    r.put("remitted", l.remittedOn());
    r.put("days", l.days() == null ? "" : String.valueOf(l.days()));
    r.put("rate", l.ratePercent());
    r.put("expected", l.expected());
    r.put("reported", l.insurerIncentive());
    r.put("outcome", l.outcome().name());
    return r;
  }
}
