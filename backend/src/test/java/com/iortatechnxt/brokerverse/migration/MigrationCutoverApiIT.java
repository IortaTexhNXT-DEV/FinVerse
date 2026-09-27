package com.iortatechnxt.brokerverse.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Cutover and run-off (wave DM2-B): a production cut-over plan with the runbook tasks relative to
 * the go-live date and their dependencies, the go / no-go criteria measured by the system or
 * recorded with evidence, the board's decision signing gate G7, the runbook workbook, the monthly
 * run-off snapshot and the decommissioning checklists with the measured legacy-context criteria.
 */
@IntegrationTest
class MigrationCutoverApiIT {

  private static final String BASE = "/api/v1/migration";

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
  void setUp() {
    company = data.company().getId();
    mig = new MigrationTestSupport(api, mvc, users, json, jdbc, company);
  }

  @Test
  void aProductionPlanRunsItsRunbookAndTheBoardDecides() throws Exception {
    JsonNode plan =
        mig.post(
            "miglead",
            BASE + "/cutover/plans?companyId=" + company,
            Map.of(
                "name",
                "Production cut-over",
                "kind",
                "PRODUCTION",
                "environment",
                "PROD",
                "goLiveDate",
                "2028-01-03"));
    String planNo = plan.get("planNo").asText();
    JsonNode detail = mig.get("miglead", BASE + "/cutover/plans/" + planNo);
    assertThat(detail.get("tasks").size()).isGreaterThan(10);
    assertThat(detail.get("criteria")).hasSize(12);
    assertThat(detail.get("tasks").get(0).get("plannedStart").asText()).startsWith("2027-12-20");

    api.doPost(
            "miglead",
            BASE + "/cutover/plans/" + planNo + "/tasks/2",
            Map.of("status", "IN_PROGRESS"))
        .andExpect(status().isUnprocessableEntity());
    mig.post(
        "miglead",
        BASE + "/cutover/plans/" + planNo + "/tasks/1",
        Map.of("status", "DONE", "note", "Reference data accepted"));
    assertThat(
            mig.post(
                    "miglead",
                    BASE + "/cutover/plans/" + planNo + "/tasks/2",
                    Map.of("status", "IN_PROGRESS"))
                .get("status")
                .asText())
        .isEqualTo("IN_PROGRESS");

    JsonNode measured = mig.post("miggonogo", BASE + "/cutover/plans/" + planNo + "/measure", null);
    assertThat(measured.findValuesAsText("measuredValue")).hasSizeGreaterThanOrEqualTo(7);
    api.doPost(
            "miglead",
            BASE + "/cutover/plans/" + planNo + "/criteria/1",
            Map.of("met", true, "note", "x"))
        .andExpect(status().isUnprocessableEntity());
    mig.post(
        "miglead",
        BASE + "/cutover/plans/" + planNo + "/criteria/9",
        Map.of("met", true, "note", "Snapshot taken 01-Jan 05:40 and restored in staging"));

    api.doPost("miggonogo", BASE + "/cutover/plans/" + planNo + "/decision", Map.of("go", true))
        .andExpect(status().isUnprocessableEntity());
    JsonNode decision =
        mig.post(
            "miggonogo",
            BASE + "/cutover/plans/" + planNo + "/decision",
            Map.of("go", false, "comment", "Clearing not at zero for one branch"));
    assertThat(decision.get("decision").asText()).isEqualTo("NO_GO");
    assertThat(
            jdbc.queryForObject(
                "select decision from mig_signoff where gate = 'G7' and cutover_plan_id ="
                    + " (select id from mig_cutover_plan where plan_no = ?)",
                String.class,
                planNo))
        .isEqualTo("REJECTED");

    api.download("miglead", BASE + "/cutover/plans/" + planNo + "/runbook")
        .andExpect(status().isOk());
    Map<String, String> params = Map.of("companyId", String.valueOf(company));
    ReportResult tasks = as.run("miglead", () -> reports.run("MIG-CUTOVER-STATUS", params));
    assertThat(tasks.rows()).anyMatch(r -> planNo.equals(r.cells().get("plan_no")));
    ReportResult gonogo = as.run("miglead", () -> reports.run("MIG-GONOGO", params));
    assertThat(gonogo.rows()).anyMatch(r -> planNo.equals(r.cells().get("plan_no")));
  }

  @Test
  void runoffAndDecommissioningAreMeasured() throws Exception {
    JsonNode cohorts =
        mig.post(
            "miglead", BASE + "/runoff/snapshot?companyId=" + company + "&date=2028-02-01", null);
    assertThat(cohorts.isArray()).isTrue();
    mig.get("miglead", BASE + "/runoff?companyId=" + company);

    JsonNode checklists = mig.get("miglead", BASE + "/decommission?companyId=" + company);
    List<String> criteria = checklists.findValuesAsText("criterion");
    assertThat(criteria).contains("NO_LEGACY_INVOICE", "NO_LEGACY_UPP", "LEGACY_ACCOUNTS_ZERO");
    JsonNode opened =
        mig.post("miglead", BASE + "/decommission/systems/ISYS?companyId=" + company, null);
    assertThat(opened.size()).isEqualTo(9);
    api.doPost("miglead", BASE + "/decommission/systems/ISYS?companyId=" + company, null)
        .andExpect(status().isUnprocessableEntity());
    long id = opened.get(0).get("id").asLong();
    api.doPost("miglead", BASE + "/decommission/items/" + id, Map.of("status", "MET"))
        .andExpect(status().isUnprocessableEntity());
    assertThat(
            mig.post(
                    "miglead",
                    BASE + "/decommission/items/" + id,
                    Map.of(
                        "status", "MET", "evidence", "Final extracts reconciled in MGB-2028-0001"))
                .get("status")
                .asText())
        .isEqualTo("MET");

    Map<String, String> params = Map.of("companyId", String.valueOf(company));
    as.run("miglead", () -> reports.run("MIG-RUNOFF", params));
    as.run("miglead", () -> reports.run("MIG-LEGACY-POSITIONS", params));
  }
}
