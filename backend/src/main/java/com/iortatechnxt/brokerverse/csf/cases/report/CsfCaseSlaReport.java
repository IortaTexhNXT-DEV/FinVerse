package com.iortatechnxt.brokerverse.csf.cases.report;

import com.iortatechnxt.brokerverse.csf.cases.domain.CaseStatus;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseCodes;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseStatistics;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseViews.CaseRow;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Case Ageing and SLA ({@value #CODE}; FR-CSF-057, 059, FRS section 6.1.4): the open cases of the
 * period by fulfilment unit and case type in the ageing bands 0-1, 2-3, 4-6 and over 6 working
 * days, with the cases within TAT, approaching breach and breached.
 */
@Component
public class CsfCaseSlaReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "CSF-CASE-SLA";

  private static final List<String> BANDS = List.of("0-1", "2-3", "4-6", "Over 6");

  private final CsfCaseStatistics statistics;

  /**
   * Creates the report.
   *
   * @param statistics case rows
   */
  public CsfCaseSlaReport(CsfCaseStatistics statistics) {
    this.statistics = statistics;
  }

  @Override
  public ReportMetadata metadata() {
    return CaseReportSupport.metadata(
        CODE,
        "Case Ageing and SLA",
        "Open cases by fulfilment unit and case type in ageing bands, within TAT, approaching"
            + " breach and breached",
        false);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Map<String, Map<String, Object>> groups = new LinkedHashMap<>();
    for (CaseRow r : statistics.rows(CaseReportSupport.criteria(p))) {
      if (CaseStatus.valueOf(r.status()).closed()) {
        continue;
      }
      String key = r.fulfilmentUnit().label() + "|" + r.caseType().label();
      Map<String, Object> g = groups.computeIfAbsent(key, k -> group(r));
      add(g, "band_" + BANDS.indexOf(CsfCaseStatistics.band(r.sla().ageDays())));
      add(g, r.sla().status());
      add(g, "total");
    }
    List<Map<String, Object>> rows = new ArrayList<>(groups.values());
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("unit", "Fulfilment Unit"),
            ReportColumn.text("case_type", "Case Type"),
            ReportColumn.count("band_0", "0-1 days"),
            ReportColumn.count("band_1", "2-3 days"),
            ReportColumn.count("band_2", "4-6 days"),
            ReportColumn.count("band_3", "Over 6 days"),
            ReportColumn.count(CsfCaseCodes.WITHIN_TAT, "Within TAT"),
            ReportColumn.count(CsfCaseCodes.APPROACHING, "Approaching Breach"),
            ReportColumn.count(CsfCaseCodes.BREACHED, "Breached"),
            ReportColumn.count("total", "Open Cases"))
        .rows(rows)
        .build();
  }

  private static Map<String, Object> group(CaseRow r) {
    Map<String, Object> g = new LinkedHashMap<>();
    g.put("unit", r.fulfilmentUnit().label());
    g.put("case_type", r.caseType().label());
    for (int i = 0; i < BANDS.size(); i++) {
      g.put("band_" + i, 0L);
    }
    g.put(CsfCaseCodes.WITHIN_TAT, 0L);
    g.put(CsfCaseCodes.APPROACHING, 0L);
    g.put(CsfCaseCodes.BREACHED, 0L);
    g.put("total", 0L);
    return g;
  }

  private static void add(Map<String, Object> g, String key) {
    g.merge(key, 1L, (a, b) -> (Long) a + (Long) b);
  }
}
