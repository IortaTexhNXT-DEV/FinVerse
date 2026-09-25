package com.iortatechnxt.brokerverse.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.brokerverse.opsledger.OpsLedgerFixtures;
import com.iortatechnxt.brokerverse.report.core.ExportOptions;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificate;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificate.Facts;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificateLine;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificateLine.Kind;
import com.iortatechnxt.brokerverse.tax.domain.TaxPeriod;
import com.iortatechnxt.brokerverse.tax.service.ReceivedCertificateService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The received-certificate register and the new BIR outputs (DIS 2.11, FRBS 3.2.0 Appendix A VII):
 * a certificate posts TAX_CWT_CERT_RECEIVED and feeds the SAWT and the income tax worksheet; every
 * new output (MAP, 1604-E, SAWT, 0619-F, 1603, 1702-Q, 1702, books of accounts, IC broker
 * statement) is generated and exported to Excel and PDF by an FRBS user.
 */
@IntegrationTest
class BirOutputsIT {

  private static final String FRBS = "glofficer";
  private static final String TAX = "fmanager";
  private static final List<String> OUTPUTS =
      List.of(
          "TAX-MAP",
          "TAX-1604E",
          "TAX-SAWT",
          "TAX-0619F",
          "TAX-1603",
          "TAX-1702Q",
          "TAX-1702",
          "TAX-BOOK-GJ",
          "TAX-BOOK-PJ",
          "TAX-BOOK-SJ",
          "TAX-BOOK-CRB",
          "TAX-BOOK-CDB",
          "TAX-BOOK-SL",
          "IC-BROKER-ASBO");

  @Autowired private ReceivedCertificateService certificates;
  @Autowired private ReportService reports;
  @Autowired private JournalBatchRepository journals;
  @Autowired private OpsLedgerFixtures ops;
  @Autowired private AsUser as;

  private static final LocalDate TODAY = LocalDate.now();

  private Map<String, String> params() {
    Map<String, String> p = new HashMap<>();
    p.put("companyId", String.valueOf(ops.company()));
    p.put("year", String.valueOf(TODAY.getYear()));
    p.put("quarter", String.valueOf(TaxPeriod.quarterNumber(TODAY)));
    p.put("month", String.valueOf(TODAY.getMonthValue()));
    p.put("fromDate", TODAY.withDayOfYear(1).toString());
    p.put("toDate", TODAY.toString());
    return p;
  }

  private ReceivedCertificate record(String user, String agent) {
    TaxPeriod quarter = TaxPeriod.quarterOf(TODAY);
    return as.run(
        user,
        () ->
            certificates.record(
                ops.company(),
                new Facts(
                    "2307-" + BookingFixtures.token(),
                    agent,
                    "Agent " + agent,
                    "123-456-789-000",
                    quarter.from(),
                    quarter.to(),
                    TODAY,
                    "DISBURSEMENT",
                    "DV-T-" + agent,
                    null),
                List.of(
                    new ReceivedCertificateLine(
                        1,
                        Kind.COMMISSION,
                        "WC158",
                        "Commission",
                        new BigDecimal("10000.00"),
                        new BigDecimal("200.00")),
                    new ReceivedCertificateLine(
                        2,
                        Kind.INCENTIVE,
                        "WC158",
                        "Incentive",
                        new BigDecimal("5000.00"),
                        new BigDecimal("100.00")))));
  }

