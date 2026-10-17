package com.iortatechnxt.brokerverse.api;

import static com.iortatechnxt.brokerverse.screening.ScreeningMatchingFixtures.person;
import static com.iortatechnxt.brokerverse.screening.ScreeningMatchingFixtures.word;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.screening.ScreeningMatchingFixtures;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * HTTP contract of matching and risk profiling (wave S1-B): matches (search, detail side by side,
 * false positive, open case), the client's matches and risk-profile history, "Update Risk Tag",
 * "Screen Now" and the run log, with the permission of each endpoint.
 */
@IntegrationTest
class ScreeningMatchingApiIT {

  private static final String VIEWER = "ucc";
  private static final String INVESTIGATOR = "investigator";
  private static final String OUTSIDER = "ao";
  private static final String BASE = "/api/v1/screening";

  @Autowired private Api api;
  @Autowired private ScreeningMatchingFixtures fx;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void matchesRunsAndTheRiskProfileAreServedOverHttp() throws Exception {
    String first = word();
    String last = word();
    fx.listed(first + " " + last, null);
    Client c = fx.prospect(fx.seedCompany(), person(first, last, null));
    Long company = fx.seedCompany();
    Long matchId =
        jdbc.queryForObject(
            "select max(id) from scr_match where client_id = ?", Long.class, c.getId());

    api.doGet(OUTSIDER, BASE + "/matches?companyId=" + company).andExpect(status().isForbidden());
    api.doGet(
            VIEWER,
            BASE
                + "/matches?companyId="
                + company
                + "&status=POTENTIAL&listType=INTERNAL&minScore=0.5&maxScore=1&uncased=false&q="
                + last.toLowerCase(java.util.Locale.ROOT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].clientCode").value(c.getCode()));
    api.doGet(VIEWER, BASE + "/matches/" + matchId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.match.id").value(matchId))
        .andExpect(jsonPath("$.client.reference").value(c.getCode()))
        .andExpect(jsonPath("$.entry.name").value(first + " " + last));
    api.doGet(VIEWER, BASE + "/clients/" + c.getId() + "/matches")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    api.doGet(VIEWER, BASE + "/clients/" + c.getId() + "/risk-profile")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].source").value("RULE"))
        .andExpect(jsonPath("$[0].categoryCode").value("HIGH_SANCTION"));
    Long runId =
        jdbc.queryForObject("select run_id from scr_match where id = ?", Long.class, matchId);
    api.doGet(VIEWER, BASE + "/runs?companyId=" + company + "&trigger=CLIENT_REGISTERED")
        .andExpect(status().isOk());
    api.doGet(VIEWER, BASE + "/runs/" + runId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.trigger").value("CLIENT_REGISTERED"));
    api.doGet(VIEWER, BASE + "/runs/" + runId + "/matches")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(matchId));

    api.doPost(VIEWER, BASE + "/clients/" + c.getId() + "/screen", Map.of())
        .andExpect(status().isForbidden());
    api.doPost(INVESTIGATOR, BASE + "/clients/" + c.getId() + "/screen", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.trigger").value("MANUAL"))
        .andExpect(jsonPath("$.matches").value(0));

    api.doPost(VIEWER, BASE + "/matches/" + matchId + "/false-positive", Map.of())
        .andExpect(status().isForbidden());
    api.doPost(
            INVESTIGATOR,
            BASE + "/matches/" + matchId + "/false-positive",
            Map.of("justification", ""))
        .andExpect(status().isUnprocessableEntity());
    api.doPost(VIEWER, BASE + "/matches/" + matchId + "/open-case", Map.of())
        .andExpect(status().isForbidden());

    api.doPost(
            VIEWER,
            BASE + "/clients/" + c.getId() + "/risk-profile",
            Map.of("riskRating", "HIGH", "justification", "x"))
        .andExpect(status().isForbidden());
    api.doPost(
            INVESTIGATOR,
            BASE + "/clients/" + c.getId() + "/risk-profile",
            Map.of(
                "riskRating", "HIGH",
                "justification", "No evidence",
                "evidenceAttachmentIds", List.of()))
        .andExpect(status().isUnprocessableEntity());
  }
}
