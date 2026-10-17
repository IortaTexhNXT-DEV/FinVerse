package com.iortatechnxt.brokerverse.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.report.core.ReportLayoutOptions;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.Json;
import com.iortatechnxt.brokerverse.support.TestData;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The Layout panel of the report runner and the MIS fields catalogue (gap G06, FR-CR-042 and
 * FR-CR-062): a report grouped by two fields shows the group subtotals and counts on screen and in
 * the files, the PDF and Excel files carry the chart, a prescribed report takes no layout, and an
 * MIS field is added by one System Administrator and authorized by another.
 */
@IntegrationTest
class ReportLayoutIT {

  private static final String GL = "fmanager";
  private static final String ADMIN = "admin";
  private static final String STRONG = "Str0ng!Passw0rd";

  @Autowired private Api api;
  @Autowired private TestData data;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private UserAdminService users;
  @Autowired private AsUser as;

  private Map<String, Object> params(String... layout) {
    Map<String, Object> p = new HashMap<>();
    p.put("companyId", data.company().getId().toString());
    for (int i = 0; i + 1 < layout.length; i += 2) {
      p.put(layout[i], layout[i + 1]);
    }
    return p;
  }

  @Test
  void aReportIsGroupedWithSubtotalsAndCountsOnTheScreen() throws Exception {
    JsonNode result =
        api.read(
            api.doPost(
                    GL,
                    "/api/v1/reports/GL-COA/run",
                    params(
                        ReportLayoutOptions.GROUP_1, "status",
                        ReportLayoutOptions.GROUP_2, "postable"))
                .andExpect(status().isOk()));
    assertThat(result.findValuesAsText("label"))
        .anyMatch(l -> l.startsWith("Status : "))
        .anyMatch(l -> l.startsWith("Total Status : ") && l.contains(" record"));
    assertThat(result.path("notes").toString())
        .contains("Layout: grouped by Status, then Postable");
  }

  @Test
  void theFilesCarryTheChart() throws Exception {
    Map<String, Object> chart =
        params(ReportLayoutOptions.GROUP_1, "status", ReportLayoutOptions.CHART, "BAR");
    byte[] pdf =
        api.doPost(GL, "/api/v1/reports/GL-COA/export?format=PDF", chart)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertThat(new String(pdf, StandardCharsets.ISO_8859_1)).contains("/Subtype/Image");
    byte[] xlsx =
        api.doPost(GL, "/api/v1/reports/GL-COA/export?format=XLSX", chart)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      assertThat(wb.getSheet("Chart")).isNotNull();
      assertThat(wb.getSheet("Chart").getDrawingPatriarch().getShapes()).isNotEmpty();
    }
  }

  @Test
  void aChartNeedsAGroupingFieldAndAPrescribedReportTakesNoLayout() throws Exception {
    api.doPost(GL, "/api/v1/reports/GL-COA/run", params(ReportLayoutOptions.CHART, "LINE"))
        .andExpect(status().isUnprocessableEntity());
    JsonNode catalogue = api.read(api.doGet(GL, "/api/v1/reports"));
    JsonNode fixed = null;
    for (JsonNode e : catalogue) {
      if (e.path("layoutFixed").asBoolean()) {
        fixed = e;
        break;
      }
    }
    assertThat(fixed).isNotNull();
    for (JsonNode e : catalogue) {
      if ("GL-COA".equals(e.path("code").asText())) {
        assertThat(e.path("layoutFixed").asBoolean()).isFalse();
      }
    }
  }

  private String otherAdministrator() {
    String username = "misadm" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
    AppUser user =
        as.run(
            ADMIN,
            () ->
                users.createUser(
                    new UserRequest(
                        username, "MIS Admin " + username, null, null, null, Set.of(), true),
                    STRONG));
    jdbc.update(
        "insert into sec_user_role (user_id, role_id) select ?, id from sec_role where code = ?",
        user.getId(),
        "SYSADMIN");
    return username;
  }

  @Test
  void anMisFieldIsAddedAndAuthorizedByAnotherAdministrator() throws Exception {
    jdbc.update(
        "update rpt_mis_field set record_status = 'INACTIVE' where source = 'CLAIM_STATUS'");
    String code = "CLAIM_STATUS_" + ThreadLocalRandom.current().nextInt(1000, 9999);
    JsonNode created =
        api.read(
            api.doPost(
                    ADMIN,
                    "/api/v1/mis-fields",
                    Json.of(
                        "code", code,
                        "label", "Claim Status",
                        "entity", "CLAIM",
                        "source", "CLAIM_STATUS"))
                .andExpect(status().isCreated()));
    long id = created.path("id").asLong();
    assertThat(created.path("recordStatus").asText()).isEqualTo("PENDING_AUTHORIZATION");
    assertThat(created.path("columnKeys").toString()).contains("claim_status");

    api.doPost(
            ADMIN,
            "/api/v1/mis-fields",
            Json.of("code", code, "label", "Again", "entity", "CLAIM", "source", "CLAIM_STATUS"))
        .andExpect(status().isUnprocessableEntity());
    api.doPost(ADMIN, "/api/v1/mis-fields/" + id + "/authorize", null)
        .andExpect(status().isUnprocessableEntity());
    JsonNode authorized =
        api.read(
            api.doPost(otherAdministrator(), "/api/v1/mis-fields/" + id + "/authorize", null)
                .andExpect(status().isOk()));
    assertThat(authorized.path("recordStatus").asText()).isEqualTo("ACTIVE");

    JsonNode listed = api.read(api.doGet(GL, "/api/v1/mis-fields").andExpect(status().isOk()));
    assertThat(listed.findValuesAsText("code")).contains(code, "INSURER", "BUSINESS_ORIGIN");
    api.doPost(GL, "/api/v1/mis-fields/" + id + "/deactivate", null)
        .andExpect(status().isForbidden());
    JsonNode inactive =
        api.read(
            api.doPost(ADMIN, "/api/v1/mis-fields/" + id + "/deactivate", null)
                .andExpect(status().isOk()));
    assertThat(inactive.path("recordStatus").asText()).isEqualTo("INACTIVE");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from master_change_log where entity_type = 'MisField'"
                    + " and entity_id = ?",
                Integer.class,
                id))
        .isPositive();
  }
}
