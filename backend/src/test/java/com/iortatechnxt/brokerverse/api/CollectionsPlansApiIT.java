package com.iortatechnxt.brokerverse.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.collections.CollectionsPlanFixtures;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * HTTP contract of the Collections plans and escalations (wave C1-B): installment plans and the
 * installments due, promises, escalations with their actions, escalation rules (maker-checker and
 * preview), statements of account (generate, PDF, recipient, send, cancel) and the bulk actions,
 * with the permission of each endpoint.
 */
@IntegrationTest
class CollectionsPlansApiIT {

  private static final String HANDLER = "mktcoll";
  private static final String TEAM_LEAD = "mkttl";
  private static final String PLANS = "/api/v1/collections/plans";
  private static final String STATEMENTS = "/api/v1/collections/billing/statements";
  private static final String ESCALATIONS = "/api/v1/collections/escalations";
  private static final String RULES = "/api/v1/collections/escalation-rules";
  private static final String PROMISES = "/api/v1/collections/promises";

  @Autowired private Api api;
  @Autowired private CollectionsPlanFixtures fx;

  @Test
  void plansAndStatementsAreServedOverHttp() throws Exception {
    BookedInvoice year1 = fx.threeYearAccount();
    Long company = fx.company();
    api.doPost(
            "uw",
            PLANS + "/policy-years",
            Map.of("companyId", company, "arn", year1.getArn(), "frequency", "ANNUAL"))
        .andExpect(status().isForbidden());
    JsonNode plan =
        api.read(
            api.doPost(
                    HANDLER,
                    PLANS + "/policy-years",
                    Map.of("companyId", company, "arn", year1.getArn(), "frequency", "ANNUAL"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.installments.length()").value(3))
                .andExpect(jsonPath("$.source").value("POLICY_YEARS")));
    long planId = plan.get("id").asLong();
    api.doGet(TEAM_LEAD, PLANS + "/" + planId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.installments[0].invoiceNo").value(year1.getInvoiceNo()));
    api.doGet(TEAM_LEAD, PLANS + "?companyId=" + company + "&status=ACTIVE&q=" + year1.getArn())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(planId));
    api.doGet(TEAM_LEAD, PLANS + "/by-account?arn=" + year1.getArn())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    api.doGet(TEAM_LEAD, PLANS + "/installments/due?companyId=" + company + "&until=2030-12-31")
        .andExpect(status().isOk());
    api.doGet(TEAM_LEAD, PLANS + "/installments/due?companyId=" + company + "&overdueOnly=true")
        .andExpect(status().isOk());
    api.doPost(HANDLER, PLANS + "/" + planId + "/refresh", null).andExpect(status().isOk());
    api.doGet("uw", PLANS + "?companyId=" + company).andExpect(status().isForbidden());

    JsonNode soa =
        api.read(
            api.doPost(HANDLER, STATEMENTS, Map.of("planId", planId, "cycleSeq", 2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.lines[0].kind").value("ARREARS"))
                .andExpect(jsonPath("$.templateVersion").value("CLX_SOA v1")));
    long soaId = soa.get("id").asLong();
    api.doPost(HANDLER, STATEMENTS, Map.of("planId", planId, "cycleSeq", 2))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("CLX_SOA_EXISTS"));
    api.doGet(TEAM_LEAD, STATEMENTS + "/" + soaId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cycleSeq").value(2));
    api.doGet(TEAM_LEAD, STATEMENTS + "/by-plan/" + planId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    api.doGet(TEAM_LEAD, STATEMENTS + "?companyId=" + company + "&q=" + year1.getArn())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
    api.doGet(TEAM_LEAD, STATEMENTS + "/" + soaId + "/document")
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/pdf"));
    api.doGet(HANDLER, STATEMENTS + "/" + soaId + "/recipient").andExpect(status().isOk());
    api.doPost(
            HANDLER,
            STATEMENTS + "/" + soaId + "/send",
            Map.of("to", List.of("client@example.com"), "subject", "SOA", "body", "Please pay"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SENT"));
    api.doPost(
            HANDLER,
            STATEMENTS + "/generate-due",
            Map.of("companyId", company, "from", "2027-10-01", "to", "2027-10-01"))
        .andExpect(status().isOk());
    api.doPost(HANDLER, STATEMENTS + "/" + soaId + "/cancel", Map.of("reason", "Resend"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    api.doPost(HANDLER, PLANS + "/" + planId + "/cancel", Map.of("reason", "Replaced"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    OpsInvoice invoice = fx.motorInvoice();
    api.doPost(
            HANDLER,
            PLANS + "/generated",
            Map.of(
                "companyId",
                company,
                "invoiceNo",
                invoice.getInvoiceNo(),
                "frequency",
                "QUARTERLY",
                "firstDue",
                LocalDate.now().toString(),
                "count",
                4))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.installmentCount").value(4));
    OpsInvoice other = fx.motorInvoice();
    api.doPost(
            HANDLER,
            PLANS + "/manual",
            Map.of(
                "companyId",
                company,
                "invoiceNo",
                other.getInvoiceNo(),
                "frequency",
                "MONTHLY",
                "entries",
                List.of(
                    Map.of(
                        "dueDate", LocalDate.now().toString(),
                        "amount", other.premiumBalance()))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.source").value("MANUAL"));
  }

  @Test
  void promisesEscalationsRulesAndBulkActionsAreServedOverHttp() throws Exception {
    Long company = fx.company();
    OpsInvoice invoice = fx.motorInvoice();
    String no = invoice.getInvoiceNo();
    JsonNode promise =
        api.read(
            api.doPost(
                    HANDLER,
                    PROMISES,
                    Map.of(
                        "companyId",
                        company,
                        "invoiceNo",
                        no,
                        "promisedDate",
                        LocalDate.now().plusDays(3).toString(),
                        "amount",
                        "100.00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN")));
    api.doGet(TEAM_LEAD, PROMISES + "/by-invoice/" + no)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    api.doGet(TEAM_LEAD, PROMISES + "?companyId=" + company + "&status=OPEN&q=" + no)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
    api.doPost(
            HANDLER,
            PROMISES + "/" + promise.get("id").asLong() + "/cancel",
            Map.of("reason", "No"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    api.doPost(
            "uw",
            PROMISES,
            Map.of(
                "companyId", company,
                "invoiceNo", no,
                "promisedDate", LocalDate.now().plusDays(3).toString()))
        .andExpect(status().isForbidden());

    Map<String, Object> escalate = new HashMap<>();
    escalate.put("companyId", company);
    escalate.put("invoiceNos", List.of(no));
    escalate.put("targetLevel", "TL");
    escalate.put("reasonCode", "NO_COMMITMENT");
    api.doPost("proc", "/api/v1/collections/bulk/escalate", escalate)
        .andExpect(status().isForbidden());
    api.doPost(HANDLER, "/api/v1/collections/bulk/escalate", escalate)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].ok").value(true));
    JsonNode list =
        api.read(
            api.doGet(TEAM_LEAD, ESCALATIONS + "?companyId=" + company + "&stage=WITH_TL&q=" + no)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1)));
    long id = list.get("content").get(0).get("id").asLong();
    api.doGet(TEAM_LEAD, ESCALATIONS + "/" + id)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].invoiceNo").value(no))
        .andExpect(jsonPath("$.overdue").value(false));
    api.doGet(TEAM_LEAD, ESCALATIONS + "/by-invoice/" + no)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    api.doPost(
            TEAM_LEAD,
            ESCALATIONS + "/" + id + "/actions/escalate_further",
            Map.of("reasonCode", "AGING", "comment", "Needs the unit head"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("WITH_UH"));
    api.doPost("uw", ESCALATIONS + "/" + id + "/actions/resolve", Map.of("comment", "x"))
        .andExpect(status().isForbidden());
    api.doPost(
            TEAM_LEAD,
            "/api/v1/collections/bulk/promises",
            Map.of(
                "companyId", company,
                "invoiceNos", List.of(no),
                "promisedDate", LocalDate.now().plusDays(4).toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].ok").value(true));

    Map<String, Object> rule = new HashMap<>();
    rule.put("companyId", company);
    rule.put("code", "API-" + CollectionsPlanFixtures.token());
    rule.put("name", "API rule");
    rule.put("basis", "AMOUNT_OVER");
    rule.put("threshold", "1.00");
    rule.put("segment", "CBG");
    rule.put("targetLevel", "SECTION_HEAD");
    rule.put("reasonCode", "OTHERS");
    rule.put("slaHours", 48);
    rule.put("notifyTarget", true);
    rule.put("effectiveFrom", "2026-01-01");
    api.doPost(TEAM_LEAD, RULES, rule).andExpect(status().isForbidden());
    JsonNode created =
        api.read(
            api.doPost("badmin", RULES, rule)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recordStatus").value("PENDING_AUTHORIZATION")));
    long ruleId = created.get("id").asLong();
    api.doPost("badmin", RULES + "/" + ruleId + "/authorize", null)
        .andExpect(status().isForbidden());
    api.doPost("approver", RULES + "/" + ruleId + "/authorize", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recordStatus").value("ACTIVE"));
    api.doGet("badmin", RULES + "/" + ruleId + "/matches?asOf=" + LocalDate.now())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.invoiceNo == '" + no + "')]").exists());
    api.doGet(TEAM_LEAD, RULES + "?companyId=" + company)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.code == 'CLX-BROKEN-PROMISE')]").exists());
    Map<String, Object> change = new HashMap<>(rule);
    change.put("threshold", "2.00");
    api.doPut("badmin", RULES + "/" + ruleId, change)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recordStatus").value("PENDING_AUTHORIZATION"));
    api.doPost("badmin", RULES + "/" + ruleId + "/deactivate", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recordStatus").value("INACTIVE"));
    Map<String, Object> noCode = new HashMap<>(rule);
    noCode.remove("code");
    api.doPost("badmin", RULES, noCode)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("CLX_RULE_CODE"));
    assertThat(created.get("maker").asText()).isEqualTo("badmin");
  }
}
