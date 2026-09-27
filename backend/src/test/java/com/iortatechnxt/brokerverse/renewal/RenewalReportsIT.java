package com.iortatechnxt.brokerverse.renewal;

import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.TL;
import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCriteria;
import com.iortatechnxt.brokerverse.renewal.alert.RenewalAlertCheck;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.home.service.RenewalHomeService;
import com.iortatechnxt.brokerverse.renewal.retention.RenewalRetentionProvider;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Renewal wave R2: every Renewal report runs with the criteria and exports, the status summary has
 * its 34 counters, the home figures, the alert check and the retention provider answer.
 */
@IntegrationTest
class RenewalReportsIT {

  private static final List<String> CODES =
      List.of(
          "RNW-EXPIRY-LIST",
          "RNW-STATUS",
          "RNW-LISTING",
          "RNW-INSURER-EXTRACT",
          "RNW-RA-DISPATCH",
          "RNW-SANITATION",
          "RNW-DECISIONS",
          "RNW-LAMD-MATCH",
          "RNW-WORKLOAD",
          "RNW-GOLIVE",
          "RNW-PACKAGE-REMAP");

  @Autowired private RenewalFixtures fx;
  @Autowired private ReportService reports;
  @Autowired private RenewalHomeService home;
  @Autowired private RenewalAlertCheck alerts;
  @Autowired private RenewalRetentionProvider retention;
  @Autowired private AsUser as;

  private Map<String, String> params(Map<String, String> extra) {
    Map<String, String> p = new HashMap<>();
    p.put("companyId", fx.company().toString());
    p.put("expiryFrom", "2026-01-01");
    p.put("expiryTo", "2030-12-31");
    p.putAll(extra);
    return p;
  }

  private static List<ReportRow> details(ReportResult result) {
    return result.rows().stream().filter(r -> r.kind() == RowKind.DETAIL).toList();
  }

  @Test
  void everyReportRunsAndExportsWithTheCriteria() {
    RenewalCandidate c = fx.unassignedRetail();
    for (String code : CODES) {
      ReportResult result = as.run(TL, () -> reports.run(code, params(Map.of())));
      assertThat(result.code()).isEqualTo(code);
      assertThat(
              as.run(TL, () -> reports.export(code, params(Map.of()), ExportFormat.XLSX)).content())
          .as(code)
          .isNotEmpty();
      ReportResult filtered =
          as.run(
              TL,
              () ->
                  reports.run(
                      code,
                      params(Map.of("risk", "!ZZZ99", "stage", "UNASSIGNED,FOR_DISPOSITION"))));
      assertThat(filtered.code()).isEqualTo(code);
    }
    ReportResult list = as.run(TL, () -> reports.run("RNW-EXPIRY-LIST", params(Map.of())));
    assertThat(details(list)).anyMatch(r -> c.getRenewalRef().equals(r.cells().get("renewal_ref")));
  }

  @Test
  void theStatusSummaryHasItsThirtyFourCounters() {
    fx.unassignedRetail();
    ReportResult summary =
        as.run(TL, () -> reports.run("RNW-STATUS", params(Map.of("view", "SUMMARY"))));
    assertThat(details(summary)).hasSize(34);
    ReportResult processing =
        as.run(TL, () -> reports.run("RNW-STATUS", params(Map.of("variant", "PROCESSING"))));
    assertThat(processing.columns()).hasSize(32);
  }

  @Test
  void homeAlertsAndRetentionAnswer() {
    RenewalCandidate c = fx.unassignedRetail();
    RenewalHomeService.Home figures = as.run(TL, () -> home.home(fx.company()));
    assertThat(figures.stages()).isNotEmpty();
    assertThat(alerts.evaluate(c.getExpiryDate().minusDays(10)))
        .anyMatch(s -> s.facts().entityId().equals(c.getRenewalRef()));
    assertThat(
            retention.countEligible(
                new RetentionCriteria(Set.of("RENEWED", "CLOSED"), LocalDate.of(2099, 1, 1))))
        .isNotNegative();
  }
}
