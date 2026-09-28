package com.iortatechnxt.brokerverse.migration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.support.Api;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Drives the migration console in the integration tests: extracts built from the layout in force
 * (header from the template download) with their control file, batches taken through the gates, and
 * the objects a batch depends on marked accepted.
 */
final class MigrationTestSupport {

  static final String BASE = "/api/v1/migration";

  private final Api api;
  private final MockMvc mvc;
  private final UserDetailsService users;
  private final ObjectMapper json;
  private final JdbcTemplate jdbc;
  private final long company;

  MigrationTestSupport(
      Api api,
      MockMvc mvc,
      UserDetailsService users,
      ObjectMapper json,
      JdbcTemplate jdbc,
      long company) {
    this.api = api;
    this.mvc = mvc;
    this.users = users;
    this.json = json;
    this.jdbc = jdbc;
    this.company = company;
  }

  JsonNode post(String user, String url, Object body) throws Exception {
    return api.read(api.doPost(user, url, body).andExpect(status().isOk()));
  }

  JsonNode get(String user, String url) throws Exception {
    return api.read(api.doGet(user, url).andExpect(status().isOk()));
  }

  List<String> header(String layout) throws Exception {
    String csv =
        api.download("migsteward", BASE + "/templates/layouts/" + layout)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return List.of(csv.lines().findFirst().orElseThrow().split(","));
  }

  /**
   * Uploads an extract of a layout; the hash total is the count of distinct keys.
   *
   * @return the extract
   */
  JsonNode upload(
      String object,
      String layout,
      String fileName,
      List<Map<String, String>> rows,
      List<String> keys)
      throws Exception {
    List<String> cols = header(layout);
    StringBuilder csv = new StringBuilder(String.join(",", cols)).append('\n');
    Set<String> distinct = new HashSet<>();
    List<String> hashCols = hashColumns(layout, keys);
    for (Map<String, String> r : rows) {
      List<String> cells = new ArrayList<>();
      cols.forEach(c -> cells.add(r.getOrDefault(c, "")));
      csv.append(String.join(",", cells)).append('\n');
      distinct.add(String.join("|", hashCols.stream().map(k -> r.getOrDefault(k, "")).toList()));
    }
    byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
    return send(object, layout, fileName, content, rows.size(), distinct.size());
  }

