package com.iortatechnxt.brokerverse.nbreport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.nbreport.domain.SalesTarget;
import com.iortatechnxt.brokerverse.nbreport.domain.UnitLevel;
import com.iortatechnxt.brokerverse.nbreport.service.NbDashboard;
import com.iortatechnxt.brokerverse.nbreport.service.NbDashboardService;
import com.iortatechnxt.brokerverse.nbreport.service.ProductionService;
import com.iortatechnxt.brokerverse.nbreport.service.ReportVariantService;
import com.iortatechnxt.brokerverse.nbreport.service.SalesTargetService;
import com.iortatechnxt.brokerverse.nbreport.service.UnitProduction;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** New Business reports, dashboard, production targets and report variants (W4). */
@IntegrationTest
class NbReportsIT {

  private static final List<String> NB_REPORTS =
      List.of(
          "NB-PLC-UPDATE",
          "NB-ACC-STATUS",
          "NB-STAGE-OUTCOME",
          "NB-PLC-SUMMARY",
          "NB-CLPC-BILLING",
          "NB-PAY-MATCH",
          "NB-PRODUCTION",
          "NB-BOOKED-REG",
          "NB-SI-REG",
          "NB-DISPATCH");

  @Autowired private ReportService reports;
  @Autowired private NbDashboardService dashboard;
  @Autowired private ProductionService production;
  @Autowired private SalesTargetService targets;
  @Autowired private ReportVariantService variants;
  @Autowired private TestData data;
  @Autowired private AsUser as;

  private Map<String, String> params(String... pairs) {
    Map<String, String> m = new HashMap<>();
    m.put("companyId", data.company().getId().toString());
    m.put("from", "2026-01-01");
    m.put("to", "2026-12-31");
    for (int i = 0; i < pairs.length; i += 2) {
      m.put(pairs[i], pairs[i + 1]);
    }
    return m;
  }

  private static List<ReportRow> details(ReportResult result) {
    return result.rows().stream().filter(r -> r.kind() == RowKind.DETAIL).toList();
  }

  @Test
  void everyNewBusinessReportRunsAndExportsInEveryFormat() {
    as.run(
        "proctl",
        () -> {
          for (String code : NB_REPORTS) {
            assertThat(reports.run(code, params()).code()).isEqualTo(code);
            for (ExportFormat format : ExportFormat.values()) {
              var file = reports.export(code, params(), format);
              assertThat(file.content()).as(code + " " + format).isNotEmpty();
              assertThat(file.fileName()).isEqualTo(code + "." + format.extension());
            }
          }
          return null;
        });
  }

  @Test
  void theCatalogueIsFilteredByPermission() {
    List<String> ao =
        as.run("ao", () -> reports.catalogue().stream().map(ReportMetadata::code).toList());
    assertThat(ao).contains("NB-PLC-UPDATE", "NB-ACC-STATUS", "NB-BOOKED-REG", "NB-DISPATCH");
    assertThat(ao).doesNotContain("NB-PRODUCTION", "NB-CLPC-BILLING", "NB-SI-REG");
    List<ReportMetadata> tl = as.run("mkttl", reports::catalogue);
    assertThat(tl)
        .filteredOn(m -> m.code().startsWith("NB-") && !"NB-KYC-DUE".equals(m.code()))
        .extracting(ReportMetadata::category)
        .containsOnly(ReportCategory.NEW_BUSINESS);
    assertThat(tl).extracting(ReportMetadata::code).contains("NB-PRODUCTION");
  }

  @Test
  void theIndividualPlacementUpdateCoversOneAccount() {
    ReportResult result =
        as.run("ao", () -> reports.run("NB-PLC-UPDATE", params("arn", "arn-2026-940003")));
    assertThat(details(result))
        .isNotEmpty()
        .allSatisfy(r -> assertThat(r.cells()).containsEntry("arn", "ARN-2026-940003"));
  }

  @Test
  void theAccountStatusReportFlagsSlaBreachesAndStalledAccounts() {
    ReportResult all = as.run("ao", () -> reports.run("NB-ACC-STATUS", params()));
    assertThat(details(all)).isNotEmpty();
    assertThat(all.notes()).anyMatch(n -> n.contains("NB_STALLED_DAYS"));
    ReportResult breaches =
        as.run("ao", () -> reports.run("NB-ACC-STATUS", params("exceptions", "SLA_BREACH")));
    assertThat(details(breaches))
        .isNotEmpty()
        .allSatisfy(r -> assertThat(r.cells()).containsEntry("breached", "Yes"));
    ReportResult stalled =
        as.run("ao", () -> reports.run("NB-ACC-STATUS", params("exceptions", "STALLED")));
    assertThat(details(stalled))
        .allSatisfy(r -> assertThat(r.cells()).containsEntry("stalled", "Yes"));
  }

