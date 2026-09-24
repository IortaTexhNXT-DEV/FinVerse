package com.iortatechnxt.brokerverse.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

/** New Business dashboard, report variants, targets and NB report exports over HTTP (W4). */
@IntegrationTest
class NbReportApiIT {

  @Autowired private Api api;
  @Autowired private TestData data;

  private String c() {
    return data.company().getId().toString();
  }

  @ParameterizedTest
  @CsvSource({
    "ao, /api/v1/nb/dashboard?companyId={c}",
    "mkttl, /api/v1/nb/dashboard?companyId={c}&asOf=2026-09-20",
    "proc, /api/v1/nb/dashboard?companyId={c}",
    "epol, /api/v1/nb/dashboard?companyId={c}",
    "mkttl, /api/v1/nb/targets?companyId={c}&from=2026-09-01&to=2026-09-30",
    "badmin, /api/v1/nb/targets?companyId={c}&from=2026-01-01&to=2026-12-31",
    "ao, /api/v1/nb/report-variants?reportCode=NB-ACC-STATUS",
    "approver, /api/v1/reports",
  })
  void readEndpointsRespondOk(String username, String url) throws Exception {
    api.doGet(username, url.replace("{c}", c())).andExpect(status().isOk());
  }

  @Test
  void theDashboardCarriesEverySection() throws Exception {
    api.doGet("mkttl", "/api/v1/nb/dashboard?companyId=" + c() + "&asOf=2026-09-20")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.asOf").value("2026-09-20"))
        .andExpect(jsonPath("$.funnel.length()").value(7))
        .andExpect(jsonPath("$.booked.count").isNumber())
        .andExpect(jsonPath("$.production[0].achievement").exists())
        .andExpect(jsonPath("$.accounts[0].label").isNotEmpty());
  }

  @Test
  void usersWithoutTheRightsAreRefused() throws Exception {
    api.doGet("accountant", "/api/v1/nb/dashboard?companyId=" + c())
        .andExpect(status().isForbidden());
    api.doGet("ao", "/api/v1/nb/targets?companyId=" + c() + "&from=2026-01-01&to=2026-01-31")
        .andExpect(status().isForbidden());
    api.doPut("mkttl", "/api/v1/nb/targets?companyId=" + c(), target("T-CBG1"))
        .andExpect(status().isForbidden());
  }

  @Test
  void aTargetIsSavedAndChanged() throws Exception {
    String unit = "U" + UUID.randomUUID().toString().substring(0, 8);
    api.doPut("badmin", "/api/v1/nb/targets?companyId=" + c(), target(unit))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.targetPremium").value(120000.0))
        .andExpect(jsonPath("$.currency").value("PHP"));
    Map<String, Object> changed = new java.util.HashMap<>(target(unit));
    changed.put("targetPremium", 150000);
    JsonNode saved =
        api.read(
            api.doPut("badmin", "/api/v1/nb/targets?companyId=" + c(), changed)
                .andExpect(status().isOk()));
    assertThat(saved.get("targetPremium").decimalValue()).isEqualByComparingTo("150000");
    api.doPut(
            "badmin",
            "/api/v1/nb/targets?companyId=" + c(),
            Map.of("unitLevel", "TEAM", "unitCode", unit))
        .andExpect(status().isBadRequest());
  }

  @Test
  void aVariantIsSavedListedAndDeleted() throws Exception {
    String name = "Stalled " + UUID.randomUUID();
    JsonNode saved =
        api.read(
            api.doPost(
                    "ao",
                    "/api/v1/nb/report-variants",
                    Map.of(
                        "reportCode",
                        "NB-ACC-STATUS",
                        "name",
                        name,
                        "parameters",
                        Map.of("exceptions", "STALLED"),
                        "shared",
                        false))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mine").value(true))
                .andExpect(jsonPath("$.parameters.exceptions").value("STALLED")));
    long id = saved.get("id").asLong();
    api.doGet("ao", "/api/v1/nb/report-variants?reportCode=NB-ACC-STATUS")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == " + id + ")].name").value(name));
    api.doDelete("ao2", "/api/v1/nb/report-variants/" + id)
        .andExpect(status().isUnprocessableEntity());
    api.doDelete("ao", "/api/v1/nb/report-variants/" + id).andExpect(status().isNoContent());
  }

  @Test
  void reportsExportAsOdsAndXmlWithTheirMetadata() throws Exception {
    Map<String, String> params = Map.of("companyId", c(), "exceptions", "ALL");
    byte[] ods =
        api.doPost("ao", "/api/v1/reports/NB-ACC-STATUS/export?format=ODS", params)
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/vnd.oasis.opendocument.spreadsheet"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertThat(new String(ods, 30, 8, StandardCharsets.US_ASCII)).isEqualTo("mimetype");
    String xml =
        api.doPost("ao", "/api/v1/reports/NB-ACC-STATUS/export?format=XML", params)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    assertThat(xml)
        .contains("<report code=\"NB-ACC-STATUS\"")
        .contains("<generatedBy>ao</generatedBy>")
        .contains("BDOI Demo Insurance Brokers, Inc.");
  }

  private static Map<String, Object> target(String unit) {
    return Map.of(
        "unitLevel",
        "TEAM",
        "unitCode",
        unit,
        "periodFrom",
        "2030-01-01",
        "periodTo",
        "2030-01-31",
        "targetCount",
        5,
        "targetPremium",
        120000,
        "targetCommission",
        24000);
  }
}
