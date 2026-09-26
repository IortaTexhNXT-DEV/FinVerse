package com.iortatechnxt.brokerverse.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.screening.ScreeningSetupFixtures;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP contract of Compliance Setup (wave S1-A): configuration versions (list, detail, changes,
 * draft, save, submit, approve, reject), watchlist entries and changes, sources, the list file
 * template, upload and the run log, with the permission of each endpoint.
 */
@IntegrationTest
class ScreeningSetupApiIT {

  private static final String MAKER = "compoff";
  private static final String CHECKER = "compchk";
  private static final String OUTSIDER = "ao";
  private static final String CONFIG = "/api/v1/screening/config";
  private static final String LISTS = "/api/v1/screening/watchlist";

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private ScreeningSetupFixtures fx;
  @Autowired private Clock clock;

  @Test
  void configurationVersionsAreServedOverHttp() throws Exception {
    Long company = fx.company();
    api.doGet(OUTSIDER, CONFIG + "/versions?companyId=" + company + "&type=SLA_MATRIX")
        .andExpect(status().isForbidden());
    JsonNode draft =
        api.read(
            api.doPost(
                    MAKER, CONFIG + "/drafts", Map.of("companyId", company, "type", "SLA_MATRIX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version.status").value("DRAFT")));
    long id = draft.get("version").get("id").asLong();
    api.doPost(CHECKER, CONFIG + "/drafts", Map.of("companyId", company, "type", "SLA_MATRIX"))
        .andExpect(status().isForbidden());
    Map<String, Object> row =
        Map.of(
            "stage", "INVESTIGATION",
            "slaHours", 48,
            "reminderLeadHours", 12,
            "escalateToRole", "COMPLIANCE_OFFICER",
            "calendar", "CALENDAR");
    api.doPut(
            MAKER,
            CONFIG + "/versions/" + id,
            Map.of(
                "effectiveFrom", LocalDate.now(clock).toString(),
                "changeNote", "API test",
                "content", Map.of("slaRules", List.of(row))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.slaRules.length()").value(1))
        .andExpect(jsonPath("$.changes.length()").value(4));
    api.doPut(
            MAKER,
            CONFIG + "/versions/" + id,
            Map.of(
                "effectiveFrom", LocalDate.now(clock).toString(),
                "content", Map.of("slaRules", List.of(Map.of("stage", "INVESTIGATION")))))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("SCR_SLA_HOURS"));
    api.doPost(MAKER, CONFIG + "/versions/" + id + "/submit", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version.status").value("PENDING"));
    api.doGet(CHECKER, CONFIG + "/versions?companyId=" + company + "&type=SLA_MATRIX")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].label").value("SLA_MATRIX v1"));
    api.doGet(CHECKER, CONFIG + "/versions/" + id + "/changes")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].after").value("48"));
    api.doGet(CHECKER, CONFIG + "/versions/" + id).andExpect(status().isOk());
    api.doPost(MAKER, CONFIG + "/versions/" + id + "/approve", null)
        .andExpect(status().isForbidden());
    api.doPost(CHECKER, CONFIG + "/versions/" + id + "/reject", Map.of("remarks", ""))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("SCR_REJECT_REASON_REQUIRED"));
    api.doPost(CHECKER, CONFIG + "/versions/" + id + "/approve", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    JsonNode second =
        api.read(
            api.doPost(
                    MAKER, CONFIG + "/drafts", Map.of("companyId", company, "type", "SLA_MATRIX"))
                .andExpect(status().isOk()));
    long secondId = second.get("version").get("id").asLong();
    api.doPost(MAKER, CONFIG + "/versions/" + secondId + "/withdraw", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decisionReason").value("withdrawn by maker"));
  }

  @Test
  void watchlistsSourcesAndRunsAreServedOverHttp() throws Exception {
    String u = fx.unique();
    api.doGet(OUTSIDER, LISTS + "/sources").andExpect(status().isForbidden());
    api.doGet(MAKER, LISTS + "/sources")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.code == 'AML_ADVISORY')].fullFile").value(true));
    JsonNode change =
        api.read(
            api.doPost(
                    MAKER,
                    LISTS + "/entries",
                    Map.of(
                        "listType", "INTERNAL",
                        "entityType", "INDIVIDUAL",
                        "primaryName", "Api Invented Person " + u,
                        "aliases", List.of(Map.of("name", "AIP " + u)),
                        "remarks", "API test"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.after.aliases[0].type").value("AKA")));
    long changeId = change.get("id").asLong();
    long entryId = change.get("entryId").asLong();
    api.doPost(MAKER, LISTS + "/entries", Map.of("listType", "INTERNAL", "remarks", "x"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("SCR_ENTRY_NAME_REQUIRED"));
    api.doGet(CHECKER, LISTS + "/changes?status=PENDING&size=200").andExpect(status().isOk());
    api.doGet(CHECKER, LISTS + "/changes/" + changeId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.changeType").value("ADD"));
    api.doPost(MAKER, LISTS + "/changes/" + changeId + "/approve", Map.of())
        .andExpect(status().isForbidden());
    api.doPost(CHECKER, LISTS + "/changes/" + changeId + "/approve", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));
    api.doGet(CHECKER, LISTS + "/entries?search=" + u)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    api.doGet(CHECKER, LISTS + "/entries/" + entryId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.aliases[0].name").value("AIP " + u))
        .andExpect(jsonPath("$.history.length()").value(1));
    api.doPost(MAKER, LISTS + "/entries/" + entryId + "/deactivate", Map.of("remarks", "Delisted"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.changeType").value("DEACTIVATE"));

    api.doGet(MAKER, LISTS + "/template")
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    org.hamcrest.Matchers.containsString("watchlist_template.xlsx")));
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "list.csv",
            "text/csv",
            ScreeningSetupFixtures.csv(u + "-9,ENTITY,Api Invented Trading " + u + ",,,,,,,,,"));
    JsonNode run =
        api.read(
            mvc.perform(
                    multipart(LISTS + "/sources/NLDS_PEP/upload")
                        .file(file)
                        .with(user(users.loadUserByUsername(MAKER)))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trigger").value("MANUAL_UPLOAD"))
                .andExpect(jsonPath("$.added").value(1)));
    long runId = run.get("id").asLong();
    api.doGet(CHECKER, LISTS + "/runs?source=NLDS_PEP")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].sourceCode").value("NLDS_PEP"));
    api.doGet(CHECKER, LISTS + "/runs/" + runId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pendingChanges").value(1));
    api.doPost(CHECKER, LISTS + "/runs/" + runId + "/approve", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.approved").value(1));
    api.doPut(
            MAKER,
            LISTS + "/sources/INTERNAL",
            Map.of("name", "Internal watchlist", "fullFile", false, "active", true))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transport").value("MANUAL"));
  }
}