  /**
   * Uploads an extract of a layout as the guided Excel load template of the console, filled in
   * below its example row and uploaded as it is.
   *
   * @return the extract
   */
  JsonNode uploadWorkbook(
      String object,
      String layout,
      String fileName,
      List<Map<String, String>> rows,
      List<String> keys)
      throws Exception {
    byte[] template =
        api.download("migsteward", BASE + "/templates/layouts/" + layout + "?format=xlsx")
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    Set<String> distinct = new HashSet<>();
    List<String> hashCols = hashColumns(layout, keys);
    byte[] content;
    try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb =
            new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                new java.io.ByteArrayInputStream(template));
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
      org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheetAt(0);
      org.apache.poi.ss.usermodel.Row header = null;
      for (org.apache.poi.ss.usermodel.Row row : sheet) {
        if (row.getCell(0) != null
            && com.iortatechnxt.brokerverse.common.excel.GuidedTables.HEADER_CORNER.equals(
                row.getCell(0).getStringCellValue())) {
          header = row;
        }
      }
      if (header == null) {
        throw new AssertionError("The load template of " + layout + " has no header row");
      }
      int next = sheet.getLastRowNum() + 1;
      for (Map<String, String> r : rows) {
        org.apache.poi.ss.usermodel.Row row = sheet.createRow(next++);
        for (int c = 1; c < header.getLastCellNum(); c++) {
          String name =
              com.iortatechnxt.brokerverse.common.excel.GuidedTables.header(
                  header.getCell(c).getStringCellValue());
          row.createCell(c).setCellValue(r.getOrDefault(name, ""));
        }
        distinct.add(String.join("|", hashCols.stream().map(k -> r.getOrDefault(k, "")).toList()));
      }
      wb.write(out);
      content = out.toByteArray();
    }
    return send(object, layout, fileName, content, rows.size(), distinct.size());
  }

  /** Posts a data file with its control file (row count, hash total, checksum). */
  private JsonNode send(
      String object, String layout, String fileName, byte[] content, int rowCount, int hashTotal)
      throws Exception {

    String[] parts = fileName.split("_");
    String source = parts[1];
    String day =
        parts[2].substring(0, 4) + "-" + parts[2].substring(4, 6) + "-" + parts[2].substring(6, 8);
    String head =
        object
            + ","
            + layout
            + ","
            + source
            + ","
            + fileName
            + ","
            + day
            + " 18:00:00,"
            + day
            + " 19:00:00,extract,";
    String control =
        "object,layout,source_system,data_file,as_of,extracted_at,extracted_by,"
            + "measure,column_name,currency,filter,value\n"
            + head
            + "ROW_COUNT,,,,"
            + rowCount
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
                            fileName.replaceAll("\\.(csv|xlsx)$", "_CONTROL.csv"),
                            "text/csv",
                            control.getBytes(StandardCharsets.UTF_8)))
                    .param("companyId", String.valueOf(company))
                    .param("objectCode", object)
                    .with(user(users.loadUserByUsername("migops")))
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(body);
  }

  /** The columns of the layout's distinct-count hash total (row count: every row distinct). */
  private List<String> hashColumns(String layout, List<String> keys) {
    Map<String, Object> l =
        jdbc.queryForMap(
            "select hash_rule, coalesce(hash_columns, key_columns) as cols from mig_layout"
                + " where code = ? and status = 'FROZEN'",
            layout);
    if ("ROW_COUNT".equals(l.get("hash_rule"))) {
      return keys;
    }
    return List.of(((String) l.get("cols")).split(","));
  }

  private static String sha256(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }

  /** Signs the reconciliation (G5) and the acceptance (G6) of a loaded batch. */
  void accept(String batchNo) throws Exception {
    JsonNode recon = get("migrecon", BASE + "/batches/" + batchNo + "/reconciliation");
    if (recon.get("breakCount").asInt() > 0) {
      throw new AssertionError("Reconciliation breaks of " + batchNo + ": " + recon.get("lines"));
    }
    post(
        "migrecon",
        BASE + "/batches/" + batchNo + "/signoff/reconciliation",
        Map.of("approve", true));
    post(
        "migowner",
        BASE + "/batches/" + batchNo + "/signoff/acceptance",
        Map.of("approve", true, "role", "DATA_OWNER"));
    post(
        "miglead",
        BASE + "/batches/" + batchNo + "/signoff/acceptance",
        Map.of("approve", true, "role", "DATA_MIGRATION_LEAD"));
  }

  /** The rows of a batch with their status and message (assertion messages). */
  String rows(String batchNo) throws Exception {
    StringBuilder out = new StringBuilder();
    get("migops", BASE + "/batches/" + batchNo + "/rows?size=100")
        .get("content")
        .forEach(
            r ->
                out.append(r.get("legacyKey").asText())
                    .append(' ')
                    .append(r.get("status").asText())
                    .append(' ')
                    .append(r.path("message").asText())
                    .append("; "));
    get("migops", BASE + "/batches/" + batchNo + "/issues?size=100")
        .get("content")
        .forEach(
            i ->
                out.append(i.get("ruleCode").asText())
                    .append(' ')
                    .append(i.get("message").asText())
                    .append("; "));
    return out.toString();
  }

  /** Marks the objects accepted in this environment (dependencies of the object under test). */
  void accepted(String... objects) {
    for (String o : objects) {
      jdbc.update(
          "insert into mig_batch (company_id, batch_no, object_code, environment_class, mode, status,"
              + " signed_off_at, created_at, created_by)"
              + " select ?, ?, ?, 'NON_PRODUCTION', 'FULL', 'SIGNED_OFF', now(), now(), 'miglead'"
              + " where not exists (select 1 from mig_batch where company_id = ? and object_code = ?"
              + " and status = 'SIGNED_OFF')",
          company,
          "MGB-T-" + o + "-" + (System.nanoTime() % 100000),
          o,
          company,
          o);
    }
  }

  void signMapping(String object) throws Exception {
    post(
        "migowner",
        BASE + "/signoffs/mapping/" + object + "?companyId=" + company,
        Map.of("approve", true, "comment", "Mapping reviewed"));
  }

  /** Plans, validates, signs G3 and G4 and loads a batch of extracts. */
  JsonNode load(String object, List<String> extractNos) throws Exception {
    String batchNo =
        post(
                "migops",
                BASE + "/batches?companyId=" + company,
                Map.of("objectCode", object, "extractNos", extractNos))
            .get("batchNo")
            .asText();
    post("migops", BASE + "/batches/" + batchNo + "/validate", Map.of());
    post(
        "migsteward",
        BASE + "/batches/" + batchNo + "/signoff/validation",
        Map.of("approve", true));
    post("miglead", BASE + "/batches/" + batchNo + "/approve-load", Map.of("comment", "Go"));
    return post("migops", BASE + "/batches/" + batchNo + "/load", Map.of());
  }
}
