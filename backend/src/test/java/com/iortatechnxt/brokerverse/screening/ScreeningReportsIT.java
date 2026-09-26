package com.iortatechnxt.brokerverse.screening;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

/**
 * The eight compliance reports (SNSRP-901, 903; FR-SS-090, 092): each runs on the demo data with
 * its filters and exports to PDF, Excel and CSV (the STR register also to Word); the audit log
 * needs SCR_AUDIT_VIEW and an investigator runs none of them.
 */
@IntegrationTest
class ScreeningReportsIT {

  private static final String COMPLIANCE = "compoff";
  private static final List<String> CODES =
      List.of(
          "SCR-HIGH-RISK-CLIENTS",
          "SCR-CASE-STATUS",
          "SCR-SLA-BREACHES",
          "SCR-SANCTIONED-NAMES",
          "SCR-PEP-CLIENTS",
          "SCR-INGEST-ERRORS",
          "SCR-STR-REGISTER",
          "SCR-AUDIT-LOG");

  @Autowired private ReportService reports;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  private Map<String, String> params() {
    Map<String, String> p = new HashMap<>();
    p.put("companyId", String.valueOf(data.company().getId()));
    p.put("fromDate", LocalDate.now().minusYears(1).toString());
    p.put("toDate", LocalDate.now().plusDays(1).toString());
    p.put("asOfDate", LocalDate.now().plusDays(1).toString());
    return p;
  }

  @Test
  void everyComplianceReportRunsAndExports() {
    List<ReportMetadata> catalogue = as.run(COMPLIANCE, () -> reports.catalogue());
    assertThat(
            catalogue.stream()
                .filter(m -> m.category() == ReportCategory.COMPLIANCE)
                .map(ReportMetadata::code))
        .containsAll(CODES);
    for (String code : CODES) {
      ReportResult result = as.run(COMPLIANCE, () -> reports.run(code, params()));
      assertThat(result).as(code).isNotNull();
      for (ExportFormat format : List.of(ExportFormat.PDF, ExportFormat.XLSX, ExportFormat.CSV)) {
        assertThat(as.run(COMPLIANCE, () -> reports.export(code, params(), format)).content())
            .as(code + " " + format)
            .isNotEmpty();
      }
    }
    assertThat(
            as.run(
                    COMPLIANCE,
                    () -> reports.export("SCR-STR-REGISTER", params(), ExportFormat.DOCX))
                .content())
        .isNotEmpty();
  }

  @Test
  void theReportsFollowTheirFiltersAndPermissions() {
    Map<String, String> byUnit = params();
    byUnit.put("caseStatus", "INVESTIGATION");
    byUnit.put("marketingUnit", "CBG-NCR");
    ReportResult status = as.run(COMPLIANCE, () -> reports.run("SCR-CASE-STATUS", byUnit));
    assertThat(status.rows()).isNotEmpty();
    Map<String, String> byDisposition = params();
    byDisposition.put("disposition", "FALSE_POSITIVE");
    assertThat(as.run(COMPLIANCE, () -> reports.run("SCR-AUDIT-LOG", byDisposition)).rows())
        .isNotEmpty();
    Map<String, String> reversed = params();
    reversed.put("toDate", LocalDate.now().minusYears(2).toString());
    assertThatThrownBy(() -> as.run(COMPLIANCE, () -> reports.run("SCR-CASE-STATUS", reversed)))
        .isNotNull();
    assertThatThrownBy(() -> as.run("ucc", () -> reports.run("SCR-AUDIT-LOG", params())))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> as.run("investigator", () -> reports.run("SCR-CASE-STATUS", params())))
        .isInstanceOf(AccessDeniedException.class);
    assertThat(as.run("ucc", () -> reports.run("SCR-HIGH-RISK-CLIENTS", params())).rows())
        .isNotEmpty();
  }
}
