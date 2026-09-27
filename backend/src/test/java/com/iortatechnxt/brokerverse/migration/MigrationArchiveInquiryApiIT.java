package com.iortatechnxt.brokerverse.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The legacy archive and the Legacy Inquiry (wave DM2-B): archive records of closed legacy
 * transactions (H01) with their labelled legacy columns, a legacy document staged and loaded
 * through the document index (H02) with its size and checksum checked, the search, view, download
 * and export by an Audit / Compliance user with a reason, and the append-only access log for the
 * Compliance reviewer.
 */
@IntegrationTest
class MigrationArchiveInquiryApiIT {

  private static final String BASE = "/api/v1/migration";
  private static final String INQUIRY = "/api/v1/legacy-inquiry";
  private static final String REASON = "&reasonCode=AUDIT&reasonText=Year-end%20audit%20sample";
  private static final byte[] PDF =
      "%PDF-1.4\n1 0 obj << /Type /Catalog >> endobj\ntrailer << /Root 1 0 R >>\n%%EOF\n"
          .getBytes(StandardCharsets.US_ASCII);

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private ObjectMapper json;
  @Autowired private TestData data;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ReportService reports;
  @Autowired private AsUser as;

  private MigrationTestSupport mig;
  private long company;

  @BeforeEach
  void setUp() throws Exception {
    company = data.company().getId();
    mig = new MigrationTestSupport(api, mvc, users, json, jdbc, company);
    mig.signMapping("H01");
    mig.signMapping("H02");
  }

  private static Map<String, String> record(String type, String key, String t) {
    Map<String, String> r = new HashMap<>();
    r.put("record_type", type);
    r.put("legacy_key", key);
    r.put("legacy_client_no", "E" + t);
    r.put("client_name", "Archivo Reyes " + t);
    r.put("policy_no", "MC-PC-22-" + t);
    r.put("document_date", "2022-03-15");
    r.put("currency", "PHP");
    r.put("amount", "1500.00");
    r.put("status", "PAID");
    return r;
  }

  private JsonNode loadRecords(String t, String seq, String status) throws Exception {
    Map<String, String> invoice = record("INVOICE", "I" + t, t);
    invoice.put("status", status);
    invoice.put("invoice_no", "I" + t);
    invoice.put(
        "detail_json", "\"{\"\"Coverage\"\":\"\"Fire\"\",\"\"Remarks\"\":\"\"Paid in full\"\"}\"");
    Map<String, String> receipt = record("RECEIPT", "AR-" + t, t);
    receipt.put("receipt_no", "AR-" + t);
    JsonNode extract =
        mig.upload(
            "H01",
            "H01",
            "H01_EBIX_20271231_" + seq + ".csv",
            List.of(invoice, receipt),
            List.of("record_type", "legacy_key"));
    JsonNode batch = mig.load("H01", List.of(extract.get("extractNo").asText()));
    assertThat(batch.get("counts").get("loaded").asInt())
        .as(mig.rows(batch.get("batchNo").asText()))
        .isEqualTo(2);
    return batch;
  }

  private void stage(String fileName, byte[] content) throws Exception {
    mvc.perform(
            multipart(BASE + "/archive/documents/EBIX")
                .file(new MockMultipartFile("files", fileName, "application/pdf", content))
                .param("companyId", String.valueOf(company))
                .with(user(users.loadUserByUsername("migops")))
                .with(csrf()))
        .andExpect(status().isOk());
  }

  private JsonNode loadDocuments(String t, String good, String bad) throws Exception {
    Map<String, String> ok = new HashMap<>();
    ok.put("record_type", "INVOICE");
    ok.put("legacy_key", "I" + t);
    ok.put("document_type", "OR");
    ok.put("document_no", "OR-" + t);
    ok.put("document_date", "2022-03-20");
    ok.put("file_name", good);
    ok.put("file_size_bytes", String.valueOf(PDF.length));
    ok.put("sha256", Sha256.hex(PDF));
    Map<String, String> wrong = new HashMap<>(ok);
    wrong.put("file_name", bad);
    wrong.put("sha256", "0".repeat(64));
    JsonNode extract =
        mig.upload(
            "H02",
            "H02",
            "H02_EBIX_20271231_" + t.substring(t.length() - 2) + ".csv",
            List.of(ok, wrong),
            List.of("record_type", "legacy_key", "file_name"));
    return mig.load("H02", List.of(extract.get("extractNo").asText()));
  }

