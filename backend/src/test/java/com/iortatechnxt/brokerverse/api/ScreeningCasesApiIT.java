package com.iortatechnxt.brokerverse.api;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * HTTP contract of screening cases, STRs and compliance reports (wave S1-C): the case list with its
 * tabs and filters, the tiles, the case page and its tabs, the client's cases, the high-risk list,
 * the STR register and extractions, and the permission of each endpoint (seed cases of V1952).
 */
@IntegrationTest
class ScreeningCasesApiIT {

  private static final String BASE = "/api/v1/screening";
  private static final String UCC = "ucc";
  private static final String COMPLIANCE = "compoff";
  private static final String OUTSIDER = "ao";

  @Autowired private Api api;
  @Autowired private TestData data;
  @Autowired private JdbcTemplate jdbc;

  private Long seedCase(String caseNo) {
    return jdbc.queryForObject("select id from scr_case where case_no = ?", Long.class, caseNo);
  }

  @Test
  void casesAndTheirTabsAreServedOverHttp() throws Exception {
    Long company = data.company().getId();
    Long investigation = seedCase("SCR-2026-000002");
    api.doGet(OUTSIDER, BASE + "/cases?companyId=" + company).andExpect(status().isForbidden());
    api.doGet(UCC, BASE + "/cases?companyId=" + company + "&tab=ALL&q=SCR-2026-000002")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].stage").value("INVESTIGATION"));
    api.doGet(
            UCC,
            BASE
                + "/cases?companyId="
                + company
                + "&tab=COMMITTEE&stage=AML_COMMITTEE&marketingUnit=CBG-NCR&createdFrom="
                + LocalDate.now().minusYears(1)
                + "&createdTo="
                + LocalDate.now().plusDays(1))
        .andExpect(status().isOk());
    api.doGet(UCC, BASE + "/cases?companyId=" + company + "&q=ab")
        .andExpect(status().isUnprocessableEntity());
    api.doGet(UCC, BASE + "/cases/tiles?companyId=" + company)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openByStage.INVESTIGATION").isNumber());
    api.doGet(UCC, BASE + "/cases/" + investigation)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.row.caseNo").value("SCR-2026-000002"))
        .andExpect(jsonPath("$.actions").isArray());
    api.doGet(UCC, BASE + "/cases/" + investigation + "/timeline")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].event").value("CREATED"));
    api.doGet(UCC, BASE + "/cases/" + investigation + "/matches").andExpect(status().isOk());
    api.doGet(UCC, BASE + "/cases/" + investigation + "/documents").andExpect(status().isOk());
    api.doGet(UCC, BASE + "/cases/" + investigation + "/review")
        .andExpect(status().is2xxSuccessful());
    api.doGet(UCC, BASE + "/cases/" + seedCase("SCR-2026-000006") + "/votes")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].member").value("amlcom1"));
    Long clientId =
        jdbc.queryForObject(
            "select client_id from scr_case where id = ?", Long.class, investigation);
    api.doGet(UCC, BASE + "/clients/" + clientId + "/cases")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].caseNo").value("SCR-2026-000002"));
    api.doGet(UCC, BASE + "/high-risk-clients?companyId=" + company).andExpect(status().isOk());
    api.doGet(UCC, BASE + "/cases/" + investigation + "/eligible-assignees")
        .andExpect(status().isOk());
    api.doPost(OUTSIDER, BASE + "/cases/" + investigation + "/reassign", Map.of())
        .andExpect(status().isForbidden());
    api.doPost(
            UCC,
            BASE + "/cases/" + investigation + "/reassign",
            Map.of("assignee", "investigator2"))
        .andExpect(status().isUnprocessableEntity());
    api.doPost(UCC, BASE + "/cases/" + investigation + "/submit", Map.of())
        .andExpect(status().isForbidden());
    api.doPost(
            "amlcom1",
            BASE + "/cases/" + seedCase("SCR-2026-000006") + "/votes",
            Map.of("remarks", "x"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void strsAndExtractionsAreServedOverHttp() throws Exception {
    Long company = data.company().getId();
    Long approved = seedCase("SCR-2026-000008");
    api.doGet(UCC, BASE + "/str?companyId=" + company).andExpect(status().isForbidden());
    api.doGet(COMPLIANCE, BASE + "/str?companyId=" + company + "&status=EXTRACTED")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.strNo == 'STR-2026-000002')]").exists());
    api.doGet(COMPLIANCE, BASE + "/cases/" + approved + "/str")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.str.status").value("APPROVED"))
        .andExpect(jsonPath("$.transactions.length()").value(1));
    api.doGet(COMPLIANCE, BASE + "/cases/" + seedCase("SCR-2026-000007") + "/str")
        .andExpect(status().isNoContent());
    Long strId =
        jdbc.queryForObject("select id from scr_str where str_no = 'STR-2026-000001'", Long.class);
    api.doGet(COMPLIANCE, BASE + "/str/" + strId + "/document?format=DOCX")
        .andExpect(status().isOk());
    api.doGet(COMPLIANCE, BASE + "/str/extractions?companyId=" + company)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
    api.doPost(
            COMPLIANCE,
            BASE + "/str/extractions/preview",
            Map.of(
                "companyId", company,
                "from", LocalDate.now().minusYears(1).toString(),
                "to", LocalDate.now().plusDays(1).toString()))
        .andExpect(status().isOk());
    api.doPost(
            UCC,
            BASE + "/str/extractions",
            Map.of("companyId", company, "from", "2026-01-01", "to", "2026-01-31"))
        .andExpect(status().isForbidden());
    api.doPost(
            COMPLIANCE,
            BASE + "/str/extractions",
            Map.of("companyId", company, "from", "2020-01-01", "to", "2020-01-31"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.detail").value("No committee-approved STR to extract"));
    api.doPost(COMPLIANCE, BASE + "/str/" + strId + "/filing", Map.of("reference", ""))
        .andExpect(status().isUnprocessableEntity());
    api.doPut(
            COMPLIANCE,
            BASE + "/str/" + strId,
            Map.of("values", Map.of(), "reasonCodes", List.of(), "transactions", List.of()))
        .andExpect(status().isUnprocessableEntity());
  }
}
