package com.iortatechnxt.brokerverse.cashiering;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.cashiering.domain.ChannelFile;
import com.iortatechnxt.brokerverse.cashiering.domain.ChannelProfile;
import com.iortatechnxt.brokerverse.cashiering.domain.ChannelProfileRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.LegacyReference;
import com.iortatechnxt.brokerverse.cashiering.domain.LegacyReferenceRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.seed.PaymentChannelSimulator;
import com.iortatechnxt.brokerverse.cashiering.seed.PaymentChannelSimulator.SimulatedFile;
import com.iortatechnxt.brokerverse.cashiering.service.BatchPrintService;
import com.iortatechnxt.brokerverse.cashiering.service.ChannelFileService;
import com.iortatechnxt.brokerverse.cashiering.service.ChannelRunReport;
import com.iortatechnxt.brokerverse.cashiering.service.ChannelRunReport.Line;
import com.iortatechnxt.brokerverse.cashiering.service.ChannelRunReport.Report;
import com.iortatechnxt.brokerverse.cashiering.service.ChannelRunReport.Summary;
import com.iortatechnxt.brokerverse.cashiering.service.MftIntakeService;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSearchService;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSearchService.PrintFilter;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSearchService.ReceiptCriteria;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * BDOI's payment files (Operations Cashiering FRS v3.2: FRS.CSH.05.01, Appendix D): the file name
 * convention, the duplicate file and duplicate row, the header record count and total of the Bills
 * Payment file, the TOTAL row of the CLPC workbook, the run report per category, the legacy numbers
 * as match keys, the Direct Credit identification of payments received, the files received via MFT
 * with the alert of a refused file, the size limit and the channel simulator end to end.
 */
@IntegrationTest
class PaymentChannelFileIT {

  @Autowired private CashFixtures fx;
  @Autowired private ChannelFileService files;
  @Autowired private ChannelRunReport reports;
  @Autowired private ChannelProfileRepository profiles;
  @Autowired private LegacyReferenceRepository legacy;
  @Autowired private MftIntakeService mft;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private TransactionTemplate tx;
  @Autowired private AsUser as;
  @Autowired private SystemParameterService parameters;
  @Autowired private ReceiptSearchService search;
  @Autowired private BatchPrintService printing;

  /** Date of the payments of the files (an open period); the file names carry another day. */
  private static final LocalDate PAID_ON = LocalDate.of(2026, 9, 24);

  private static final String MFT_COMPANY = "CASH_MFT_COMPANY_CODE";

  private static final java.time.format.DateTimeFormatter US =
      java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy");

  private ChannelFile upload(String type, String name, byte[] content) {
    return as.run("cashier", () -> files.receive(fx.company(), type, name, content, "UPLOAD"));
  }

  /** A Bills Payment file of BDOI's layout: header record then one detail record per payment. */
  private static String bills(int firstInc, Integer announced, String... refAmounts) {
    StringBuilder details = new StringBuilder();
    BigDecimal total = BigDecimal.ZERO;
    int inc = firstInc;
    for (String refAmount : refAmounts) {
      String[] p = refAmount.split("=");
      total = total.add(new BigDecimal(p[1]));
      details.append(
          String.format(
              "2|%05d|%s|Juan Dela Cruz|%012.2f|N|2|BDO|00411335|||%s||00300|%s|09:00:00%n",
              inc++, p[0], new BigDecimal(p[1]), p[0], PAID_ON.format(US)));
    }
    int count = announced == null ? refAmounts.length : announced;
    return String.format("1|%05d|%012.2f|%s%n", count, total, PAID_ON.format(US)) + details;
  }

  /** The failed rows of a file, to explain a failed check. */
  private String failures(ChannelFile file) {
    return reports.report(file.getId()).lines().stream()
        .filter(l -> l.messages() != null && !l.messages().isBlank())
        .map(l -> l.rowNo() + ": " + l.messages())
        .toList()
        .toString();
  }

  private static LocalDate someDay() {
    return LocalDate.of(2024, 1, 1).plusDays(ThreadLocalRandom.current().nextInt(700));
  }

  private static String name(LocalDate day, int n) {
    return "BDOI" + day.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + n + ".TXT";
  }

