package com.iortatechnxt.brokerverse.csf;

import static com.iortatechnxt.brokerverse.csf.CsfFixtures.AGENT;
import static com.iortatechnxt.brokerverse.csf.CsfFixtures.MANAGEMENT;
import static com.iortatechnxt.brokerverse.csf.CsfFixtures.SUPERVISOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.csf.report.AgentActivityReport;
import com.iortatechnxt.brokerverse.csf.report.ContactChangesReport;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * CSF reports with their exports and the API under the CSF permissions (FR-CSF-002, 041, 042; test
 * plan TC-CSF-002, 041, 042).
 */
@IntegrationTest
class CsfReportsApiIT {

  @Autowired private CsfFixtures fx;
  @Autowired private ReportService reports;
  @Autowired private Api api;
  @Autowired private AsUser as;

  private Map<String, String> params(Map<String, String> extra) {
    Map<String, String> p = new HashMap<>();
    p.put("companyId", fx.company().toString());
    p.put("fromDate", "2020-01-01");
    p.put("toDate", "2030-12-31");
    p.putAll(extra);
    return p;
  }

  private String url(String path) {
    return "/api/v1/csf" + path + (path.contains("?") ? "&" : "?") + "companyId=" + fx.company();
  }

  private Client verifiedAndChanged() throws Exception {
    Client c = fx.client(true);
    JsonNode v =
        api.read(
            api.doPost(
                    AGENT,
                    url("/clients/" + c.getId() + "/verifications"),
                    Map.of(
                        "channel",
                        "EMAIL",
                        "checks",
                        List.of(
                            Map.of("code", "ADDRESS", "matched", true),
                            Map.of("code", "EMAIL", "matched", true))))
                .andExpect(status().isCreated()));
    assertThat(v.get("result").asText()).isEqualTo("PASSED");
    api.doPost(
            AGENT,
            url("/clients/" + c.getId() + "/contact-changes"),
            Map.of(
                "verificationId",
                v.get("id").asLong(),
                "reasonCode",
                "CORRECTION",
                "values",
                Map.of("ADDRESS_LINE", "12 New Street")))
        .andExpect(status().isCreated());
    return c;
  }

  @Test
  void bothReportsRunAndExportForManagement() throws Exception {
    Client c = verifiedAndChanged();
    for (String code : List.of(ContactChangesReport.CODE, AgentActivityReport.CODE)) {
      ReportResult result = as.run(MANAGEMENT, () -> reports.run(code, params(Map.of())));
      assertThat(result.rows()).as(code).isNotEmpty();
      for (ExportFormat format : List.of(ExportFormat.PDF, ExportFormat.XLSX, ExportFormat.CSV)) {
        assertThat(
                as.run(MANAGEMENT, () -> reports.export(code, params(Map.of()), format)).content())
            .as(code + " " + format)
            .isNotEmpty();
      }
    }
    ReportResult changes =
        as.run(
            SUPERVISOR,
            () -> reports.run(ContactChangesReport.CODE, params(Map.of("client", c.getCode()))));
    assertThat(changes.rows())
        .filteredOn(r -> r.kind() == RowKind.DETAIL)
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.cells().get("field")).isEqualTo("Address line");
              assertThat(r.cells().get("new_value")).isEqualTo("12 New Street");
              assertThat(r.cells().get("verification")).isEqualTo("Passed (2 of 2 required)");
              assertThat(r.cells().get("sync_status")).isEqualTo("Not configured");
              assertThat(r.cells().get("agent")).isEqualTo("Clarissa Contact Center Agent");
            });
    ReportResult detail =
        as.run(
            SUPERVISOR,
            () ->
                reports.run(
                    AgentActivityReport.CODE,
                    params(
                        Map.of(
                            "layout",
                            "Detail",
                            "action",
                            "Contact change",
                            "client",
                            c.getCode()))));
    assertThat(detail.rows()).filteredOn(r -> r.kind() == RowKind.DETAIL).hasSize(1);
    assertThatThrownBy(
            () ->
                as.run(
                    MANAGEMENT,
                    () ->
                        reports.run(
                            ContactChangesReport.CODE,
                            params(Map.of("fromDate", "2026-10-01", "toDate", "2026-09-01")))))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void theCsfPermissionsSeparateAgentsSupervisorsAndManagement() throws Exception {
    Client c = fx.client(true);
    String base = "/clients/" + c.getId();
    for (String user : List.of(AGENT, SUPERVISOR, MANAGEMENT)) {
      api.doGet(user, url(base)).andExpect(status().isOk());
      api.doGet(user, url(base + "/accounts")).andExpect(status().isOk());
      api.doGet(user, url(base + "/payments")).andExpect(status().isOk());
      api.doGet(user, url(base + "/renewal-advices")).andExpect(status().isOk());
      api.doGet(user, url(base + "/epolicies")).andExpect(status().isOk());
      api.doGet(user, url(base + "/documents")).andExpect(status().isOk());
      api.doGet(user, url(base + "/contact-changes")).andExpect(status().isOk());
      api.doGet(user, url("/search?keyType=CLIENT_ID&q=" + c.getCode())).andExpect(status().isOk());
      api.doGet(user, url("/contact-changes")).andExpect(status().isOk());
    }
    api.doPost(
            MANAGEMENT,
            url(base + "/verifications"),
            Map.of(
                "channel", "HOTLINE", "checks", List.of(Map.of("code", "EMAIL", "matched", true))))
        .andExpect(status().isForbidden());
    api.doPost(
            AGENT,
            url(base + "/resend-advice"),
            Map.of("documentId", 1, "recipient", "someone@else.ph", "reason", "abroad"))
        .andExpect(status().isNotFound());
    api.doGet("ao", url(base)).andExpect(status().isForbidden());
    api.doGet(AGENT, "/api/v1/reports").andExpect(status().isForbidden());
    assertThatThrownBy(
            () -> as.run(AGENT, () -> reports.run(ContactChangesReport.CODE, params(Map.of()))))
        .isInstanceOf(RuntimeException.class);
    JsonNode found = api.read(api.doGet(AGENT, url("/search?keyType=CLIENT_ID&q=" + c.getCode())));
    assertThat(found.get("clients").get(0).get("id").asLong()).isEqualTo(c.getId());
    JsonNode summary = api.read(api.doGet(AGENT, url(base)));
    assertThat(summary.get("contact").get("email").asText()).isEqualTo(c.getEmail());
  }
}
