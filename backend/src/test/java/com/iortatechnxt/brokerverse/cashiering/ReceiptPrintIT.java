package com.iortatechnxt.brokerverse.cashiering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.FormText;
import com.iortatechnxt.brokerverse.cashiering.domain.PrintBatch;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptForm;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptSeries;
import com.iortatechnxt.brokerverse.cashiering.service.BatchPrintService;
import com.iortatechnxt.brokerverse.cashiering.service.CashReceiptService;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptFormService;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSearchService;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSearchService.PrintFilter;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSearchService.ReceiptCriteria;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSeriesService;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSeriesService.Numbering;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSeriesService.SeriesRequest;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.StoredDownloads;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Printing of ARs and ORs in the client's forms (Operations Cashiering FRS v3.2: FRS.CSH.02.03,
 * 02.04, 02.06): the certificate number at the first print, the REPRINT mark and print count, the
 * copy labels with the company's copy kept on the receipt, the ZIP of one PDF per receipt, the
 * skipped failures, the limit of 500, the print queue and re-print search, the form changed with
 * maker-checker, the series with its year and number format and the Certification of Payment series
 * number.
 */
@IntegrationTest
class ReceiptPrintIT {

  @Autowired private CashFixtures fx;
  @Autowired private BatchPrintService printing;
  @Autowired private ReceiptFormService forms;
  @Autowired private ReceiptSearchService search;
  @Autowired private ReceiptSeriesService series;
  @Autowired private CashReceiptService receipts;
  @Autowired private StoredDownloads downloads;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private static final String CASHIER = "cashier";

  private static String text(byte[] pdf) throws IOException {
    StringBuilder all = new StringBuilder();
    try (PdfReader reader = new PdfReader(pdf)) {
      PdfTextExtractor extractor = new PdfTextExtractor(reader);
      for (int page = 1; page <= reader.getNumberOfPages(); page++) {
        all.append(extractor.getTextFromPage(page)).append('\n');
      }
    }
    return all.toString();
  }

  private static int pages(byte[] pdf) throws IOException {
    try (PdfReader reader = new PdfReader(pdf)) {
      return reader.getNumberOfPages();
    }
  }

  private PrintBatch print(List<Long> ids, String copy) {
    return as.run(CASHIER, () -> printing.print(fx.company(), ids, "Selection", copy));
  }

  private Receipt paid() {
    OpsInvoice invoice = fx.motorInvoice();
    return fx.pay(invoice.getInvoiceNo(), new BigDecimal("1215.10")).receipt();
  }

  @Test
  void theFirstPrintGivesTheCertificateAndTheSecondIsMarkedReprint() throws IOException {
    Receipt ar = paid();

    PrintBatch first = print(List.of(ar.getId()), "CLIENT");
    PrintBatch.Line line = first.getLines().get(0);
    assertThat(line.isReprint()).isFalse();
    String month =
        BusinessClock.today(Clock.systemUTC()).format(DateTimeFormatter.ofPattern("MMyyyy"));
    assertThat(line.getCertificateNo()).matches("AC_[A-Z0-9]+_" + month + "_\\d{5}");
    String firstText = text(downloads.bytes(as.run(CASHIER, () -> printing.file(first.getId()))));
    assertThat(firstText)
        .contains("ACKNOWLEDGEMENT", ar.getReceiptNo(), "Client's Copy", "THE SUM OF", "TIN")
        .contains("One Thousand Two Hundred Fifteen Pesos and Ten Centavos")
        .contains("ACKNOWLEDGEMENT CERTIFICATE NO. " + line.getCertificateNo())
        .doesNotContain("REPRINT");

    PrintBatch second = print(List.of(ar.getId()), "CLIENT");
    assertThat(second.getLines().get(0).isReprint()).isTrue();
    assertThat(second.getLines().get(0).getCertificateNo()).isEqualTo(line.getCertificateNo());
    assertThat(text(downloads.bytes(as.run(CASHIER, () -> printing.file(second.getId())))))
        .contains("REPRINT");
    Receipt again = as.run(CASHIER, () -> receipts.get(ar.getId()));
    assertThat(again.getPrintedCount()).isEqualTo(2);
    assertThat(again.getCertificateNo()).isEqualTo(line.getCertificateNo());
  }