  private static byte[] utf8(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void aBillsPaymentFileIsCheckedByNameHeaderAndContentAndReportedPerCategory() {
    OpsInvoice invoice = fx.motorInvoice();
    LocalDate day = someDay();
    int inc = ThreadLocalRandom.current().nextInt(10000, 90000);
    String unknown = "UNK-" + inc;
    String content =
        bills(
            inc,
            null,
            invoice.getInvoiceNo() + "=" + invoice.premiumBalance(),
            unknown + "=150.00");

    ChannelFile badName = upload("BILLS_PAYMENT", "payments.txt", utf8(content));
    assertThat(badName.getStatus()).isEqualTo(ChannelFile.REFUSED);
    assertThat(badName.getMessage())
        .startsWith("The file name payments.txt does not follow the convention of Bills Payment");

    ChannelFile badHeader =
        upload("BILLS_PAYMENT", name(day, 7), utf8(bills(inc, 48, unknown + "=150.00")));
    assertThat(badHeader.getStatus()).isEqualTo(ChannelFile.REFUSED);
    assertThat(badHeader.getMessage())
        .isEqualTo("The header gives 48 records but the file holds 1 detail records");

    ChannelFile done = upload("BILLS_PAYMENT", name(day, 1), utf8(content));
    assertThat(done.getStatus()).as(() -> failures(done)).isEqualTo(ChannelFile.PROCESSED);
    assertThat(done.getHeaderCount()).isEqualTo(2);
    assertThat(done.getStoredFileId()).isNotNull();
    assertThat(fx.invoice(invoice.getInvoiceNo()).getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    Report report = reports.report(done.getId());
    assertThat(report.summary())
        .extracting(Summary::category)
        .contains("Applied Payment", "Unapplied - Unbooked/Unmatched");
    assertThat(report.lines())
        .extracting(Line::reference)
        .contains(invoice.getInvoiceNo(), unknown);
    assertThat(reports.excel(done.getId())).isNotEmpty();
    List<Long> uploaded =
        jdbc.queryForList(
            "select distinct p.receipt_id from csh_payment p where p.batch_ref = ?"
                + " and p.receipt_id is not null",
            Long.class,
            done.getBulkJobNo());
    List<Long> queue =
        as.run(
                "cashier",
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
                            null,
                            null,
                            new PrintFilter(null, false, true, null)),
                        Pageable.ofSize(500)))
            .map(Receipt::getId)
            .getContent();
    assertThat(queue).as("the ARs of the upload are For Printing").containsAll(uploaded);
    assertThat(
            as.run("cashier", () -> printing.print(fx.company(), uploaded, "Print queue"))
                .getPrintedCount())
        .isEqualTo(uploaded.size());

    ChannelFile again = upload("BILLS_PAYMENT", name(day, 1), utf8(content));
    assertThat(again.getStatus()).isEqualTo(ChannelFile.REFUSED);
    assertThat(again.getMessage())
        .isEqualTo("This file was already uploaded as " + done.getUploadRef());

    String more =
        bills(
            inc,
            null,
            invoice.getInvoiceNo() + "=" + invoice.premiumBalance(),
            unknown + "=150.00",
            unknown + "-NEW=99.00");
    ChannelFile overlapping = upload("BILLS_PAYMENT", name(day, 2), utf8(more));
    Report second = reports.report(overlapping.getId());
    assertThat(second.lines())
        .extracting(Line::status)
        .as(overlapping.getMessage())
        .containsExactly("Duplicate", "Duplicate", "Unapplied - Unbooked/Unmatched");
  }

