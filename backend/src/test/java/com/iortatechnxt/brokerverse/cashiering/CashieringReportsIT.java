package com.iortatechnxt.brokerverse.cashiering;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The Cashiering reports (CSHID.017/018/023, BRQID.006, CSHID.027): every report runs and exports
 * in every format, viewing and exporting follow the Operations permissions, and the cashiering jobs
 * run.
 */
@IntegrationTest
class CashieringReportsIT {

  private static final List<String> CODES =
      List.of(
          "CSH-APPLIED-PREM",
          "CSH-APPLIED-COMM",
          "CSH-PDC-WAREHOUSE",
          "CSH-MINBAL-EXCESS",
          "CSH-CANCELLED-OR",
          "CSH-CANCELLED-AR",
          "CSH-CHECK-PICKUP",
          "CSH-PRIORITY-POSTED",
          "CSH-UNAPPLIED-COMM-MANCOM",
          "CSH-UNAPPLIED-COMM-YTD",
          "CSH-MINBAL-PREMIUM",
          "CSH-MINBAL-COMMISSION",
          "CSH-DAILY-CASH-REC",
          "CSH-ADVANCE-PAYMENT",
          "CSH-PAYMENT-REVERSAL",
          "CSH-DIRECT-PAYMENT",
          "CSH-REINSTATEMENT-MON",
          "CSH-REAPPLICATION",
          "CSH-CERT-OF-PAYMENT",
          "CSH-REINSTATEMENT",
          "CSH-CWT",
          "CSH-AR-OUTSTANDING",
          "CSH-BATCH-RUN",
          "CSH-2307-TXN");

  @Autowired private ReportService reports;
  @Autowired private CashFixtures fx;
  @Autowired private List<ManagedJob> jobs;
  @Autowired private AsUser as;

  private Map<String, String> params() {
    Map<String, String> m = new HashMap<>();
    m.put("companyId", fx.company().toString());
    m.put("from", "2026-01-01");
    m.put("to", "2027-12-31");
    return m;
  }

  @Test
  void everyCashieringReportRunsAndExportsInEveryFormat() {
    fx.pay(fx.motorInvoice().getInvoiceNo(), new BigDecimal("100.00"));
    as.run(
        "cashtl",
        () -> {
          List<String> catalogue = reports.catalogue().stream().map(ReportMetadata::code).toList();
          assertThat(catalogue).containsAll(CODES);
          for (String code : CODES) {
            assertThat(reports.run(code, params()).code()).isEqualTo(code);
            for (ExportFormat format : ExportFormat.values()) {
              assertThat(reports.export(code, params(), format).content())
                  .as(code + " " + format)
                  .isNotEmpty();
            }
          }
          assertThat(reports.run("CSH-APPLIED-PREM", params()).rows()).isNotEmpty();
          return null;
        });
  }

  @Test
  void theCashieringJobsRun() {
    for (String name :
        List.of(
            "PREBOOKED_REMATCH", "PAYMENT_AUTOMATCH", "PDC_MATURITY", "MINIMAL_BALANCE_SWEEP")) {
      ManagedJob job = jobs.stream().filter(j -> j.name().equals(name)).findFirst().orElseThrow();
      assertThat(job.cron()).isNotBlank();
      assertThat(job.description()).isNotBlank();
      assertThat(job.execute(LocalDate.now()).message()).isNotBlank();
    }
  }
}