  @Test
  void aCertificateIsPostedListedInTheSawtAndCancelled() {
    String agent = "INS-" + BookingFixtures.token();
    ReceivedCertificate c = record(TAX, agent);
    assertThat(c.getTaxTotal()).isEqualByComparingTo("300.00");
    assertThat(c.getJournalBatchNo()).isNotNull();
    assertThat(journals.findByCompanyIdAndBatchNo(ops.company(), c.getJournalBatchNo()))
        .isPresent();
    assertThat(certificates.bySource("DISBURSEMENT", "DV-T-" + agent)).hasSize(1);
    ReportResult sawt = as.run(FRBS, () -> reports.run("TAX-SAWT", params()));
    assertThat(sawt.rows()).anyMatch(r -> ("Agent " + agent).equals(r.cells().get("party")));
    ReportResult itr = as.run(FRBS, () -> reports.run("TAX-1702Q", params()));
    assertThat(itr.rows()).anyMatch(r -> "70".equals(r.cells().get("code")));
    ReceivedCertificate cancelled = as.run(TAX, () -> certificates.cancel(c.getId(), "Duplicate"));
    assertThat(cancelled.getStatus()).isEqualTo(ReceivedCertificate.CANCELLED);
    assertThat(cancelled.getCancelJournalNo()).isNotNull();
    assertThatThrownBy(() -> as.run(TAX, () -> certificates.cancel(c.getId(), "Again")))
        .hasMessageContaining("already");
    assertThatThrownBy(() -> as.run(TAX, () -> certificates.cancel(c.getId(), " ")))
        .hasMessageContaining("reason");
  }

  @Test
  void aCertificateIsValidatedAndNotRecordedTwice() {
    String agent = "INS-" + BookingFixtures.token();
    ReceivedCertificate first = record(TAX, agent);
    Facts same =
        new Facts(
            first.getCertificateNo(),
            agent,
            "Agent",
            null,
            first.getPeriodFrom(),
            first.getPeriodTo(),
            TODAY,
            null,
            null,
            null);
    List<ReceivedCertificateLine> one =
        List.of(
            new ReceivedCertificateLine(
                1, Kind.COMMISSION, "WC158", null, BigDecimal.TEN, BigDecimal.ONE));
    assertThatThrownBy(() -> as.run(TAX, () -> certificates.record(ops.company(), same, one)))
        .hasMessageContaining(first.getCertificateNo());
    Facts future =
        new Facts(
            "X-" + BookingFixtures.token(),
            agent,
            "Agent",
            null,
            TODAY,
            TODAY,
            TODAY.plusDays(5),
            null,
            null,
            null);
    assertThatThrownBy(() -> as.run(TAX, () -> certificates.record(ops.company(), future, one)))
        .hasMessageContaining("future");
    Facts ok =
        new Facts(
            "Y-" + BookingFixtures.token(),
            agent,
            "Agent",
            null,
            TODAY,
            TODAY,
            TODAY,
            null,
            null,
            null);
    assertThatThrownBy(() -> as.run(TAX, () -> certificates.record(ops.company(), ok, List.of())))
        .hasMessageContaining("income payment");
    Facts noAgent = new Facts("Z", " ", "Agent", null, TODAY, TODAY, TODAY, null, null, null);
    assertThatThrownBy(() -> as.run(TAX, () -> certificates.record(ops.company(), noAgent, one)))
        .hasMessageContaining("withholding agent");
  }

  @Test
  void everyNewBirOutputIsGeneratedAndExportedToExcelAndPdf() {
    ops.motorInvoice();
    for (String code : OUTPUTS) {
      ReportResult r = as.run(FRBS, () -> reports.run(code, params()));
      assertThat(r.code()).isEqualTo(code);
      for (ExportFormat format : List.of(ExportFormat.XLSX, ExportFormat.PDF)) {
        assertThat(
                as.run(FRBS, () -> reports.export(code, params(), format, ExportOptions.NONE))
                    .content())
            .isNotEmpty();
      }
    }
    ReportResult asbo = as.run(FRBS, () -> reports.run("IC-BROKER-ASBO", params()));
    assertThat(asbo.rows()).isNotEmpty();
    Map<String, String> sl = params();
    sl.put("accountClass", "LIABILITY");
    assertThat(as.run(FRBS, () -> reports.run("TAX-BOOK-SL", sl)).rows()).isNotEmpty();
  }
}
