package com.iortatechnxt.brokerverse.receivables;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItem;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Receivables REST API end to end: receipts, deposits, PDCs and bank reconciliation. */
@IntegrationTest
class ReceivablesApiIT {

  private static final String BASE = "/api/v1/receivables";
  private static final LocalDate JULY = LocalDate.of(2026, 7, 6);
  private static final String SLIP_JSON =
      "{\"companyId\":%d,\"branchId\":%d,\"bankAccountCode\":\"1112\","
          + "\"slipDate\":\"%s\",\"receiptIds\":[%d]}";

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private UserDetailsService users;
  @Autowired private ReceivablesFixtures fx;

  private ResultActions call(String username, MockHttpServletRequestBuilder request)
      throws Exception {
    return mvc.perform(request.with(user(users.loadUserByUsername(username))).with(csrf()));
  }

  private JsonNode body(ResultActions result) throws Exception {
    return json.readTree(result.andReturn().getResponse().getContentAsString());
  }

  private MockHttpServletRequestBuilder postJson(String url, String content) {
    return post(url).contentType(MediaType.APPLICATION_JSON).content(content);
  }

  private String receiptJson(
      String mode, String amount, String allocationMethod, String allocations) {
    return """
        {"companyId":%d,"branchId":%d,"receiptDate":"%s","payerType":"POLICYHOLDER",
         "partyCode":"C-000201","mode":"%s","instrumentNo":"API1","draweeBank":"BPI",
         "currency":"PHP","amount":%s,"bankAccountCode":"1112","allocationMethod":"%s",
         "allocations":%s}
        """
        .formatted(fx.company(), fx.branch(), JULY, mode, amount, allocationMethod, allocations);
  }

