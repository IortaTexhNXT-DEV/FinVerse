package com.iortatechnxt.brokerverse.remittance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler.FlowInFile;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.domain.PartyRepository;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.OrStatus;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceFileDelivery;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceSettingChange;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceSettingChange.Values;
import com.iortatechnxt.brokerverse.remittance.service.ExtractDelivery;
import com.iortatechnxt.brokerverse.remittance.service.InsurerOrUploads;
import com.iortatechnxt.brokerverse.remittance.service.InsurerOrUploads.UploadResult;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceExtractFiles;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceQueryService;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceQueryService.BatchSearch;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceScheduleJob;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceSettingsService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Remittance extract files, settlement numbering, shared folder, Remittance Settings and the
 * insurer's returned extract (FRS BRD-02 FRID.RMT.001, 003, 008; ICD IF-RMT-01 to 04). All data is
 * invented by the fixtures.
 */
@IntegrationTest
class RemittanceExtractFilesIT {

  private static final int OR_DATE = 32;
  private static final int OR_NO = 33;
  private static final int OR_AMOUNT = 34;
  private static final int BATCH_NUMBER = 36;

  @Autowired private RemittanceFixtures fx;
  @Autowired private RemittanceExtractFiles files;
  @Autowired private ExtractDelivery delivery;
  @Autowired private RemittanceSettingsService settings;
  @Autowired private RemittanceScheduleJob schedule;
  @Autowired private InsurerOrUploads uploads;
  @Autowired private RemittanceQueryService queries;
  @Autowired private DisbursementQueueService queue;
  @Autowired private PartyRepository parties;
  @Autowired private AsUser as;
  @Autowired private TransactionTemplate tx;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private MockMvc mvc;

  private void assignDv(RemittanceBatch batch) {
    DisbursementRequest request = queue.find("REMITTANCE", batch.getBatchNo()).orElseThrow();
    as.run(
        "disb",
        () -> tx.execute(s -> queue.assignDv(request.getId(), "DV-" + BookingFixtures.token())));
  }

  private UploadResult upload(String name, byte[] content) {
    return as.run("remit", () -> uploads.upload(new FlowInFile(name, content)));
  }

  @Test
  void aBatchGetsASettlementNumberAndBothExtractFilesInItsFolder() throws IOException {
    OpsInvoice invoice = fx.paidInvoice();
    fx.extract(invoice.getInvoiceNo());
    RemittanceBatch batch = fx.batchOf(invoice.getInvoiceNo());

    assertThat(batch.getSettlementNo()).matches("\\d{8}");
    assertThat(batch.getSettlementRef())
        .isEqualTo(batch.getInsurerCode() + batch.getSettlementNo());
    String excelName = files.excelName(batch);
    assertThat(excelName)
        .matches("\\d{8}" + batch.getInsurerCode() + batch.getSettlementNo() + "_Normal\\w+\\.xls");
    assertThat(files.textName(batch))
        .matches("AM01\\d{6}" + batch.getInsurerCode() + batch.getSettlementNo() + "\\.txt");
    assertThat(delivery.history(batch.getId()))
        .hasSize(2)
        .allSatisfy(
            d -> {
              assertThat(d.getStage()).isEqualTo(RemittanceFileDelivery.Stage.CREATED);
              assertThat(d.getStatus()).isEqualTo(RemittanceFileDelivery.Status.DELIVERED);
            });

    try (HSSFWorkbook wb =
        new HSSFWorkbook(new ByteArrayInputStream(files.excel(batch, "remit")))) {
      Sheet sheet = wb.getSheetAt(0);
      assertThat(sheet.getRow(8).getCell(0).getStringCellValue()).isEqualTo("Invoice Number");
      assertThat(sheet.getRow(8).getCell(27).getStringCellValue())
          .isEqualTo("Mall Assurance Total Deductions");
      Row account = sheet.getRow(9);
      assertThat(account.getCell(0).getStringCellValue()).isEqualTo(invoice.getInvoiceNo());
      assertThat(account.getCell(BATCH_NUMBER).getStringCellValue())
          .isEqualTo(batch.getSettlementRef());
      assertThat(account.getCell(17).getNumericCellValue())
          .isEqualTo(batch.getTotals().paidAr().doubleValue());
      assertThat(sheet.getRow(12).getCell(0).getStringCellValue()).isEqualTo("Number of Accounts:");
    }

    String[] text =
        new String(files.text(batch), StandardCharsets.UTF_8).replace("﻿", "").split("\r\n");
    String[] header = text[0].split("\\|", -1);
    assertThat(header[0]).isEqualTo("H");
    assertThat(header[4]).isEqualTo(String.valueOf(text.length - 1));
    assertThat(new BigDecimal(header[5])).isEqualByComparingTo(batch.getTotals().paidAr());
    assertThat(text[1].split("\\|", -1)).hasSize(27).startsWith("D", invoice.getInvoiceNo());
  }

