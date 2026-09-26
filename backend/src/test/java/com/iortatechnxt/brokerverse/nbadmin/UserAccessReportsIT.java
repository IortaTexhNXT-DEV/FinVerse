package com.iortatechnxt.brokerverse.nbadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.nbadmin.report.AccessAuditLogReport;
import com.iortatechnxt.brokerverse.nbadmin.report.AccessRequestsReport;
import com.iortatechnxt.brokerverse.nbadmin.report.GroupMembersReport;
import com.iortatechnxt.brokerverse.nbadmin.report.GroupProfileReport;
import com.iortatechnxt.brokerverse.nbadmin.report.UserAccessReport;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.security.api.dto.UserRequest;
import com.iortatechnxt.brokerverse.security.service.ChangeAuthority;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The five user access reports of BRD-11 wave U1-B (BRD 1.008, 3.003.1-3, 4.003.1; FR-UA-060 to
 * 063): contents, from / to values of a group profile change with its approver and request number,
 * the as-of view, the optional log-ins and log-outs, the date checks, the permission and the PDF,
 * XLSX and CSV exports.
 */
@IntegrationTest
class UserAccessReportsIT {

  private static final AtomicLong IDS = new AtomicLong(System.nanoTime() % 1_000_000_000L);
  private static final String BADMIN = "badmin";
  private static final String MKT_TL = "MKT_TL";
  private static final String PROCESSING_TL = "PROCESSING_TL";
  private static final String PASSWORD = "Initial!Passw0rd";

  @Autowired private ReportService reports;
  @Autowired private UserAdminService admin;
  @Autowired private AsUser as;
  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;

  private static String today() {
    return LocalDate.now(ZoneId.of("Asia/Manila")).toString();
  }

  private ReportResult run(String code, Map<String, String> params) {
    return as.run(BADMIN, () -> reports.run(code, params));
  }

  private static List<Map<String, Object>> details(ReportResult result) {
    return result.rows().stream()
        .filter(r -> r.kind() == RowKind.DETAIL)
        .map(ReportRow::cells)
        .toList();
  }

  /** A user enrolled as Marketing Team Leader and moved to Processing Team Leader by requests. */
  private String movedUser() {
    String username = "u1br" + IDS.incrementAndGet();
    as.run(
        "admin",
        () -> {
          var created =
              admin.createUser(
                  new UserRequest(
                      username, "Report Tester", null, null, null, Set.of(MKT_TL), true),
                  PASSWORD,
                  ChangeAuthority.request("AR-U1B-" + username, "uamapprover"));
          return admin.updateUser(
              created.getId(),
              new UserRequest(
                  username, "Report Tester", null, null, null, Set.of(PROCESSING_TL), true),
              ChangeAuthority.request("AR-U1B-M-" + username, "uamapprover"));
        });
    return username;
  }