  @Test
  void theStageReportListsEveryAccountStage() {
    ReportResult result = as.run("proc", () -> reports.run("NB-STAGE-OUTCOME", params()));
    assertThat(details(result))
        .extracting(r -> r.cells().get("stage"))
        .contains("Draft", "Placed with insurer", "Booked");
  }

  @Test
  void productionIsComparedWithTargetsProRata() {
    String officer = "t" + UUID.randomUUID().toString().substring(0, 8);
    long company = data.company().getId();
    as.run(
        "badmin",
        () ->
            targets.save(
                company,
                new SalesTarget.Unit(
                    UnitLevel.OFFICER,
                    officer,
                    LocalDate.parse("2031-01-01"),
                    LocalDate.parse("2031-01-31")),
                new SalesTarget.Values(31, new BigDecimal("3100"), new BigDecimal("310"))));
    UnitProduction unit =
        production
            .production(
                company,
                UnitLevel.OFFICER,
                LocalDate.parse("2031-01-01"),
                LocalDate.parse("2031-01-10"))
            .stream()
            .filter(u -> u.code().equals(officer))
            .findFirst()
            .orElseThrow();
    assertThat(unit.targetCount()).isEqualTo(10);
    assertThat(unit.targetPremium()).isEqualByComparingTo("1000.00");
    assertThat(unit.premium()).isEqualByComparingTo("0");
    assertThat(unit.achievement()).isEqualByComparingTo("0");
    assertThatThrownBy(
            () ->
                as.run(
                    "badmin",
                    () ->
                        targets.save(
                            company,
                            new SalesTarget.Unit(
                                UnitLevel.TEAM,
                                officer,
                                LocalDate.parse("2031-02-01"),
                                LocalDate.parse("2031-01-01")),
                            new SalesTarget.Values(1, BigDecimal.ONE, BigDecimal.ONE))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("period end");
  }

  @Test
  void theDashboardSummarisesNewBusiness() {
    NbDashboard d =
        as.run(
            "mkttl",
            () -> dashboard.dashboard(data.company().getId(), LocalDate.parse("2026-09-20")));
    assertThat(d.funnel()).hasSize(7);
    assertThat(d.accounts()).isNotEmpty().allSatisfy(c -> assertThat(c.count()).isPositive());
    assertThat(d.requests()).extracting(NbDashboard.StatusCount::group).contains("QUOTATION");
    assertThat(d.ageing()).isNotEmpty();
    assertThat(d.production()).extracting(UnitProduction::code).contains("T-CBG1");
    assertThat(d.booked().premium()).isNotNull();
  }

  @Test
  void reportVariantsAreSavedSharedAndDeletedByTheirOwner() {
    String name = "Mine " + UUID.randomUUID();
    var saved =
        as.run(
            "ao",
            () ->
                variants.save(
                    "NB-ACC-STATUS",
                    name,
                    Map.of("exceptions", "STALLED", "companyId", "1", "unknown", "x"),
                    false));
    assertThat(variants.parameters(saved)).containsOnly(Map.entry("exceptions", "STALLED"));
    assertThat(as.run("ao2", () -> variants.visible("NB-ACC-STATUS")))
        .noneMatch(v -> v.getId().equals(saved.getId()));
    as.run("ao", () -> variants.save("NB-ACC-STATUS", name, Map.of("status", "PLACED"), true));
    assertThat(as.run("ao2", () -> variants.visible("NB-ACC-STATUS")))
        .anyMatch(v -> v.getId().equals(saved.getId()));
    assertThatThrownBy(() -> as.run("ao2", () -> runDelete(saved.getId())))
        .isInstanceOf(BusinessRuleException.class);
    as.run("ao", () -> runDelete(saved.getId()));
    assertThat(as.run("ao", () -> variants.visible("NB-ACC-STATUS")))
        .noneMatch(v -> v.getId().equals(saved.getId()));
    assertThatThrownBy(
            () -> as.run("ao", () -> variants.save("NB-PRODUCTION", "x", Map.of(), false)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("not available");
  }

  private Void runDelete(Long id) {
    variants.delete(id);
    return null;
  }
}
