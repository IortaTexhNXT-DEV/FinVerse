package com.iortatechnxt.brokerverse.eb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

/**
 * The Employee Benefits reports (FR-EB-060 to 062): every report runs on the seed data with the
 * common parameters and exports to PDF, Excel and Word; the Renewal report lists the programme
 * whose renewal advice was sent, the TAT report measures the renewal advice, and only holders of
 * EB_REPORT_VIEW run them.
 */
@IntegrationTest
class EbReportsIT {

  private static final List<String> CODES =
      List.of(
          "EB-PRODUCTION",
          "EB-RENEWAL",
          "EB-PLACEMENT",
          "EB-NEW-BUSINESS",
          "EB-TAT",
          "EB-PENDING-ITEMS",
          "EB-FRANCHISE");

  @Autowired private ReportService reports;
  @Autowired private EbFixtures fx;
  @Autowired private AsUser as;

  private Map<String, String> params(Map<String, String> extra) {
    Map<String, String> p = new HashMap<>();
    p.put("companyId", fx.company().toString());
    p.put("from", "2020-01-01");
    p.put("to", "2030-12-31");
    p.putAll(extra);
    return p;
  }

  private static List<ReportRow> details(ReportResult result) {
    return result.rows().stream().filter(r -> r.kind() == RowKind.DETAIL).toList();
  }

  @Test
  void everyReportRunsAndExportsWithTheCommonFilters() {
    for (String code : CODES) {
      ReportResult result = as.run("ebmgmt", () -> reports.run(code, params(Map.of())));
      assertThat(result.code()).isEqualTo(code);
      for (ExportFormat format : List.of(ExportFormat.PDF, ExportFormat.XLSX, ExportFormat.DOCX)) {
        assertThat(as.run("ebmgmt", () -> reports.export(code, params(Map.of()), format)).content())
            .as(code + " " + format)
            .isNotEmpty();
      }
      ReportResult filtered =
          as.run(
              "ebtl",
              () ->
                  reports.run(
                      code,
                      params(
                          Map.of(
                              "team", "BDO",
                              "ao", "ebao",
                              "client", "Pacific",
                              "benefitLine", "HMO",
                              "insurer", "ins-mgic",
                              "businessType", "RENEWAL"))));
      assertThat(filtered.code()).isEqualTo(code);
    }
    assertThat(
            as.run("ebao", () -> reports.catalogue()).stream()
                .filter(m -> m.category() == ReportCategory.EMPLOYEE_BENEFITS)
                .count())
        .isGreaterThanOrEqualTo(CODES.size());
  }

  @Test
  void theRenewalReportShowsTheAdviceSentAndTheTatReportMeasuresIt() {
    ReportResult renewal =
        as.run("ebao", () -> reports.run("EB-RENEWAL", params(Map.of("team", "BDO"))));
    assertThat(details(renewal))
        .anySatisfy(
            r -> {
              assertThat(r.cells().get("programme").toString()).startsWith("EBP-2026-000001");
              assertThat(r.cells().get("ra_sent")).isNotNull();
              assertThat(r.cells().get("status")).isEqualTo("Ra sent");
            });
    ReportResult tat =
        as.run("ebao", () -> reports.run("EB-TAT", params(Map.of("activity", "RENEWAL_ADVICE"))));
    assertThat(details(tat))
        .isNotEmpty()
        .allSatisfy(r -> assertThat(r.cells().get("breach")).isEqualTo("No"));
    ReportResult pending =
        as.run("ebao", () -> reports.run("EB-PENDING-ITEMS", params(Map.of("status", "ALL"))));
    assertThat(details(pending)).isNotEmpty();
  }

  @Test
  void onlyHoldersOfTheEbReportPermissionRunThem() {
    assertThatThrownBy(
            () -> as.run("accountant", () -> reports.run("EB-RENEWAL", params(Map.of()))))
        .isInstanceOf(AccessDeniedException.class);
  }
}