  @Test
  void theClpcWorkbookIsReadWithItsTotalRowAndTheLegacyNumberIsAMatchKey() throws IOException {
    OpsInvoice invoice = fx.motorInvoice();
    String qps = "QPS-" + System.nanoTime();
    tx.executeWithoutResult(
        s -> legacy.save(new LegacyReference(fx.company(), "QPS", qps, invoice.getInvoiceNo())));
    LocalDate day = someDay();
    BigDecimal due = invoice.premiumBalance();
    byte[] ok =
        clpc(List.of(qps + "=" + due, "UNK-CLPC=100.00"), due.add(new BigDecimal("100.00")));
    String file =
        "CLPC-"
            + day.format(
                java.time.format.DateTimeFormatter.ofPattern("MMMddyyyy", java.util.Locale.ENGLISH))
            + ".xlsx";
    byte[] wrongTotal = clpc(List.of("UNK-CLPC2=100.00"), new BigDecimal("999.00"));
    ChannelFile refused = upload("CLPC", file, wrongTotal);
    assertThat(refused.getStatus()).isEqualTo(ChannelFile.REFUSED);
    assertThat(refused.getMessage()).contains("differs from the total of the payments");

    ChannelFile done = upload("CLPC", file, ok);
    assertThat(done.getStatus()).as(() -> failures(done)).isEqualTo(ChannelFile.PROCESSED);
    assertThat(done.getRowsRead()).isEqualTo(2);
    assertThat(fx.invoice(invoice.getInvoiceNo()).getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
  }

  private static byte[] clpc(List<String> refAmounts, BigDecimal total) throws IOException {
    try (XSSFWorkbook book = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = book.createSheet("CLPC");
      String[] head = {
        "COUNT",
        "DATE CREDIT",
        "AMOUNT",
        "AR NUMBER",
        "INVOICE",
        "ASSURED",
        "PN NUMBER",
        "CORP.DEPT",
        "RISK CODE",
        "REMARKS"
      };
      Row header = sheet.createRow(0);
      for (int c = 0; c < head.length; c++) {
        header.createCell(c).setCellValue(head[c]);
      }
      int r = 1;
      for (String refAmount : refAmounts) {
        String[] p = refAmount.split("=");
        Row row = sheet.createRow(r);
        row.createCell(0).setCellValue(r);
        row.createCell(1).setCellValue(PAID_ON.format(US));
        row.createCell(2).setCellValue(p[1]);
        row.createCell(4).setCellValue(p[0]);
        row.createCell(5).setCellValue("Juan Dela Cruz " + r);
        r++;
      }
      Row totals = sheet.createRow(r);
      totals.createCell(0).setCellValue("TOTAL");
      totals.createCell(2).setCellValue(total.toPlainString());
      book.write(out);
      return out.toByteArray();
    }
  }

  @Test
  void aDirectCreditRowIdentifiesAPaymentAlreadyReceivedInsteadOfIssuingASecondAr() {
    OpsInvoice invoice = fx.motorInvoice();
    LocalDate day = someDay();
    int inc = ThreadLocalRandom.current().nextInt(10000, 90000);
    String unknown = "UNK-DC-" + inc;
    BigDecimal due = invoice.premiumBalance();
    String billsName = name(day, 3);
    ChannelFile bills =
        upload("BILLS_PAYMENT", billsName, utf8(bills(inc, null, unknown + "=" + due)));
    assertThat(bills.getStatus()).as(() -> failures(bills)).isEqualTo(ChannelFile.PROCESSED);
    long arsBefore = jdbc.queryForObject("select count(*) from csh_receipt", Long.class);

    String header =
        "Transaction date|BP filename|Transaction no|Paid amount|Payment type|Payor|Account ref no"
            + "|Assured|Ebix reference no.|Logged by|Requestor\n";
    String rows =
        String.format(
                "%s|%s|%05d|%s|ONLINE|Juan|%s|Juan|||%n",
                PAID_ON.format(US), billsName, inc, due.toPlainString(), invoice.getInvoiceNo())
            + String.format(
                "%s|NO-SUCH-FILE.TXT|00001|10.00|ONLINE|Juan|%s|Juan|||%n",
                PAID_ON.format(US), invoice.getInvoiceNo());
    String dcName =
        "OTC" + day.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "1- AUTOCREDIT.TXT";
    ChannelFile dc = upload("DIRECT_CREDIT", dcName, utf8(header + rows));
    assertThat(dc.getStatus()).as(() -> failures(dc)).isEqualTo(ChannelFile.PROCESSED);
    Report report = reports.report(dc.getId());
    assertThat(report.lines())
        .extracting(Line::status)
        .containsExactly("Applied Payment", "Invalid BP filename");
    assertThat(fx.invoice(invoice.getInvoiceNo()).getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(jdbc.queryForObject("select count(*) from csh_receipt", Long.class))
        .isEqualTo(arsBefore);

    ChannelFile replay =
        upload(
            "DIRECT_CREDIT",
            dcName.replace("1- ", "2- "),
            utf8(header + rows.lines().findFirst().orElseThrow() + "\n"));
    assertThat(reports.report(replay.getId()).lines())
        .extracting(Line::status)
        .containsExactly("Duplicate");
  }

  @Test
  void filesReceivedViaMftAreProcessedAndARefusedOneIsAlerted() throws IOException {
    ChannelProfile profile = profiles.findById("BILLS_PAYMENT").orElseThrow();
    String folder = "it-mft-" + System.nanoTime();
    // The MFT files go to the company of the fixtures, whatever companies other tests created.
    String mftCompany = parameters.text(MFT_COMPANY, "");
    String company =
        jdbc.queryForObject(
            "select code from org_company where id = ?", String.class, fx.company());
    as.run("admin", () -> parameters.update(MFT_COMPANY, company));
    as.run(
        "admin",
        () ->
            files.changeProfile(
                "BILLS_PAYMENT",
                new ChannelProfile.Change(
                    profile.getNamePattern(),
                    profile.getNameExample(),
                    profile.getFieldMap(),
                    profile.getMaxMb(),
                    folder,
                    true)));
    try {
      Path in = mft.folder(profiles.findById("BILLS_PAYMENT").orElseThrow());
      Files.createDirectories(in);
      LocalDate day = someDay();
      int inc = ThreadLocalRandom.current().nextInt(10000, 90000);
      Files.writeString(in.resolve(name(day, 4)), bills(inc, null, "UNK-MFT-" + inc + "=210.00"));
      Files.writeString(in.resolve("payments.txt"), bills(inc + 1, null, "UNK-MFT-X=10.00"));
      List<ChannelFile> received = mft.poll();
      assertThat(received).extracting(ChannelFile::getSource).containsOnly("MFT");
      assertThat(received)
          .extracting(ChannelFile::getStatus)
          .as(() -> received.stream().map(this::failures).toList().toString())
          .containsExactlyInAnyOrder(ChannelFile.PROCESSED, ChannelFile.REFUSED);
      ChannelFile refused =
          received.stream()
              .filter(f -> ChannelFile.REFUSED.equals(f.getStatus()))
              .findFirst()
              .orElseThrow();
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from alt_alert where dedup_key = ?",
                  Long.class,
                  "CASH_MFT_FILE_REFUSED:" + refused.getUploadRef()))
          .isEqualTo(1L);
      assertThat(Files.exists(in.resolve("processed").resolve(name(day, 4)))).isTrue();
      assertThat(Files.exists(in.resolve("refused").resolve("payments.txt"))).isTrue();
    } finally {
      as.run(
          "admin",
          () ->
              files.changeProfile(
                  "BILLS_PAYMENT",
                  new ChannelProfile.Change(
                      profile.getNamePattern(),
                      profile.getNameExample(),
                      profile.getFieldMap(),
                      profile.getMaxMb(),
                      profile.getMftFolder(),
                      false)));
      as.run("admin", () -> parameters.update(MFT_COMPANY, mftCompany));
    }
  }

