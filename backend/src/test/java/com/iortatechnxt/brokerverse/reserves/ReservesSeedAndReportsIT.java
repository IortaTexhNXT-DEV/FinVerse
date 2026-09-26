package com.iortatechnxt.brokerverse.reserves;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.reserves.domain.RunStatus;
import com.iortatechnxt.brokerverse.reserves.domain.ValuationRun;
import com.iortatechnxt.brokerverse.reserves.seed.ReservesSeedData;
import com.iortatechnxt.brokerverse.reserves.service.ReserveAnalysisService;
import com.iortatechnxt.brokerverse.reserves.service.ReserveParameterService;
import com.iortatechnxt.brokerverse.reserves.service.ReserveSummary;
import com.iortatechnxt.brokerverse.reserves.service.TakafulSettingService;
import com.iortatechnxt.brokerverse.reserves.service.ValuationRunService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.underwriting.UwFixtures;
import com.iortatechnxt.brokerverse.underwriting.domain.Product;
import com.iortatechnxt.brokerverse.underwriting.service.ProductService;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the reserves seed data loader (normally only with the seed profile) inside a rolled back
 * transaction, checks the runs it creates and its idempotency, and runs and exports every actuarial
 * report on that data.
 */
@IntegrationTest
@Transactional
class ReservesSeedAndReportsIT {

  private static final String[] CODES = {
    "PGIBR072", "PGIBR079", "PGIBR080", "PGIBR074", "RSV-SUMMARY", "RSV-TRIANGLE", "RSV-UPR-MOVE"
  };

  @Autowired private OrganizationService organization;
  @Autowired private ProductService products;
  @Autowired private ReserveParameterService parameters;
  @Autowired private TakafulSettingService takaful;
  @Autowired private ValuationRunService runs;
  @Autowired private ReserveAnalysisService analysis;
  @Autowired private UserDetailsService users;
  @Autowired private ReportService reports;
  @Autowired private ReserveFixtures fx;
  @Autowired private UwFixtures uw;
  @Autowired private AsUser as;

  private ReservesSeedData loader() {
    return new ReservesSeedData(organization, products, parameters, takaful, runs, users);
  }

  private Map<String, String> params(String... pairs) {
    Map<String, String> p = new HashMap<>();
    p.put("companyId", fx.companyId().toString());
    p.put("valuationDate", "2026-08-31");
    p.put("businessLine", "FIRE");
    for (int i = 0; i < pairs.length; i += 2) {
      p.put(pairs[i], pairs[i + 1]);
    }
    return p;
  }

  private static long details(ReportResult r) {
    return r.rows().stream().filter(row -> row.kind() == RowKind.DETAIL).count();
  }

  @Test
  void seedRunsAreBuiltIdempotentlyAndEveryReportRunsAndExports() {
    Product product = uw.product("FIRE", false);
    uw.issue(uw.brokerRequest(product), UwFixtures.ISSUE);
    uw.issue(uw.brokerRequest(uw.product("PA", false)), UwFixtures.ISSUE);
    loader().load(fx.companyId());

    List<ValuationRun> list = runs.list(fx.companyId());
    assertThat(list).filteredOn(r -> r.getStatus() == RunStatus.POSTED).hasSize(8);
    assertThat(runs.forMonth(fx.companyId(), LocalDate.of(2026, 9, 30)))
        .map(ValuationRun::getStatus)
        .contains(RunStatus.PENDING_APPROVAL);
    assertThat(parameters.list(fx.companyId())).hasSizeGreaterThanOrEqualTo(8);
    assertThat(takaful.inForce(fx.companyId())).isPresent();

    loader().load(fx.companyId());
    assertThat(runs.list(fx.companyId())).hasSameSizeAs(list);

    ReserveSummary summary = analysis.summary(fx.companyId(), LocalDate.of(2026, 8, 31));
    assertThat(summary.previousDate()).isEqualTo(LocalDate.of(2026, 7, 31));
    assertThat(summary.rows()).anyMatch(r -> r.reserve().equals("UPR"));

    as.run(
        "fmanager",
        () -> {
          everyReportRunsAndExports();
          return null;
        });
  }

  private void everyReportRunsAndExports() {
    assertThat(reports.catalogue()).extracting("code").contains((Object[]) CODES);
    for (String code : CODES) {
      ReportResult result = reports.run(code, params());
      assertThat(result.rows()).as(code).isNotNull();
      for (ExportFormat format : ExportFormat.values()) {
        assertThat(reports.export(code, params(), format).content()).as(code).isNotEmpty();
      }
    }
    assertThat(details(reports.run("PGIBR072", params()))).isPositive();
    assertThat(details(reports.run("PGIBR072", params("option", "EARNED", "level", "SUMMARY"))))
        .isPositive();
    assertThat(details(reports.run("PGIBR079", params("summary", "Y", "businessLine", "PA"))))
        .isPositive();
    assertThat(details(reports.run("RSV-SUMMARY", params()))).isPositive();
    assertThat(details(reports.run("RSV-UPR-MOVE", params()))).isPositive();
    assertThat(details(reports.run("RSV-TRIANGLE", params("basis", "PAID", "period", "QUARTER"))))
        .isEqualTo(5);
    assertThat(reports.run("PGIBR074", params("posting", "POSTED", "fromDate", "2026-01-01")))
        .isNotNull();
    assertThat(reports.run("PGIBR080", params("valuationDate", "2026-10-15"))).isNotNull();
  }
}