  @Test
  void theReturnedExcelAndTextExtractsRecordTheInsurersOfficialReceipts() throws IOException {
    OpsInvoice first = fx.paidInvoice();
    RemittanceBatch batch = fx.approvedBatch(first);
    assignDv(batch);
    assertThat(delivery.history(batch.getId()))
        .extracting(RemittanceFileDelivery::getStage)
        .contains(RemittanceFileDelivery.Stage.APPROVED);

    BigDecimal paid = batch.line(first.getInvoiceNo()).getAmounts().paidAr();
    byte[] returned;
    try (HSSFWorkbook wb = new HSSFWorkbook(new ByteArrayInputStream(files.excel(batch, "remit")));
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Row row = wb.getSheetAt(0).getRow(9);
      row.createCell(OR_DATE).setCellValue("10/20/2026");
      row.createCell(OR_NO).setCellValue("XOR-" + BookingFixtures.token());
      row.createCell(OR_AMOUNT).setCellValue(paid.toPlainString());
      wb.write(out);
      returned = out.toByteArray();
    }
    UploadResult excel = upload(files.excelName(batch), returned);
    assertThat(excel.updated())
        .singleElement()
        .satisfies(l -> assertThat(l.getOrStatus()).isEqualTo(OrStatus.MATCHED));

    OpsInvoice second = fx.paidInvoice();
    RemittanceBatch other = fx.approvedBatch(second);
    assignDv(other);
    String body = new String(files.text(other), StandardCharsets.UTF_8);
    String[] lines = body.split("\r\n");
    String[] d = lines[1].split("\\|", -1);
    d[22] = "10/21/2026";
    d[23] = "TOR-" + BookingFixtures.token();
    d[24] = other.line(second.getInvoiceNo()).getAmounts().paidAr().toPlainString();
    String filled = lines[0] + "\r\n" + String.join("|", d) + "\r\n";
    UploadResult text = upload(files.textName(other), filled.getBytes(StandardCharsets.UTF_8));
    assertThat(text.updated())
        .singleElement()
        .extracting(BatchLine::getInvoiceNo)
        .isEqualTo(second.getInvoiceNo());

    String wrongTotals = lines[0].replaceFirst("\\|1\\|", "|2|") + "\r\n" + String.join("|", d);
    UploadResult refused =
        upload(files.textName(other), wrongTotals.getBytes(StandardCharsets.UTF_8));
    assertThat(refused.updated()).isEmpty();
    assertThat(refused.records())
        .singleElement()
        .satisfies(r -> assertThat(r.getMessage()).contains("header"));
  }

  @Test
  void settingsChangesNeedAnotherUserAndSetTheFoldersAndTheSchedule() {
    Long company = fx.company();
    Values unreachable =
        new Values(
            true,
            "06:00",
            false,
            "Remittance/Peso.invalid",
            "Remittance/Peso.invalid",
            "Remittance/Peso.invalid",
            "{date}{insurer}{settlement}_{type}",
            "AM01");
    RemittanceSettingChange change =
        as.run("remit", () -> settings.propose(company, unreachable, "Test of the shared folder"));
    assertThatThrownBy(() -> as.run("remit", () -> settings.decide(change.getId(), true, null)))
        .extracting("code")
        .isEqualTo("REMIT_SETTING_SAME_USER");
    assertThatThrownBy(
            () ->
                as.run(
                    "remit",
                    () -> settings.propose(company, RemittanceSettingsService.DEFAULTS, "Again")))
        .extracting("code")
        .isEqualTo("REMIT_SETTING_PENDING");
    as.run("remittl", () -> settings.decide(change.getId(), true, "Agreed"));
    try {
      assertThat(settings.current(company).extractionTime()).isEqualTo("06:00");
      ZonedDateTime early = ZonedDateTime.of(2026, 10, 19, 5, 0, 0, 0, ZoneId.of("Asia/Manila"));
      assertThat(schedule.due(company, settings.current(company), early)).isFalse();
      assertThat(schedule.due(company, settings.current(company), early.plusHours(2))).isTrue();

      OpsInvoice invoice = fx.paidInvoice();
      fx.extract(invoice.getInvoiceNo());
      RemittanceBatch batch = fx.batchOf(invoice.getInvoiceNo());
      assertThat(delivery.history(batch.getId()))
          .allSatisfy(
              d -> assertThat(d.getStatus()).isEqualTo(RemittanceFileDelivery.Status.FAILED));
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from alt_alert where exception_code = ? and entity_id = ?",
                  Integer.class,
                  ExtractDelivery.FAILED_ALERT,
                  batch.getBatchNo()))
          .isPositive();
    } finally {
      RemittanceSettingChange back =
          as.run(
              "remit", () -> settings.propose(company, RemittanceSettingsService.DEFAULTS, "Back"));
      as.run("remittl", () -> settings.decide(back.getId(), true, null));
    }
    assertThatThrownBy(
            () ->
                as.run(
                    "remit",
                    () ->
                        settings.propose(
                            company,
                            new Values(true, "25:00", true, "a", "b", "c", "{insurer}", "AM01"),
                            "Bad")))
        .extracting("code")
        .isEqualTo("REMIT_SETTING_INVALID");
  }

  @Test
  void batchesOfReinsurersAreHiddenUntilPhaseTwo() throws Exception {
    OpsInvoice invoice = fx.paidInvoice();
    fx.extract(invoice.getInvoiceNo());
    RemittanceBatch batch = fx.batchOf(invoice.getInvoiceNo());
    String code = "RI-" + BookingFixtures.token();
    tx.execute(
        s ->
            parties.save(
                new Party(fx.company(), code, "Test Reinsurer", PartyType.REINSURER, "PHP")));
    jdbc.update("update rem_batch set insurer_code = ? where id = ?", code, batch.getId());

    List<RemittanceBatch> hidden =
        queries
            .batches(
                new BatchSearch(fx.company(), null, code, null, null, false), PageRequest.of(0, 10))
            .getContent();
    List<RemittanceBatch> shown =
        queries
            .batches(
                new BatchSearch(fx.company(), null, code, null, null, true), PageRequest.of(0, 10))
            .getContent();
    assertThat(hidden).isEmpty();
    assertThat(shown).extracting(RemittanceBatch::getId).containsExactly(batch.getId());
    mvc.perform(
            get("/api/v1/remittance/batches/" + batch.getId())
                .with(user("remit").authorities(() -> "REMIT_PROCESS")))
        .andExpect(status().isForbidden());
  }
}