  @Test
  void bothCopiesArePrintedTheCompanyCopyIsKeptAndTheZipNamesEachReceipt() throws IOException {
    Receipt ar = paid();
    Receipt other = paid();

    PrintBatch batch = print(List.of(ar.getId(), other.getId()), "BOTH");

    byte[] merged = downloads.bytes(as.run(CASHIER, () -> printing.file(batch.getId())));
    assertThat(pages(merged)).isEqualTo(4);
    assertThat(text(merged)).contains("Client's Copy", " Copy");
    assertThat(as.run(CASHIER, () -> printing.documents(receipts.get(ar.getId()))))
        .singleElement()
        .satisfies(f -> assertThat(f.getFileName()).endsWith("_Company_Copy.pdf"));
    List<String> names = new ArrayList<>();
    byte[] zip = downloads.bytes(as.run(CASHIER, () -> printing.zipFile(batch.getId())));
    try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
      for (ZipEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
        names.add(e.getName());
      }
    }
    assertThat(names)
        .containsExactly("AR_" + ar.getReceiptNo() + ".pdf", "AR_" + other.getReceiptNo() + ".pdf");
  }

  @Test
  void aFailedReceiptIsSkippedAndMoreThanFiveHundredAreRefused() {
    Receipt ok = paid();
    Receipt cancelled = paid();
    as.run(
        CASHIER,
        () -> {
          jdbc.update(
              "update csh_receipt set status = 'CANCELLED' where id = ?", cancelled.getId());
          return null;
        });

    PrintBatch batch = print(List.of(ok.getId(), cancelled.getId()), "CLIENT");
    assertThat(batch.getFailedCount()).isEqualTo(1);
    PrintBatch skipped =
        as.run(CASHIER, () -> printing.skip(batch.getId(), List.of(cancelled.getId())));
    assertThat(skipped.getSkippedCount()).isEqualTo(1);
    assertThat(skipped.getFailedCount()).isZero();
    assertThat(skipped.getLines())
        .filteredOn(l -> l.getReceiptId().equals(cancelled.getId()))
        .extracting(PrintBatch.Line::getStatus)
        .containsExactly(PrintBatch.SKIPPED);

    List<Long> tooMany = new ArrayList<>(Collections.nCopies(501, ok.getId()));
    assertThatThrownBy(() -> print(tooMany, "CLIENT"))
        .hasMessage("Select between 1 and 500 receipts to print");
  }

  @Test
  void theQueueListsReceiptsNotPrintedAndReprintingFindsAPartOfTheNumber() {
    Receipt ar = paid();
    String part = ar.getReceiptNo().substring(ar.getReceiptNo().length() - 4);

    assertThat(find(new PrintFilter(ar.getBranchId(), false, false, null))).contains(ar.getId());
    assertThat(find(new PrintFilter(ar.getBranchId(), false, true, null)))
        .as("a receipt issued over the counter is not a receipt the system generated")
        .doesNotContain(ar.getId());
    print(List.of(ar.getId()), "CLIENT");
    assertThat(find(new PrintFilter(ar.getBranchId(), false, false, null)))
        .doesNotContain(ar.getId());
    assertThat(find(new PrintFilter(null, true, false, part))).contains(ar.getId());
  }

  private List<Long> find(PrintFilter filter) {
    return as.run(
            CASHIER,
            () ->
                search.search(
                    new ReceiptCriteria(
                        fx.company(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        ReceiptKind.AR,
                        null,
                        filter),
                    Pageable.ofSize(500)))
        .map(Receipt::getId)
        .getContent();
  }

  @Test
  void aFormChangeIsPrintedOnlyOnceAnotherUserApprovedIt() throws IOException {
    LocalDate today = BusinessClock.today(Clock.systemUTC());
    String footer = "Footer line " + System.nanoTime();
    FormText text =
        new FormText(
            null,
            "Insurance broker",
            null,
            null,
            "Note line",
            footer,
            "ACKNOWLEDGEMENT CERTIFICATE NO. {CERTIFICATE_NO} Date issued {PRINT_DATE}",
            "THIS DOCUMENT IS NOT VALID FOR CLAIM OF INPUT TAX.",
            null);
    ReceiptForm proposed = as.run("cashtl", () -> forms.propose(fx.company(), "AR", text, today));
    assertThat(proposed.getStatus()).isEqualTo(ReceiptForm.PENDING);
    Receipt ar = paid();
    assertThat(printedText(ar)).doesNotContain(footer);

    assertThatThrownBy(() -> as.run("cashtl", () -> forms.decide(proposed.getId(), true, null)))
        .hasMessage("The user who changed the form cannot approve it");
    as.run("approver", () -> forms.decide(proposed.getId(), true, null));

    assertThat(printedText(ar)).contains(footer, "Insurance broker", "Note line");
    assertThatThrownBy(
            () ->
                as.run("cashtl", () -> forms.propose(fx.company(), "AR", text, today.minusDays(1))))
        .hasMessage("The effective date must be today or a later date");
  }

  private String printedText(Receipt r) throws IOException {
    PrintBatch b = print(List.of(r.getId()), "CLIENT");
    return text(downloads.bytes(as.run(CASHIER, () -> printing.file(b.getId()))));
  }

  @Test
  void aSeriesFollowsTheStandardFormatOfItsYearAndACertificationHasASeriesNumber() {
    int year = BusinessClock.today(Clock.systemUTC()).getYear();
    Long dvo = fx.branch("DVO");
    String prefix = "AY" + (System.nanoTime() % 10000);
    ReceiptSeries created =
        as.run(
            "cashtl",
            () ->
                series.create(
                    new SeriesRequest(
                        fx.company(),
                        dvo,
                        ReceiptKind.AR,
                        prefix,
                        1,
                        9_999_999_999L,
                        "ATP-Y",
                        5,
                        new Numbering(year, "{PREFIX}-{YEAR}-{BRANCH}-{SEQ}"))));
    assertThat(created.getSeriesYear()).isEqualTo(year);
    assertThat(created.preview()).isEqualTo(prefix + "-" + year + "-DVO-0000000001");
    assertThat(created.getNumberFormat()).isEqualTo(prefix + "-" + year + "-DVO-{SEQ}");

    Receipt ar = paid();
    as.run(CASHIER, () -> printing.certificateOfPayment(ar.getId(), "POL-1", "CBG-NCR"));
    assertThat(
            jdbc.queryForObject(
                "select series_no from csh_certificate_of_payment where receipt_id = ?",
                String.class,
                ar.getId()))
        .startsWith("COP-" + year + "-");
  }
}