  @Test
  void aFileAboveTheSizeLimitIsRefusedAndTheSimulatorRunsEndToEnd() {
    ChannelProfile profile = profiles.findById("TRADE").orElseThrow();
    as.run(
        "admin",
        () ->
            files.changeProfile(
                "TRADE",
                new ChannelProfile.Change(
                    profile.getNamePattern(),
                    profile.getNameExample(),
                    profile.getFieldMap(),
                    1,
                    null,
                    false)));
    try {
      ChannelFile big = upload("TRADE", "UploadTrade_01.02.2025.TXT", new byte[2 * 1024 * 1024]);
      assertThat(big.getMessage()).isEqualTo("The file is larger than 1 MB");
    } finally {
      as.run(
          "admin",
          () ->
              files.changeProfile(
                  "TRADE",
                  new ChannelProfile.Change(
                      profile.getNamePattern(),
                      profile.getNameExample(),
                      profile.getFieldMap(),
                      profile.getMaxMb(),
                      null,
                      false)));
    }
    fx.motorInvoice();
    PaymentChannelSimulator simulator =
        new PaymentChannelSimulator(profiles, mft, jdbc, Clock.systemUTC());
    SimulatedFile trade = simulator.generate(fx.company(), "TRADE", 2, null);
    ChannelFile done = upload("TRADE", trade.name(), trade.content());
    assertThat(done.getStatus())
        .as(done.getMessage())
        .isIn(ChannelFile.PROCESSED, ChannelFile.PARTIAL);
    assertThat(done.getRowsRead()).isGreaterThanOrEqualTo(2);
  }
}
