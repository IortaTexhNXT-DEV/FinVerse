package com.iortatechnxt.brokerverse.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.nbadmin.report.AccessAuditLogReport;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.SignInPasswords;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/**
 * BDOI FRS FRUM.008.01 to FRUM.008.03 and FRPM.021.01: the Audit Trail shows the module, the
 * Windows ID, the role at the time and the source address, filters by action and reference number,
 * refuses an end date before the start date, and exports as "Audit Logs_MMDDYYYY"; the User Access
 * Audit Log follows the fields of BDOI's audit log template and lists inactivity and time-outs.
 */
@IntegrationTest
class AuditTrailBdoiIT {

  private static final String ADDRESS = String.join(".", "192", "0", "2", "33");

  @Autowired private MockMvc mvc;
  @Autowired private Api api;
  @Autowired private SignInPasswords passwords;

  @Test
  void theAuditTrailShowsTheRoleAndTheAddressAndFiltersByActionAndReference() throws Exception {
    signInAndReportInactivity("auditor");
    String today = LocalDate.now(BusinessClock.zone()).toString();
    api.doGet(
            "auditor",
            "/api/v1/audit-logs?from="
                + today
                + "&to="
                + today
                + "&action=INACTIVITY&entityId=audit&sort=username&direction=asc")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].action").value("INACTIVITY"))
        .andExpect(jsonPath("$.content[0].actionLabel").value("Inactivity"))
        .andExpect(jsonPath("$.content[0].module").value("User Access Maintenance"))
        .andExpect(jsonPath("$.content[0].ipAddress").value(ADDRESS))
        .andExpect(jsonPath("$.content[0].roleNames").value(Matchers.not(Matchers.emptyString())));
    api.doGet("auditor", "/api/v1/audit-logs?from=" + today + "&to=2020-01-01")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.detail").value("The end date must be on or after the start date"));
  }

  @Test
  void theAuditLogExportIsNamedAuditLogsWithTheExtractionDate() throws Exception {
    String today = LocalDate.now(BusinessClock.zone()).toString();
    String date =
        BusinessClock.today(Clock.systemUTC()).format(DateTimeFormatter.ofPattern("MMddyyyy"));
    String csv =
        api.doPost(
                "auditor",
                "/api/v1/reports/CTL-AUDIT/export?format=CSV",
                Map.of("fromDate", today, "toDate", today))
            .andExpect(status().isOk())
            .andExpect(
                header()
                    .string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        Matchers.containsString("Audit Logs_" + date + ".csv")))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    assertThat(csv).contains("User Id (Windows ID)", "Role", "IP Address", "From (Old Value)");
  }

  @Test
  void theUserAccessAuditLogFollowsBdoisTemplateAndListsInactivity() throws Exception {
    signInAndReportInactivity("auditor");
    String today = LocalDate.now(BusinessClock.zone()).toString();
    String body =
        api.doPost(
                "auditor",
                "/api/v1/reports/" + AccessAuditLogReport.CODE + "/run",
                Map.of("from", today, "to", today, "includeSignIns", "true"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    assertThat(body)
        .contains(
            "\"Module\"", "\"User Group Profile\"", "\"IP Address\"", "Inactivity of auditor");
  }

  private void signInAndReportInactivity(String username) throws Exception {
    String login =
        mvc.perform(passwords.login(username))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = JsonPath.read(login, "$.accessToken");
    mvc.perform(
            post("/api/v1/auth/session/inactivity")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .with(
                    r -> {
                      r.setRemoteAddr(ADDRESS);
                      return r;
                    }))
        .andExpect(status().isNoContent());
  }
}
