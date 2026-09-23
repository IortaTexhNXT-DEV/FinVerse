package com.iortatechnxt.finverse.receivables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.receivables.api.dto.PdcRequest;
import com.iortatechnxt.finverse.receivables.api.dto.StatementImportRequest;
import com.iortatechnxt.finverse.receivables.domain.AllocationMethod;
import com.iortatechnxt.finverse.receivables.domain.PayerType;
import com.iortatechnxt.finverse.receivables.domain.ReceiptMode;
import com.iortatechnxt.finverse.receivables.service.BankStatementService;
import com.iortatechnxt.finverse.receivables.service.PdcService;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.ReportRow;
import com.iortatechnxt.finverse.report.core.ReportService;
import com.iortatechnxt.finverse.report.core.RowKind;
import com.iortatechnxt.finverse.report.render.ExportFormat;
import com.iortatechnxt.finverse.subledger.domain.OpenItem;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ReceivablesReportsIT {

  private static final String AS_OF = "2026-09-30";
  private static final LocalDate APRIL = LocalDate.of(2026, 4, 6);

  @Autowired private ReportService reports;
  @Autowired private ReceivablesFixtures fx;
  @Autowired private PdcService pdcs;
  @Autowired private BankStatementService statements;
  @Autowired private AsUser as;

  @BeforeEach
  void data() {
    OpenItem note = fx.debitNote("C-000201", APRIL, "20000.00", "PHP");
    fx.debitNote("C-000202", APRIL, "900.00", "USD");
    fx.approved(
        fx.request(
                PayerType.POLICYHOLDER,
                "C-000201",
                APRIL.plusDays(2),
                ReceiptMode.CHEQUE,
                "25000.00",
                "1112")
            .with(AllocationMethod.MANUAL, ReceivablesFixtures.alloc(note, "12000.00")));
    as.run(
        "accountant",
        () ->
            pdcs.register(
                new PdcRequest(
                    fx.company(),
                    fx.branch(),
                    APRIL,
                    "C-000201",
                    "FND",
                    "RPT" + System.nanoTime() % 100_000,
                    APRIL.plusMonths(7),
                    "BPI",
                    "PHP",
                    new BigDecimal("5000.00"),
                    "1111",
                    note.getId(),
                    null)));
  }

  private Map<String, String> params(String... pairs) {
    Map<String, String> p = new HashMap<>();
    p.put("companyId", fx.company().toString());
    for (int i = 0; i < pairs.length; i += 2) {
      p.put(pairs[i], pairs[i + 1]);
    }
    return p;
  }

  private ReportResult run(String code, Map<String, String> params) {
    return as.run("fmanager", () -> reports.run(code, params));
  }

  private ReportResult runAndExport(String code, Map<String, String> params) {
    ReportResult result = run(code, params);
    assertThat(result.code()).isEqualTo(code);
    for (ExportFormat format : ExportFormat.values()) {
      assertThat(as.run("fmanager", () -> reports.export(code, params, format)).content())
          .isNotEmpty();
    }
    return result;
  }

  private static long details(ReportResult r) {
    return r.rows().stream().filter(row -> row.kind() == RowKind.DETAIL).count();
  }

  @Test
  void ageingAndStatementReportsRunAndExport() {
    ReportResult detail = runAndExport("FIN-AR-AGE-DET", params("asOfDate", AS_OF));
    assertThat(details(detail)).isPositive();
    assertThat(detail.columns()).extracting(c -> c.label()).contains("0-30", "Over 120");
    ReportResult summary =
        runAndExport(
            "FIN-AR-AGE-SUM",
            params(
                "asOfDate",
                AS_OF,
                "orderBy",
                "DOCUMENT_DATE",
                "currencyBasis",
                "FOREIGN",
                "ageingSlots",
                "15,45,400",
                "ledger",
                "POLICYHOLDER",
                "partyFrom",
                "C",
                "partyTo",
                "D"));
    assertThat(summary.columns()).extracting(c -> c.label()).contains("0-15", "Over 400");
    assertThat(summary.rows()).anyMatch(r -> r.kind() == RowKind.TOTAL);
    assertThat(details(runAndExport("FIN-AR-AGE-DIV", params("asOfDate", AS_OF)))).isPositive();
    ReportResult soo = runAndExport("FIN-AR-SOO", params("asOfDate", AS_OF));
    assertThat(soo.rows()).extracting(ReportRow::label).contains("Net Balance", "PDC Cheques");
    assertThat(soo.rows())
        .anyMatch(r -> "Balance Net of PDC".equals(r.label()) && r.cells().containsKey("balance"));
    ReportResult fc = runAndExport("FIN-AR-SOO-FC", params("asOfDate", AS_OF, "currency", "USD"));
    assertThat(details(fc)).isPositive();
    ReportResult soa =
        runAndExport("FIN-ARAP-SOA-MATCH", params("fromDate", "2026-01-01", "toDate", AS_OF));
    assertThat(soa.rows())
        .extracting(ReportRow::label)
        .contains("Matched Details", "Unmatched Details");
    runAndExport(
        "FIN-ARAP-SOA-MATCH",
        params(
            "fromDate",
            "2026-01-01",
            "toDate",
            AS_OF,
            "currencyBasis",
            "BASE",
            "ledger",
            "INTERMEDIARY"));
    assertThatThrownBy(
            () -> run("FIN-AR-AGE-SUM", params("asOfDate", AS_OF, "ageingSlots", "60,30")))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void chequeAndBankReportsRunAndExport() {
    ReportResult received =
        runAndExport("FIN-AR-CHQ-RCPT", params("fromDate", "2026-01-01", "toDate", AS_OF));
    assertThat(details(received)).isPositive();
    runAndExport(
        "FIN-AR-CHQ-RCPT",
        params(
            "fromDate",
            "2026-01-01",
            "toDate",
            AS_OF,
            "bankAccount",
            "1112",
            "partyCode",
            "C-000201"));
    ReportResult undeposited = runAndExport("FIN-AR-CHQ-UNDEP", params("asOfDate", AS_OF));
    assertThat(details(undeposited)).isPositive();
    runAndExport("FIN-BRS-UNREC-BOOK", params("asOfDate", AS_OF));
    runAndExport(
        "FIN-BRS-UNREC-BOOK", params("asOfDate", AS_OF, "bankAccount", "1112", "detail", "false"));
    as.run(
        "accountant",
        () ->
            statements.importStatement(
                new StatementImportRequest(
                    fx.company(),
                    "1102",
                    "RPT-" + System.nanoTime(),
                    null,
                    "date,description,reference,debit,credit\n"
                        + "2026-04-01,Charges,SC,10.00,\n2026-04-02,Interest,INT,,4.00\n",
                    BigDecimal.ZERO)));
    ReportResult bank =
        runAndExport("FIN-BRS-UNREC-BANK", params("asOfDate", AS_OF, "bankAccount", "1102"));
    assertThat(details(bank)).isGreaterThanOrEqualTo(2);
    assertThat(
            details(runAndExport("FIN-BRS-STMT", params("asOfDate", AS_OF, "bankAccount", "1102"))))
        .isGreaterThanOrEqualTo(2);
    ReportResult brs =
        runAndExport("FIN-BRS-STMT", params("asOfDate", AS_OF, "bankAccount", "1112"));
    assertThat(brs.rows())
        .extracting(ReportRow::label)
        .contains("Balance as per Book", "Unexplained Difference");
  }

  @Test
  void pdcReportsRunAndExport() {
    for (String code :
        List.of(
            "FIN-PDC-RCV-ONHAND",
            "FIN-PDC-RCV-DUEBANK",
            "FIN-PDC-RCV-ONHAND-DDB",
            "FIN-PDC-RCV-DUEBANK-DDB")) {
      runAndExport(code, params("asOfDate", AS_OF, "divisionFrom", "HO", "divisionTo", "HO"));
    }
    assertThat(details(runAndExport("FIN-PDC-RCV-ONHAND", params("asOfDate", AS_OF)))).isPositive();
    assertThat(
            details(
                runAndExport(
                    "FIN-PDC-RCV-PERIOD", params("fromDate", "2026-01-01", "toDate", AS_OF))))
        .isPositive();
    ReportResult ddb =
        runAndExport(
            "FIN-PDC-RCV-PERIOD-DDB",
            params(
                "fromDate", "2026-01-01", "toDate", AS_OF, "bankFrom", "1111", "bankTo", "1111"));
    assertThat(ddb.rows()).anyMatch(r -> r.kind() == RowKind.GROUP_HEADER);
    assertThat(
            details(run("FIN-PDC-RCV-ONHAND-DDB", params("asOfDate", AS_OF, "partyFrom", "ZZZ"))))
        .isZero();
  }
}