  @Test
  void theArchiveIsSearchedViewedAndExportedWithEveryAccessLogged() throws Exception {
    String t = LegacyInvoiceFixtures.token();
    loadRecords(t, t.substring(t.length() - 2), "PAID");
    String good = "OR_" + t + ".pdf";
    String bad = "BAD_" + t + ".pdf";
    stage(good, PDF);
    stage(bad, PDF);
    JsonNode docs = loadDocuments(t, good, bad);
    assertThat(docs.get("counts").get("loaded").asInt())
        .as(mig.rows(docs.get("batchNo").asText()))
        .isEqualTo(1);
    assertThat(mig.rows(docs.get("batchNo").asText())).contains("SHA-256");

    api.doGet("legacyaudit", INQUIRY + "/records?companyId=" + company + "&invoiceNo=I" + t)
        .andExpect(status().isUnprocessableEntity());
    JsonNode found =
        mig.get(
            "legacyaudit", INQUIRY + "/records?companyId=" + company + "&client=E" + t + REASON);
    assertThat(found.get("content")).hasSize(2);
    JsonNode byInvoice =
        mig.get(
            "legacyaudit", INQUIRY + "/records?companyId=" + company + "&invoiceNo=I" + t + REASON);
    assertThat(byInvoice.get("content")).hasSize(1);
    long id = byInvoice.get("content").get(0).get("id").asLong();
    assertThat(byInvoice.get("content").get(0).get("documentCount").asInt()).isEqualTo(1);

    JsonNode detail = mig.get("legacyaudit", INQUIRY + "/records/" + id + "?x=1" + REASON);
    assertThat(detail.get("details").get("Remarks").asText()).isEqualTo("Paid in full");
    assertThat(detail.get("documents")).hasSize(1);
    assertThat(detail.get("documents").get(0).get("description").asText())
        .contains("OFFICIAL_RECEIPT", "(OR)");
    long doc = detail.get("documents").get(0).get("id").asLong();
    byte[] file =
        api.download(
                "legacyaudit", INQUIRY + "/records/" + id + "/documents/" + doc + "?x=1" + REASON)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertThat(file).isEqualTo(PDF);

    api.download(
            "legacyaudit", INQUIRY + "/export?companyId=" + company + "&invoiceNo=I" + t + REASON)
        .andExpect(status().isOk());
    api.doGet("legacyaudit", INQUIRY + "/access-log?companyId=" + company)
        .andExpect(status().isForbidden());

    JsonNode log =
        mig.get(
            "legacyrev", INQUIRY + "/access-log?companyId=" + company + "&username=legacyaudit");
    List<String> actions = log.get("content").findValuesAsText("action");
    assertThat(actions).contains("SEARCH", "VIEW", "DOWNLOAD", "EXPORT");
    assertThat(log.get("content").get(0).get("reasonCode").asText()).isEqualTo("AUDIT");
    assertThatThrownBy(
            () -> jdbc.update("delete from mig_access_log where username = 'legacyaudit'"))
        .hasMessageContaining("cannot be changed");

    ReportResult report =
        as.run(
            "legacyrev",
            () -> reports.run("MIG-ACCESS-LOG", Map.of("companyId", String.valueOf(company))));
    assertThat(report.rows()).anyMatch(r -> "legacyaudit".equals(r.cells().get("username")));
  }

  @Test
  void aRolledBackArchiveBatchHidesItsRecords() throws Exception {
    String t = LegacyInvoiceFixtures.token();
    String batchNo = loadRecords(t, "01", "PAID").get("batchNo").asText();
    mig.post("miglead", BASE + "/batches/" + batchNo + "/rollback", Map.of("reason", "Wrong file"));
    mig.post(
        "migrecon", BASE + "/batches/" + batchNo + "/rollback/approve", Map.of("comment", "Ok"));
    assertThat(
            mig.get(
                    "legacyaudit",
                    INQUIRY + "/records?companyId=" + company + "&receiptNo=AR-" + t + REASON)
                .get("content"))
        .isEmpty();
    loadRecords(t, "02", "PAID IN FULL");
    assertThat(
            mig.get(
                    "legacyaudit",
                    INQUIRY + "/records?companyId=" + company + "&receiptNo=AR-" + t + REASON)
                .get("content"))
        .hasSize(1);
  }
}