  private long createReceipt(String mode, String amount, String method, String allocations)
      throws Exception {
    return body(call(
                "accountant",
                postJson(BASE + "/receipts", receiptJson(mode, amount, method, allocations)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.summary.status").value("PENDING_APPROVAL")))
        .path("summary")
        .path("id")
        .asLong();
  }

  @Test
  void receiptLifecycleThroughTheApi() throws Exception {
    OpenItem note = fx.debitNote("C-000201", JULY.minusDays(3), "3000.00", "PHP");
    long id =
        createReceipt(
            "BANK_TRANSFER",
            "5000.00",
            "MANUAL",
            "[{\"debitItemId\":%d,\"amount\":3000.00}]".formatted(note.getId()));
    call(
            "accountant",
            get(BASE + "/receipts?companyId=" + fx.company() + "&status=PENDING_APPROVAL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
    call("checker", post(BASE + "/receipts/" + id + "/approve"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary.status").value("APPROVED"))
        .andExpect(jsonPath("$.summary.unappliedAmount").value(2000.0))
        .andExpect(jsonPath("$.allocations[0].matched").value(true));
    fx.debitNote("C-000201", JULY, "1500.00", "PHP");
    call(
            "accountant",
            postJson(
                BASE + "/receipts/" + id + "/apply",
                "{\"date\":\"%s\",\"method\":\"FIFO\"}".formatted(JULY.plusDays(1))))
        .andExpect(status().isOk());
    call("checker", postJson(BASE + "/receipts/" + id + "/cancel", reason(JULY.plusDays(2))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary.status").value("CANCELLED"));
    call("uw", get(BASE + "/receipts/" + id)).andExpect(status().isForbidden());

    long rejected = createReceipt("CASH", "10.00", "NONE", "[]");
    call("checker", postJson(BASE + "/receipts/" + rejected + "/reject", reason(JULY)))
        .andExpect(jsonPath("$.summary.status").value("REJECTED"));
    call("accountant", postJson(BASE + "/receipts", "{}")).andExpect(status().isBadRequest());
  }

  @Test
  void depositSlipsAndBouncedChequeThroughTheApi() throws Exception {
    long first = createReceipt("CHEQUE", "800.00", "NONE", "[]");
    long second = createReceipt("CHEQUE", "300.00", "NONE", "[]");
    call("checker", post(BASE + "/receipts/" + first + "/approve")).andExpect(status().isOk());
    call("checker", post(BASE + "/receipts/" + second + "/approve")).andExpect(status().isOk());
    call(
            "accountant",
            get(BASE + "/deposits/undeposited?companyId=" + fx.company() + "&bankAccountCode=1112"))
        .andExpect(status().isOk());
    long slip =
        body(call(
                    "accountant",
                    postJson(
                        BASE + "/deposits/slips",
                        SLIP_JSON.formatted(fx.company(), fx.branch(), JULY, first)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receipts[0].id").value(first)))
            .path("id")
            .asLong();
    call(
            "accountant",
            postJson(
                BASE + "/deposits/slips/" + slip + "/confirm", "{\"date\":\"%s\"}".formatted(JULY)))
        .andExpect(jsonPath("$.status").value("DEPOSITED"));
    call("accountant", get(BASE + "/deposits/slips?companyId=" + fx.company()))
        .andExpect(status().isOk());
    long other =
        body(call(
                "accountant",
                postJson(
                    BASE + "/deposits/slips",
                    SLIP_JSON.formatted(fx.company(), fx.branch(), JULY, second))))
            .path("id")
            .asLong();
    call("accountant", post(BASE + "/deposits/slips/" + other + "/cancel"))
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    call("checker", postJson(BASE + "/receipts/" + first + "/bounce", reason(JULY.plusDays(5))))
        .andExpect(jsonPath("$.summary.status").value("BOUNCED"));
  }

  @Test
  void pdcRegisterThroughTheApi() throws Exception {
    String pdc =
        """
        {"companyId":%d,"branchId":%d,"receivedDate":"%s","partyCode":"C-000204",
         "chequeNo":"%s","chequeDate":"%s","draweeBank":"BPI","currency":"PHP","amount":1200.00,
         "bankAccountCode":"1111"}
        """;
    long id =
        body(call(
                    "accountant",
                    postJson(
                        BASE + "/pdcs",
                        pdc.formatted(
                            fx.company(), fx.branch(), JULY, "API-P1", JULY.plusDays(10))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ON_HAND")))
            .path("id")
            .asLong();
    call(
            "accountant",
            post(BASE + "/pdcs/mark-due?companyId=" + fx.company() + "&asOf=" + JULY.plusDays(10)))
        .andExpect(status().isOk());
    long receipt =
        body(call(
                    "accountant",
                    postJson(
                        BASE + "/pdcs/" + id + "/deposit",
                        "{\"date\":\"%s\"}".formatted(JULY.plusDays(10))))
                .andExpect(jsonPath("$.summary.mode").value("PDC")))
            .path("summary")
            .path("id")
            .asLong();
    call("checker", post(BASE + "/receipts/" + receipt + "/approve")).andExpect(status().isOk());
    call(
            "accountant",
            postJson(
                BASE + "/pdcs/" + id + "/clear", "{\"date\":\"%s\"}".formatted(JULY.plusDays(12))))
        .andExpect(jsonPath("$.status").value("CLEARED"))
        .andExpect(jsonPath("$.history.length()").value(4));
    call("checker", postJson(BASE + "/pdcs/" + id + "/bounce", reason(JULY.plusDays(13))))
        .andExpect(status().isUnprocessableEntity());

    long other =
        body(call(
                "accountant",
                postJson(
                    BASE + "/pdcs",
                    pdc.formatted(fx.company(), fx.branch(), JULY, "API-P2", JULY.plusDays(20)))))
            .path("id")
            .asLong();
    call(
            "accountant",
            postJson(
                BASE + "/pdcs/" + other + "/replace",
                ("{\"date\":\"%s\",\"chequeNo\":\"API-P3\",\"chequeDate\":\"%s\","
                        + "\"draweeBank\":\"BPI\",\"amount\":1200.00}")
                    .formatted(JULY.plusDays(1), JULY.plusDays(25))))
        .andExpect(jsonPath("$.chequeNo").value("API-P3"));
    long third =
        body(call(
                "accountant",
                postJson(
                    BASE + "/pdcs",
                    pdc.formatted(fx.company(), fx.branch(), JULY, "API-P4", JULY.plusDays(20)))))
            .path("id")
            .asLong();
    call("accountant", postJson(BASE + "/pdcs/" + third + "/return", reason(JULY.plusDays(2))))
        .andExpect(jsonPath("$.status").value("RETURNED"));
    call("accountant", get(BASE + "/pdcs?companyId=" + fx.company() + "&status=RETURNED"))
        .andExpect(status().isOk());
  }

  @Test
  void bankReconciliationThroughTheApi() throws Exception {
    long receipt =
        body(call(
                "accountant",
                postJson(
                    BASE + "/receipts",
                    receiptJson("BANK_TRANSFER", "4321.00", "NONE", "[]")
                        .replace("\"1112\"", "\"1120\""))))
            .path("summary")
            .path("id")
            .asLong();
    call("checker", post(BASE + "/receipts/" + receipt + "/approve")).andExpect(status().isOk());
    String csv =
        "date,description,reference,debit,credit,balance\\n"
            + "2026-07-07,Transfer,,,4321.00,4321.00\\n2026-07-08,Charge,,25.00,,4296.00\\n";
    long statement =
        body(call(
                    "fmanager",
                    postJson(
                        BASE + "/bank-rec/statements",
                        "{\"companyId\":%d,\"bankAccountCode\":\"1120\",\"fileName\":\"july.csv\",\"content\":\"%s\"}"
                            .formatted(fx.company(), csv)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lineCount").value(2)))
            .path("id")
            .asLong();
    call("fmanager", get(BASE + "/bank-rec/statements?companyId=" + fx.company()))
        .andExpect(status().isOk());
    JsonNode lines =
        body(
            call("fmanager", get(BASE + "/bank-rec/statements/" + statement + "/lines"))
                .andExpect(status().isOk()));
    String query = "?companyId=" + fx.company() + "&bankAccountCode=1120&asOf=2026-07-31";
    JsonNode bench =
        body(
            call("fmanager", get(BASE + "/bank-rec/workbench" + query)).andExpect(status().isOk()));
    long match =
        body(call(
                    "fmanager",
                    postJson(
                        BASE + "/bank-rec/matches",
                        ("{\"companyId\":%d,\"bankAccountCode\":\"1120\","
                                + "\"ledgerEntryIds\":[%d],\"statementLineIds\":[%d]}")
                            .formatted(
                                fx.company(),
                                bench.path("bookEntries").get(0).path("id").asLong(),
                                lines.get(0).path("id").asLong())))
                .andExpect(status().isCreated()))
            .path("id")
            .asLong();
    call("fmanager", post(BASE + "/bank-rec/matches/" + match + "/unmatch"))
        .andExpect(status().isOk());
    call(
            "fmanager",
            postJson(
                BASE + "/bank-rec/auto-match",
                "{\"companyId\":%d,\"bankAccountCode\":\"1120\",\"asOf\":\"2026-07-31\",\"dateWindowDays\":3}"
                    .formatted(fx.company())))
        .andExpect(jsonPath("$.length()").value(1));
    call(
            "fmanager",
            get(BASE + "/bank-rec/matches?companyId=" + fx.company() + "&bankAccountCode=1120"))
        .andExpect(status().isOk());
    call("fmanager", get(BASE + "/bank-rec/brs" + query))
        .andExpect(jsonPath("$.figures.difference").value(0.0))
        .andExpect(jsonPath("$.bankDebits.length()").value(1));
    long rec =
        body(call(
                "fmanager",
                postJson(
                    BASE + "/bank-rec/reconciliations",
                    "{\"companyId\":%d,\"bankAccountCode\":\"1120\",\"asOf\":\"2026-07-31\"}"
                        .formatted(fx.company()))))
            .path("id")
            .asLong();
    call("fmanager", post(BASE + "/bank-rec/reconciliations/" + rec + "/finalize"))
        .andExpect(jsonPath("$.status").value("FINALIZED"));
    call("fmanager", get(BASE + "/bank-rec/reconciliations?companyId=" + fx.company()))
        .andExpect(status().isOk());
    call("uw", get(BASE + "/bank-rec/brs" + query)).andExpect(status().isForbidden());
  }

  @Test
  void lookupsThroughTheApi() throws Exception {
    fx.debitNote("C-000201", JULY, "250.00", "PHP");
    call("accountant", get(BASE + "/bank-accounts?companyId=" + fx.company()))
        .andExpect(jsonPath("$[?(@.code=='1111')]").exists());
    call("accountant", get(BASE + "/open-items?companyId=" + fx.company() + "&partyCode=C-000201"))
        .andExpect(status().isOk());
    call("accountant", get(BASE + "/ageing-slots?slots=30,60"))
        .andExpect(jsonPath("$[2]").value("Over 60"));
  }

  private static String reason(LocalDate date) {
    return "{\"date\":\"%s\",\"reason\":\"API test\"}".formatted(date);
  }
}
