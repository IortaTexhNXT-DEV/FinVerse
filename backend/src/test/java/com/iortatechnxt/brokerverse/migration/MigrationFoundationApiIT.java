package com.iortatechnxt.brokerverse.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.common.security.UserDisplayNames;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Data Migration foundation (wave DM0): the reference-data object R01 from extract to accepted
 * object through the console API (intake with control file, validation with an unmapped code
 * excluded by the data owner, the gates G2 to G6 with their segregation, load, reconciliation),
 * rollback of a batch, decisions (G1), code map versions, the load templates exported from the
 * layouts in force and the home tiles.
 */
@IntegrationTest
class MigrationFoundationApiIT {

  private static final String BASE = "/api/v1/migration";
  private static final String R01_HEADER =
      "list_type,code,description,parent_code,active_flag,effective_from,effective_to,used_count";

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private ObjectMapper json;
  @Autowired private TestData data;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private UserDisplayNames names;

  private long company() {
    return data.company().getId();
  }

  private static String sha256(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }

  private JsonNode upload(String fileName, List<String> rows, int hashTotal) throws Exception {
    return upload(fileName, rows, hashTotal, rows.size());
  }

  private JsonNode upload(String fileName, List<String> rows, int hashTotal, int declaredRows)
      throws Exception {
    byte[] content =
        (R01_HEADER + "\n" + String.join("\n", rows) + "\n").getBytes(StandardCharsets.UTF_8);
    String head =
        "R01,R01,QPS," + fileName + ",2026-11-20 18:00:00,2026-11-20 19:00:00,qps-extract,";
    String control =
        "object,layout,source_system,data_file,as_of,extracted_at,extracted_by,"
            + "measure,column_name,currency,filter,value\n"
            + head
            + "ROW_COUNT,,,,"
            + declaredRows
            + "\n"
            + head
            + "HASH_TOTAL,,,,"
            + hashTotal
            + "\n"
            + head
            + "SHA256,,,,"
            + sha256(content)
            + "\n";
    String body =
        mvc.perform(
                multipart(BASE + "/extracts")
                    .file(new MockMultipartFile("file", fileName, "text/csv", content))
                    .file(
                        new MockMultipartFile(
                            "control",
                            fileName.replace(".csv", "_CONTROL.csv"),
                            "text/csv",
                            control.getBytes(StandardCharsets.UTF_8)))
                    .param("companyId", String.valueOf(company()))
                    .param("objectCode", "R01")
                    .with(user(users.loadUserByUsername("migops")))
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(body);
  }

  private JsonNode post(String user, String url, Object body) throws Exception {
    return api.read(api.doPost(user, url, body).andExpect(status().isOk()));
  }

  private JsonNode get(String user, String url) throws Exception {
    return api.read(api.doGet(user, url).andExpect(status().isOk()));
  }

  private String plan(String extractNo) throws Exception {
    return post(
            "migops",
            BASE + "/batches?companyId=" + company(),
            Map.of("objectCode", "R01", "extractNos", List.of(extractNo)))
        .get("batchNo")
        .asText();
  }

  private void signMapping() throws Exception {
    post(
        "migowner",
        BASE + "/signoffs/mapping/R01?companyId=" + company(),
        Map.of("approve", true, "comment", "Layout and lists mapped"));
  }

  private JsonNode loadApproved(String batchNo) throws Exception {
    post(
        "migsteward",
        BASE + "/batches/" + batchNo + "/signoff/validation",
        Map.of("approve", true));
    post("miglead", BASE + "/batches/" + batchNo + "/approve-load", Map.of("comment", "Go"));
    return post("migops", BASE + "/batches/" + batchNo + "/load", Map.of());
  }

  @Test
  void referenceDataTravelsFromExtractToAcceptedObject() throws Exception {
    signMapping();
    String unknown = "ZZ" + Long.toString(System.nanoTime() % 100_000, 36).toUpperCase();
    JsonNode extract =
        upload(
            "R01_QPS_20261120_01.csv",
            List.of(
                "MARKET_SEGMENT,CBG,Consumer Banking,,Y,2020-01-01,,120",
                "MARKET_SEGMENT,COMM,Commercial Banking,,Y,2020-01-01,,80",
                "MARKET_SEGMENT," + unknown + ",Unknown segment,,Y,,,1"),
            3);
    assertThat(extract.get("status").asText()).isEqualTo("STAGED");
    assertThat(extract.get("stagedRows").asInt()).isEqualTo(3);

    String batchNo = plan(extract.get("extractNo").asText());
    JsonNode validated = post("migops", BASE + "/batches/" + batchNo + "/validate", Map.of());
    assertThat(validated.get("status").asText()).isEqualTo("VALIDATED");
    assertThat(validated.get("unmappedCount").asInt()).isEqualTo(1);

    // The unmapped code blocks the validation sign-off until the data owner excludes the row.
    api.doPost(
            "migsteward",
            BASE + "/batches/" + batchNo + "/signoff/validation",
            Map.of("approve", true))
        .andExpect(status().is4xxClientError());
    JsonNode invalid = get("migsteward", BASE + "/batches/" + batchNo + "/rows?status=INVALID");
    List<Long> ids = new ArrayList<>();
    invalid.get("content").forEach(r -> ids.add(r.get("id").asLong()));
    assertThat(ids).hasSize(1);
    post(
        "migowner",
        BASE + "/batches/" + batchNo + "/exclude",
        Map.of("rowIds", ids, "reason", "NOT_NEEDED", "note", "Value retired in legacy"));

    // The operator who validated cannot approve the load.
    post(
        "migsteward",
        BASE + "/batches/" + batchNo + "/signoff/validation",
        Map.of("approve", true));
    api.doPost("migops", BASE + "/batches/" + batchNo + "/approve-load", Map.of())
        .andExpect(status().is4xxClientError());
    post("miglead", BASE + "/batches/" + batchNo + "/approve-load", Map.of("comment", "Go"));
    JsonNode loaded = post("migops", BASE + "/batches/" + batchNo + "/load", Map.of());
    assertThat(loaded.get("status").asText()).isIn("LOADED", "RECONCILED");
    assertThat(loaded.get("counts").get("loaded").asInt()).isEqualTo(2);

    JsonNode recon = get("migrecon", BASE + "/batches/" + batchNo + "/reconciliation");
    assertThat(recon.get("breakCount").asInt()).isZero();
    assertThat(recon.get("lines")).isNotEmpty();

    // The operator of the batch cannot sign its reconciliation.
    api.doPost(
            "migops",
            BASE + "/batches/" + batchNo + "/signoff/reconciliation",
            Map.of("approve", true))
        .andExpect(status().is4xxClientError());
    post(
        "migrecon",
        BASE + "/batches/" + batchNo + "/signoff/reconciliation",
        Map.of("approve", true));
    post(
        "migowner",
        BASE + "/batches/" + batchNo + "/signoff/acceptance",
        Map.of("approve", true, "role", "DATA_OWNER"));
    JsonNode accepted =
        post(
            "miglead",
            BASE + "/batches/" + batchNo + "/signoff/acceptance",
            Map.of("approve", true, "role", "DATA_MIGRATION_LEAD"));
    assertThat(accepted.get("gate").asText()).isEqualTo("G6");
    JsonNode batch = get("miglead", BASE + "/batches/" + batchNo);
    assertThat(batch.get("status").asText()).isEqualTo("SIGNED_OFF");
    assertThat(batch.get("purgeDueOn").isNull()).isFalse();

    JsonNode xref = get("miglead", BASE + "/xref?q=COMM");
    assertThat(xref.get("content").toString()).contains(batchNo);
    JsonNode signoffs = get("miglead", BASE + "/batches/" + batchNo + "/signoffs");
    assertThat(signoffs).hasSizeGreaterThanOrEqualTo(5);
    JsonNode matrix = get("miglead", BASE + "/signoffs/matrix?companyId=" + company());
    assertThat(matrix.toString()).contains(batchNo);
    // Who signed a gate is named, never shown by login id.
    String owner = names.displayName("migowner");
    assertThat(owner).isNotEqualTo("migowner");
    assertThat(matrix.toString()).contains(owner + " (").doesNotContain("migowner (");
    assertThat(get("migops", BASE + "/batches/" + batchNo + "/log")).isNotEmpty();
    api.download("migops", BASE + "/batches/" + batchNo + "/rejects").andExpect(status().isOk());
    api.download("migops", BASE + "/extracts/" + extract.get("extractNo").asText() + "/files/data")
        .andExpect(status().isOk());
    JsonNode tiles = get("miglead", BASE + "/home?companyId=" + company());
    assertThat(tiles.get("batchesByStatus").toString()).contains("SIGNED_OFF");
  }

  @Test
  void aLoadedBatchIsRolledBackAfterApproval() throws Exception {
    signMapping();
    JsonNode extract =
        upload(
            "R01_EBIX_20261121_01.csv".replace("EBIX", "QPS"),
            List.of(
                "CIVIL_STATUS,S,Single,,Y,2020-01-01,,10",
                "CIVIL_STATUS,M,Married,,Y,2020-01-01,,12"),
            2);
    String batchNo = plan(extract.get("extractNo").asText());
    post("migops", BASE + "/batches/" + batchNo + "/validate", Map.of());
    loadApproved(batchNo);
    api.doPost(
            "migops",
            BASE + "/batches/" + batchNo + "/rollback",
            Map.of("reason", "Wrong list extracted"))
        .andExpect(status().isForbidden());
    JsonNode requested =
        post(
            "miglead",
            BASE + "/batches/" + batchNo + "/rollback",
            Map.of("reason", "Wrong list extracted"));
    assertThat(requested.get("status").asText()).isEqualTo("ROLLBACK_REQUESTED");
    JsonNode rolledBack =
        post(
            "migrecon",
            BASE + "/batches/" + batchNo + "/rollback/approve",
            Map.of("comment", "Agreed"));
    assertThat(rolledBack.get("status").asText()).isEqualTo("ROLLED_BACK");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from mig_key_xref x join mig_batch b on b.id = x.batch_id"
                    + " where b.batch_no = ? and x.rolled_back_at is null",
                Integer.class,
                batchNo))
        .isZero();
  }

  @Test
  void anExtractFailingItsControlTotalsIsRejected() throws Exception {
    JsonNode extract =
        upload("R01_QPS_20261122_01.csv", List.of("MARKET_SEGMENT,CORP,Corporate,,Y,,,5"), 7);
    assertThat(extract.get("status").asText()).isEqualTo("REJECTED");
    assertThat(extract.get("rejectMessage").asText()).contains("hash total");
  }

  @Test
  void aRejectedExtractListsEveryFailedCheckWithItsReason() throws Exception {
    JsonNode extract =
        upload("R01_QPS_20261123_01.csv", List.of("MARKET_SEGMENT,CORP,Corporate,,Y,,,5"), 7, 3);
    assertThat(extract.get("status").asText()).isEqualTo("REJECTED");
    assertThat(extract.get("parsedRows").asInt()).isEqualTo(1);
    List<String> checks = new ArrayList<>();
    extract
        .get("rejectChecks")
        .forEach(c -> checks.add(c.get("check").asText() + ": " + c.get("reason").asText()));
    assertThat(checks)
        .hasSize(2)
        .anySatisfy(c -> assertThat(c).startsWith("Row count: The file has 1 row;"))
        .anySatisfy(c -> assertThat(c).startsWith("Hash total: The hash total"));
  }

  @Test
  void theBusinessOwnerDecidesTheClassOfAnObject() throws Exception {
    JsonNode decision =
        post(
            "miglead",
            BASE + "/objects/R10/decision?companyId=" + company(),
            Map.of("conditionMet", false));
    String no = decision.get("decisionNo").asText();
    assertThat(get("migowner", BASE + "/decisions/pending").toString()).contains(no);
    api.doPost("miglead", BASE + "/decisions/" + no + "/approve", Map.of())
        .andExpect(status().isForbidden());
    JsonNode approved =
        post("migowner", BASE + "/decisions/" + no + "/approve", Map.of("comment", "Agreed"));
    assertThat(approved.get("status").asText()).isEqualTo("DECIDED");
    assertThat(get("miglead", BASE + "/objects/R10").get("status").asText()).isEqualTo("DECIDED");
  }

  @Test
  void aCodeMapVersionIsApprovedByTheBusinessOwner() throws Exception {
    JsonNode draft =
        post(
            "migsteward",
            BASE + "/maps/LOV:NATIONALITY/versions?companyId=" + company(),
            Map.of("copyApproved", false, "comment", "First mapping"));
    long id = draft.get("id").asLong();
    post(
        "migsteward",
        BASE + "/maps/versions/" + id + "/entries",
        Map.of(
            "sourceSystem", "QPS", "legacyCode", "PH", "action", "MAP", "targetCode", "FILIPINO"));
    post("migsteward", BASE + "/maps/versions/" + id + "/submit", Map.of());
    api.doPost("migsteward", BASE + "/maps/versions/" + id + "/approve", Map.of())
        .andExpect(status().isForbidden());
    JsonNode approved = post("migowner", BASE + "/maps/versions/" + id + "/approve", Map.of());
    assertThat(approved.toString()).contains("APPROVED");
  }

  @Test
  void loadTemplatesAreExportedFromTheLayoutsInForce() throws Exception {
    String csv =
        api.download("migsteward", BASE + "/templates/layouts/R01")
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(csv).startsWith(R01_HEADER);
    String control =
        api.download("migsteward", BASE + "/templates/control")
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(control).contains("measure");
    byte[] workbook =
        api.download("migsteward", BASE + "/templates/workbook")
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    // Every template is a guided sheet: no separate column or instruction sheets.
    try (XSSFWorkbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(workbook))) {
      List<String> sheets = new ArrayList<>();
      wb.forEach(s -> sheets.add(s.getSheetName()));
      assertThat(sheets.get(0)).isEqualTo("Start here");
      assertThat(sheets).anyMatch(n -> n.startsWith("R01 ")).anyMatch(n -> n.startsWith("F01 "));
      assertThat(String.join("|", sheets)).doesNotContainIgnoringCase("how to fill");
    }
    byte[] layout =
        api.download("migsteward", BASE + "/templates/layouts/R01?format=xlsx")
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    try (XSSFWorkbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(layout))) {
      List<List<String>> table = new ArrayList<>();
      var formatter = new org.apache.poi.ss.usermodel.DataFormatter();
      for (var row : wb.getSheetAt(0)) {
        List<String> cells = new ArrayList<>();
        row.forEach(c -> cells.add(formatter.formatCellValue(c)));
        while (table.size() < row.getRowNum()) {
          table.add(List.of());
        }
        table.add(cells);
      }
      int header =
          com.iortatechnxt.brokerverse.common.excel.GuidedTables.headerRow(table, List.of());
      assertThat(table.get(header - 1).get(0)).isEqualTo("What to enter");
      assertThat(
              String.join(
                  ",",
                  com.iortatechnxt.brokerverse.common.excel.GuidedTables.headers(table.get(header))
                      .subList(1, table.get(header).size())))
          .isEqualTo(R01_HEADER);
    }
    byte[] controlXlsx =
        api.download("migsteward", BASE + "/templates/control?format=xlsx")
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    try (XSSFWorkbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(controlXlsx))) {
      assertThat(wb.getSheetAt(0).getSheetName()).isEqualTo("Control");
    }
  }
}
