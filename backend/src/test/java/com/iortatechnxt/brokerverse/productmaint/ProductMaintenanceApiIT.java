package com.iortatechnxt.brokerverse.productmaint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Product Maintenance (package request) endpoints through the full HTTP stack: every read endpoint
 * on the V997 seed requests, a request created and submitted over HTTP, the permission checks, and
 * the three reports in every export format.
 */
@IntegrationTest
class ProductMaintenanceApiIT {

  private static final String BASE = "/api/v1/product-maintenance";
  private static final List<String> REPORTS =
      List.of("PM-PKG-STATUS", "PM-PKG-EXPIRY", "PM-VERSION-HISTORY");

  @Autowired private Api api;
  @Autowired private TestData data;
  @Autowired private ReportService reports;
  @Autowired private AsUser as;

  private String company() {
    return data.company().getId().toString();
  }

  private long seed(String requestNo) throws Exception {
    JsonNode page =
        api.read(
            api.doGet("tsu", BASE + "/requests?companyId=" + company() + "&text=" + requestNo)
                .andExpect(status().isOk()));
    return page.get("content").get(0).get("id").asLong();
  }

  @ParameterizedTest
  @CsvSource({
    "ao, /requests?companyId={c}",
    "ao, /requests?companyId={c}&stage=DRAFT&stage=NEGOTIATION&type=NEW&scope=GENERIC&mine=true",
    "tsu, /requests?companyId={c}&text=PKR&productCode=MTR10&expiringWithin=400",
    "mkttl, /counts?companyId={c}",
    "tsu, /advisories?companyId={c}",
    "tsu, /expiry?companyId={c}",
    "mbs, /expiry?companyId={c}&within=60",
  })
  void listEndpointsRespondOk(String user, String path) throws Exception {
    api.doGet(user, BASE + path.replace("{c}", company())).andExpect(status().isOk());
  }

  @Test
  void aSeedRequestIsReadableWithItsRoundsOutputsAndDocuments() throws Exception {
    long negotiating = seed("PKR-2026-900003");
    String r = BASE + "/requests/" + negotiating;
    api.doGet("ao", r)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("NEGOTIATION"))
        .andExpect(jsonPath("$.requestedTerms.insurers.length()").value(3));
    api.doGet("tsu", r + "/rounds")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].responses.length()").value(3))
        .andExpect(jsonPath("$[1].status").value("SENT"));
    api.doGet("tsu", r + "/rounds/2/comparative")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rows[0].insurerCode").value("INS-MGIC"));
    api.doGet("tsu", r + "/rounds/1/quotation-slip.pdf").andExpect(status().isOk());
    for (String path :
        List.of(
            "/responses/history", "/comparatives", "/requirements", "/advisories", "/form.pdf")) {
      api.doGet("tsu", r + path).andExpect(status().isOk());
    }
    long released = seed("PKR-2026-900006");
    api.doGet("mkttl", BASE + "/requests/" + released + "/advisories")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("SENT"));
    api.doGet("mancom", BASE + "/requests/" + seed("PKR-2026-900004") + "/package-slip.pdf")
        .andExpect(status().isOk());
    api.doGet("mbs", BASE + "/requests/" + seed("PKR-2026-900005") + "/requirements")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.signoffs[0].decision").value("SIGNED"));
  }

  @Test
  void aRequestIsCreatedAndSubmittedOverHttp() throws Exception {
    Map<String, Object> body =
        Map.of(
            "companyId", data.company().getId(),
            "type", "NEW",
            "scope", "GENERIC",
            "title", "HTTP package",
            "lineCode", "LIABILITY",
            "coverTypeCode", "CGL",
            "reason", "NEW_PROGRAMME",
            "terms",
                Map.of(
                    "coverages", List.of(Map.of("coverageCode", "CGL_BI", "included", true)),
                    "dates", Map.of("packageEndDate", "2027-12-31"),
                    "insurers", List.of(Map.of("insurerCode", "INS-MGIC"))));
    JsonNode created =
        api.read(
            api.doPost("ao", BASE + "/requests", body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT")));
    long id = created.get("id").asLong();
    api.doPut("ao", BASE + "/requests/" + id, body).andExpect(status().isOk());
    api.doPost("ao", BASE + "/requests/" + id + "/submit", Map.of("comment", "go"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FOR_MKT_APPROVAL"))
        .andExpect(jsonPath("$.milestones.submittedBy").value("ao"));
    api.doPost("ao", BASE + "/requests/" + id + "/approve", Map.of())
        .andExpect(status().isForbidden());
    api.doPost("mkttl", BASE + "/requests/" + id + "/approve", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FOR_TSU_REVIEW"));
    api.doPost("tsulead", BASE + "/requests/" + id + "/recommend", Map.of("text", "ok"))
        .andExpect(status().isOk());
    api.doPost("tsuhead", BASE + "/requests/" + id + "/tsu-approve", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("NEGOTIATION"));
    api.doPut(
            "tsu",
            BASE + "/requests/" + id + "/rounds/1",
            Map.of("insurers", List.of("INS-MGIC", "INS-LAC"), "notes", "please quote"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.insurers.length()").value(2));
    api.doPost("tsu", BASE + "/requests/" + id + "/rounds/1/quotation-slip/submit", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FOR_APPROVAL"));
    api.doPost("tsu", BASE + "/requests/" + id + "/rounds/1/quotation-slip/approve", Map.of())
        .andExpect(status().isForbidden());
    api.doPost("tsulead", BASE + "/requests/" + id + "/rounds/1/quotation-slip/approve", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.responses.length()").value(2));
    api.doPost("tsu", BASE + "/requests/" + id + "/comparatives/master", Map.of())
        .andExpect(status().isCreated());
    api.doPost(
            "tsu",
            BASE + "/requests/" + id + "/comparatives",
            Map.of("title", "For the client", "fields", List.of("OUTCOME")))
        .andExpect(status().isCreated());
  }

  @Test
  void permissionsAreEnforced() throws Exception {
    api.doPost(
            "mbs",
            BASE + "/requests",
            Map.of("title", "x", "lineCode", "MOTOR", "reason", "OTHERS", "companyId", 1))
        .andExpect(status().isForbidden());
    api.doGet("ao", BASE + "/expiry?companyId=" + company()).andExpect(status().isForbidden());
    api.doPost("ao", BASE + "/expiry/renewal-requests", Map.of()).andExpect(status().isForbidden());
    api.doPost("tsu", BASE + "/requests/1/signoff", Map.of()).andExpect(status().isForbidden());
    api.doPost("tsu", BASE + "/requests/1/setup", Map.of()).andExpect(status().isForbidden());
    api.doPost("ao", BASE + "/advisories/1/send", Map.of()).andExpect(status().isForbidden());
    api.doGet("tsu", BASE + "/requests/999999999").andExpect(status().isNotFound());
  }

  @Test
  void theProductMaintenanceReportsRunAndExportInEveryFormat() {
    Map<String, String> params = Map.of("companyId", company(), "within", "400");
    as.run(
        "tsuhead",
        () -> {
          for (String code : REPORTS) {
            assertThat(reports.run(code, params).code()).isEqualTo(code);
            for (ExportFormat format : ExportFormat.values()) {
              assertThat(reports.export(code, params, format).content()).isNotEmpty();
            }
          }
          assertThat(
                  reports
                      .run(
                          "PM-PKG-STATUS",
                          Map.of("companyId", company(), "request", "pkr-2026-900006"))
                      .rows())
              .isNotEmpty();
          return null;
        });
  }
}