  @Test
  void theAuditLogShowsTheProfileChangeWithFromAndToValues() throws Exception {
    String username = movedUser();
    List<Map<String, Object>> rows =
        details(
            run(
                AccessAuditLogReport.CODE,
                Map.of("from", today(), "to", today(), "user", username)));
    assertThat(rows)
        .anySatisfy(
            r -> {
              assertThat(r.get("activity")).isEqualTo("Modify User Group Profile of " + username);
              assertThat(r.get("fromValue")).isEqualTo("Marketing Team Leader / Head (approver)");
              assertThat(r.get("toValue")).isEqualTo("Processing Team Leader");
              assertThat(r.get("approvedBy")).isEqualTo("uamapprover");
              assertThat(r.get("requestNo")).isEqualTo("AR-U1B-M-" + username);
              assertThat(r.get("doneBy")).isEqualTo("admin");
            });

    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username, "password", "x"))))
        .andExpect(status().isUnauthorized());
    Map<String, String> withSignIns =
        Map.of("from", today(), "to", today(), "user", username, "includeSignIns", "true");
    assertThat(details(run(AccessAuditLogReport.CODE, withSignIns)))
        .anySatisfy(r -> assertThat(r.get("activity")).isEqualTo("Failed Log-in " + username));
    assertThat(
            details(
                run(
                    AccessAuditLogReport.CODE,
                    Map.of("from", today(), "to", today(), "user", username))))
        .noneSatisfy(r -> assertThat((String) r.get("activity")).startsWith("Failed Log-in"));

    assertThatThrownBy(
            () ->
                run(
                    AccessAuditLogReport.CODE,
                    Map.of(
                        "from", today(), "to", LocalDate.parse(today()).minusDays(1).toString())))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("The end date must be on or after the start date");
  }

  @Test
  void theUserAccessReportAndTheMembershipShowTheProfilesAsOfADate() {
    String username = movedUser();
    Map<String, Object> user =
        details(run(UserAccessReport.CODE, Map.of("asOf", today(), "groupProfile", PROCESSING_TL)))
            .stream()
            .filter(r -> username.equals(r.get("userId")))
            .findFirst()
            .orElseThrow();
    assertThat(user.get("profiles")).isEqualTo("Processing Team Leader");
    assertThat(user.get("createdBy")).isEqualTo("uamapprover (AR-U1B-" + username + ")");
    assertThat(user.get("modifiedBy")).isEqualTo("uamapprover (AR-U1B-M-" + username + ")");
    assertThat(user.get("status")).isEqualTo("Active");
    assertThat(details(run(UserAccessReport.CODE, Map.of("asOf", today(), "groupProfile", MKT_TL))))
        .noneMatch(r -> username.equals(r.get("userId")));
    assertThat(
            details(
                run(
                    UserAccessReport.CODE,
                    Map.of("asOf", LocalDate.parse(today()).minusDays(1).toString()))))
        .noneMatch(r -> username.equals(r.get("userId")));
    assertThatThrownBy(
            () ->
                run(
                    UserAccessReport.CODE,
                    Map.of("asOf", LocalDate.parse(today()).plusDays(1).toString())))
        .hasMessage("The as-of date cannot be in the future");

    assertThat(details(run(GroupMembersReport.CODE, Map.of("groupProfile", PROCESSING_TL))))
        .anySatisfy(
            r -> {
              assertThat(r.get("username")).isEqualTo(username);
              assertThat(r.get("addedBy")).isEqualTo("uamapprover (AR-U1B-M-" + username + ")");
            });
    assertThat(details(run(GroupMembersReport.CODE, Map.of("groupProfile", MKT_TL))))
        .noneMatch(r -> username.equals(r.get("username")));
  }

  @Test
  void theGroupProfileAndRequestReportsRunAndEveryReportExports() {
    List<Map<String, Object>> tasks =
        details(run(GroupProfileReport.CODE, Map.of("groupProfile", MKT_TL, "area", "CLIENTS")));
    assertThat(tasks)
        .isNotEmpty()
        .allSatisfy(r -> assertThat(r.get("access")).isIn("With Access", "No Access"));
    assertThat(details(run(GroupProfileReport.CODE, Map.of("groupProfile", MKT_TL))))
        .anySatisfy(r -> assertThat(r.get("access")).isEqualTo("With Access"));
    assertThat(details(run(AccessRequestsReport.CODE, Map.of("from", "2020-01-01", "to", today()))))
        .isNotEmpty();

    for (String code :
        List.of(
            UserAccessReport.CODE,
            GroupProfileReport.CODE,
            GroupMembersReport.CODE,
            AccessAuditLogReport.CODE,
            AccessRequestsReport.CODE)) {
      for (ExportFormat format : List.of(ExportFormat.PDF, ExportFormat.XLSX, ExportFormat.CSV)) {
        assertThat(as.run(BADMIN, () -> reports.export(code, Map.of(), format)).content())
            .as(code + " " + format)
            .isNotEmpty();
      }
    }
  }

  @Test
  void onlyHoldersOfTheReportPermissionRunThem() throws Exception {
    assertThatThrownBy(
            () -> as.run("requestor", () -> reports.run(UserAccessReport.CODE, Map.of())))
        .isInstanceOf(AccessDeniedException.class);
    api.doPost("uamapprover", "/api/v1/reports/" + GroupMembersReport.CODE + "/run", Map.of())
        .andExpect(status().isOk());
  }
}
