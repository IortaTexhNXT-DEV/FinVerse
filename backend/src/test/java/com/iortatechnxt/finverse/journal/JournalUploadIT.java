package com.iortatechnxt.finverse.journal;

import static com.iortatechnxt.finverse.support.CsrfRequests.multipart;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.finverse.journal.domain.JournalStatus;
import com.iortatechnxt.finverse.journal.service.JournalEntryService;
import com.iortatechnxt.finverse.journal.service.JournalUploadService;
import com.iortatechnxt.finverse.journal.service.JournalUploadTemplate;
import com.iortatechnxt.finverse.journal.service.UploadResult;
import com.iortatechnxt.finverse.journal.service.UploadResult.VoucherResult;
import com.iortatechnxt.finverse.journal.service.UploadResult.VoucherStatus;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestData;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class JournalUploadIT {

  private static final String HEADER =
      "voucher_key,branch_code,journal_type,value_date,currency,narration,reference,"
          + "account_code,debit,credit,cost_center,line_narration\n";

  @Autowired private JournalUploadService service;
  @Autowired private JournalEntryService entries;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;
  @Autowired private TestData data;
  @Autowired private MockMvc mvc;

  private UploadResult process(String csv, boolean commit) {
    return as.run(
        "accountant",
        () ->
            service.process(
                data.company().getId(),
                "upload.csv",
                csv.getBytes(StandardCharsets.UTF_8),
                commit));
  }

  private static Map<String, VoucherResult> byKey(UploadResult result) {
    return result.vouchers().stream()
        .collect(Collectors.toMap(VoucherResult::voucherKey, Function.identity()));
  }

  private long journalsWithReference(String reference) {
    Long count =
        jdbc.queryForObject(
            "select count(*) from jnl_batch where reference = ?", Long.class, reference);
    return count == null ? 0 : count;
  }

  private static String validFile(String reference) {
    String date = LocalDate.now().toString();
    return HEADER
        + "U1,HO,MANUAL,"
        + date
        + ",PHP,Uploaded rent,"
        + reference
        + ",5603,1000.00,,FIN,Rent\n"
        + "U1,,,,,,,1111,,1000.00,,\n"
        + "U2,HO,ACCRUAL,"
        + date
        + ",PHP,\"Accrual, fees\","
        + reference
        + ",5605,250.00,,FIN,\n"
        + "U2,,,,,,,2502,,250.00,,\n";
  }

  @Test
  void validateModeChecksEverythingButCreatesNothing() {
    UploadResult result = process(validFile("UPL-DRY"), false);
    assertThat(result.committed()).isFalse();
    assertThat(result.totalRows()).isEqualTo(4);
    assertThat(result.vouchers())
        .extracting(VoucherResult::status)
        .containsOnly(VoucherStatus.VALID);
    assertThat(result.validVouchers()).isEqualTo(2);
    assertThat(result.createdVouchers()).isZero();
    assertThat(result.rows()).allMatch(UploadResult.RowResult::valid);
    assertThat(journalsWithReference("UPL-DRY")).isZero();
  }

  @Test
  void importModeCreatesOneDraftPerVoucher() {
    UploadResult result = process(validFile("UPL-REAL"), true);
    assertThat(result.createdVouchers()).isEqualTo(2);
    VoucherResult u2 = byKey(result).get("U2");
    assertThat(u2.batchNo()).startsWith("ACR-HO-");
    assertThat(u2.totalDebit()).isEqualByComparingTo("250.00");
    var batch = entries.get(u2.batchId());
    assertThat(batch.getStatus()).isEqualTo(JournalStatus.DRAFT);
    assertThat(batch.getNarration()).isEqualTo("Accrual, fees");
    assertThat(batch.getCreatedBy()).isEqualTo("accountant");
    assertThat(journalsWithReference("UPL-REAL")).isEqualTo(2);

    // Uploaded drafts behave like any manual draft: they can be copied and cancelled.
    var copy = as.run("accountant", () -> entries.copy(u2.batchId(), LocalDate.now()));
    assertThat(copy.getStatus()).isEqualTo(JournalStatus.DRAFT);
    assertThat(as.run("accountant", () -> entries.cancel(u2.batchId())).getStatus())
        .isEqualTo(JournalStatus.CANCELLED);
  }

  @Test
  void invalidVouchersAreReportedAndSkippedWhileValidOnesAreCreated() {
    String date = LocalDate.now().toString();
    String csv =
        HEADER
            + "OK,HO,MANUAL,"
            + date
            + ",PHP,Good voucher,UPL-MIX,5603,10.00,,FIN,\n"
            + "OK,,,,,,,1111,,10.00,,\n"
            + "UNB,HO,MANUAL,"
            + date
            + ",PHP,Unbalanced,UPL-MIX,5603,10.00,,FIN,\n"
            + "UNB,,,,,,,1111,,9.00,,\n"
            + "ACC,HO,MANUAL,"
            + date
            + ",PHP,Unknown account,UPL-MIX,NOPE,10.00,,,\n"
            + "ACC,,,,,,,1111,,10.00,,\n"
            + "BR,XX,MANUAL,"
            + date
            + ",PHP,Unknown branch,UPL-MIX,5603,10.00,,FIN,\n"
            + "BR,,,,,,,1111,,10.00,,\n"
            + "HDR,HO,MANUAL,"
            + date
            + ",PHP,Header clash,UPL-MIX,5603,10.00,,FIN,\n"
            + "HDR,HO,MANUAL,"
            + date
            + ",USD,,,1111,,10.00,,\n"
            + "ROW,HO,MANUAL,"
            + date
            + ",PHP,Bad row,UPL-MIX,5603,abc,,FIN,\n"
            + "ROW,,,,,,,1111,,10.00,,\n"
            + "ONE,HO,MANUAL,"
            + date
            + ",PHP,Single line,UPL-MIX,5603,10.00,,FIN,\n"
            + ",HO,MANUAL,"
            + date
            + ",PHP,No key,UPL-MIX,5603,10.00,,FIN,\n";
    UploadResult result = process(csv, true);
    Map<String, VoucherResult> vouchers = byKey(result);
    assertThat(vouchers.get("OK").status()).isEqualTo(VoucherStatus.CREATED);
    assertThat(vouchers.get("UNB").messages()).anyMatch(m -> m.contains("differ"));
    assertThat(vouchers.get("ACC").status()).isEqualTo(VoucherStatus.ERROR);
    assertThat(vouchers.get("ACC").messages()).anyMatch(m -> m.contains("NOPE"));
    assertThat(vouchers.get("BR").messages()).contains("Unknown branch XX");
    assertThat(vouchers.get("HDR").messages()).anyMatch(m -> m.contains("currency differs"));
    assertThat(vouchers.get("ROW").messages()).contains("Row 12 has errors");
    assertThat(vouchers.get("ONE").messages()).contains("A voucher needs at least two lines");
    assertThat(result.rows())
        .filteredOn(r -> !r.valid())
        .extracting(r -> r.rowNumber())
        .contains(12, 15);
    assertThat(journalsWithReference("UPL-MIX")).isEqualTo(1);
  }

  @Test
  void structuralProblemsRejectTheWholeFile() {
    assertThatThrownBy(() -> process("voucher_key,account_code\nV1,1111\n", false))
        .extracting("code")
        .isEqualTo("MISSING_COLUMNS");
    assertThatThrownBy(() -> process("", false))
        .extracting("code")
        .isEqualTo("INVALID_UPLOAD_SIZE");
    assertThatThrownBy(() -> process("\n\n", false)).extracting("code").isEqualTo("EMPTY_FILE");
  }

  @Test
  void xlsxTemplateValidates() {
    UploadResult result =
        as.run(
            "accountant",
            () ->
                service.process(
                    data.company().getId(),
                    "template.xlsx",
                    JournalUploadTemplate.xlsx(LocalDate.now()),
                    false));
    assertThat(result.vouchers()).hasSize(2).allMatch(v -> v.status() == VoucherStatus.VALID);
  }

  @Test
  @WithUserDetails("accountant")
  void httpUploadAndTemplates() throws Exception {
    mvc.perform(
            multipart("/api/v1/journals/upload")
                .file(
                    new MockMultipartFile(
                        "file",
                        "j.csv",
                        "text/csv",
                        validFile("UPL-HTTP").getBytes(StandardCharsets.UTF_8)))
                .param("companyId", data.company().getId().toString())
                .param("mode", "VALIDATE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vouchers.length()").value(2))
        .andExpect(jsonPath("$.committed").value(false));
    mvc.perform(get("/api/v1/journals/upload/template?format=xlsx"))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")));
    mvc.perform(get("/api/v1/journals/upload/template"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("text/csv")));
  }
}
