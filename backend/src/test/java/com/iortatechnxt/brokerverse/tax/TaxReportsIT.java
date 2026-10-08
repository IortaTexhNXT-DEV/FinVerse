package com.iortatechnxt.brokerverse.tax;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.tax.service.BirExportService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Every tax report runs and exports to PDF, Excel and CSV; the BIR list exports follow the relief
 * layout.
 */
@IntegrationTest
class TaxReportsIT {

  static final List<String> CODES =
      List.of(
          "TAX-VAT-2550Q",
          "TAX-SLS",
          "TAX-SLP",
          "TAX-EWT-1601EQ",
          "TAX-QAP",
          "TAX-2307-REG",
          "TAX-REMIT");

  private static final LocalDate DAY = LocalDate.of(2026, 8, 20);

  @Autowired private ReportService reports;
  @Autowired private BirExportService exports;
  @Autowired private TaxFixtures fixtures;
  @Autowired private AsUser as;

  @BeforeEach
  void scenario() {
    fixtures.masters();
    fixtures.supplierInvoice(DAY, "5000");
  }

  @Test
  void everyTaxReportRunsAndExportsInAllFormats() {
    as.run("fmanager", this::runAndExportEveryReport);
  }

  private Void runAndExportEveryReport() {
    assertThat(reports.catalogue()).extracting("code").containsAll(CODES);
    for (String code : CODES) {
      ReportResult result = reports.run(code, params());
      assertThat(result.code()).isEqualTo(code);
      for (ExportFormat format : ExportFormat.values()) {
        assertThat(reports.export(code, params(), format).content())
            .as(code + " " + format)
            .isNotEmpty();
      }
    }
    assertThat(reports.run("TAX-VAT-2550Q", params()).rows()).isNotEmpty();
    assertThat(reports.catalogue())
        .extracting("code")
        .doesNotContain("TAX-DST-2000", "TAX-PREMTAX", "IC-PREM-LOB", "IC-RBC");
    return null;
  }

  @Test
  void birListsFollowTheReliefLayout() {
    Long company = fixtures.companyId();
    String sls = text(exports.sales(company, 2026, 3).content());
    assertThat(sls).startsWith("H,S,\"000123456\"").doesNotContain("\r\nD,S,");
    String slp = text(exports.purchases(company, 2026, 3).content());
    assertThat(slp).startsWith("H,P,").contains("D,P,\"802111222\"");
    String qap = text(exports.alphalist(company, 2026, 3).content());
    assertThat(qap).startsWith("HQAP,H1601EQ,000123456,000,").contains("D1,1601EQ,");
    assertThat(qap.lines().reduce((a, b) -> b).orElseThrow()).startsWith("C1,1601EQ,000123456");
    assertThat(exports.sales(company, 2026, 3).fileName()).isEqualTo("000123456SLS2026Q3.csv");
  }

  private Map<String, String> params() {
    return Map.of(
        "companyId", fixtures.companyId().toString(),
        "fromDate", "2026-07-01",
        "toDate", "2026-09-30",
        "year", "2026",
        "quarter", "3");
  }

  private static String text(byte[] content) {
    return new String(content, StandardCharsets.UTF_8);
  }
}
