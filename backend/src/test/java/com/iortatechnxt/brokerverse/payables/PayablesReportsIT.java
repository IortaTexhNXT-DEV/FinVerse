package com.iortatechnxt.brokerverse.payables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.payables.service.PayablesSeedData;
import com.iortatechnxt.brokerverse.payables.service.PayablesSeedDataRunner;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;

@IntegrationTest
class PayablesReportsIT {

  private static final String[] PERIOD_REPORTS = {
    "FIN-PC-REIMB",
    "FIN-PDC-ISS-PERIOD",
    "FIN-PDC-ISS-PERIOD-DDB",
    "FIN-PDC-CONF-AUDIT-DDB",
    "FIN-AP-VOUCHER"
  };
  private static final String[] AS_OF_REPORTS = {
    "FIN-AP-AGE-SUM",
    "FIN-AP-AGE-DET",
    "FIN-AP-SOP",
    "FIN-AP-SUPOS",
    "FIN-PC-PENDING",
    "FIN-PDC-ISS-DUEPAY",
    "FIN-PDC-ISS-DUEPAY-DDB"
  };

  @Autowired private ReportService reports;
  @Autowired private PayablesSeedData seedData;
  @Autowired private TestData data;

  @BeforeEach
  void loadSeedData() {
    new PayablesSeedDataRunner(seedData).run(null);
    assertThat(seedData.loadIfMissing()).isFalse();
  }

  private Map<String, String> params(String... pairs) {
    Map<String, String> p = new HashMap<>();
    p.put("companyId", data.company().getId().toString());
    for (int i = 0; i < pairs.length; i += 2) {
      p.put(pairs[i], pairs[i + 1]);
    }
    return p;
  }

  private void runAndExport(String code, Map<String, String> p) {
    ReportResult result = reports.run(code, p);
    assertThat(result.code()).isEqualTo(code);
    assertThat(result.rows()).as(code + " has rows").isNotEmpty();
    for (ExportFormat format : ExportFormat.values()) {
      assertThat(reports.export(code, p, format).content()).isNotEmpty();
    }
  }

  @Test
  @WithUserDetails("fmanager")
  void everyPayablesReportRunsOnSeedDataAndExportsInAllFormats() {
    for (String code : PERIOD_REPORTS) {
      runAndExport(code, params("fromDate", "2026-01-01", "toDate", "2026-09-30"));
    }
    for (String code : AS_OF_REPORTS) {
      runAndExport(code, params("asOfDate", "2026-09-23"));
    }
    runAndExport(
        "FIN-BRS-PAYNOTIFY",
        params(
            "bankAccountCode",
            "BPI-SA",
            "fromDate",
            "2026-06-01",
            "toDate",
            "2026-06-30",
            "includeCheques",
            "true"));
  }

  @Test
  @WithUserDetails("fmanager")
  void creditorAgeingSupportsOptionsAndShowsSeedSuppliers() {
    ReportResult summary =
        reports.run(
            "FIN-AP-AGE-SUM",
            params(
                "asOfDate",
                "2026-09-23",
                "orderBy",
                "DOCUMENT_DATE",
                "currency",
                "FOREIGN",
                "partyType",
                "ALL_CREDITORS",
                "slot1",
                "15",
                "slot2",
                "45",
                "partyFrom",
                "S-0001",
                "partyTo",
                "S-0003",
                "mainAccountFrom",
                "2501",
                "mainAccountTo",
                "2501"));
    assertThat(summary.columns()).extracting("label").contains("0-15", "16-45", "Over 45");
    assertThat(summary.rows())
        .filteredOn(r -> r.kind() == RowKind.DETAIL)
        .extracting(r -> r.cells().get("partyCode"))
        .contains("S-0001", "S-0002", "S-0003");

    ReportResult statement =
        reports.run(
            "FIN-AP-SOP",
            params("asOfDate", "2026-09-23", "partyFrom", "S-0001", "partyTo", "S-0001"));
    assertThat(statement.rows())
        .filteredOn(r -> r.kind() == RowKind.DETAIL)
        .anySatisfy(r -> assertThat(r.cells()).containsKey("pdcNo"));

    ReportResult due =
        reports.run("FIN-PDC-ISS-DUEPAY", params("asOfDate", "2026-09-23", "dueWithinDays", "30"));
    assertThat(due.rows()).filteredOn(r -> r.kind() == RowKind.DETAIL).isNotEmpty();

    assertThatThrownBy(
            () ->
                reports.run(
                    "FIN-AP-AGE-SUM",
                    params("asOfDate", "2026-09-23", "slot1", "60", "slot2", "30")))
        .isInstanceOf(BusinessRuleException.class);
  }
}
