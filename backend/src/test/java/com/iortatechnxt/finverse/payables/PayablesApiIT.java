package com.iortatechnxt.finverse.payables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Drives the payables API end to end over HTTP (security, validation, serialization). */
@IntegrationTest
class PayablesApiIT {

  private static final String BASE = "/api/v1/payables";

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private UserDetailsService users;
  @Autowired private TestData data;
  @Autowired private PayablesFixtures fx;

  private JsonNode call(
      String username, MockHttpServletRequestBuilder request, Object body, ResultMatcher expected)
      throws Exception {
    MockHttpServletRequestBuilder r =
        request.with(user(users.loadUserByUsername(username))).with(csrf());
    if (body != null) {
      r = r.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }
    String text = mvc.perform(r).andExpect(expected).andReturn().getResponse().getContentAsString();
    return text.isEmpty() || !text.startsWith("{") && !text.startsWith("[")
        ? null
        : json.readTree(text);
  }

  private JsonNode ok(String username, MockHttpServletRequestBuilder request, Object body)
      throws Exception {
    return call(username, request, body, status().is2xxSuccessful());
  }

  private Long company() {
    return data.company().getId();
  }

  private Long ho() {
    return data.branch("HO").getId();
  }

  @Test
  void bankAccountsAndChequeBooks() throws Exception {
    Map<String, Object> body =
        Map.of(
            "companyId",
            company(),
            "code",
            PayablesFixtures.unique("TB").substring(0, 11),
            "name",
            "Test placement account",
            "bankName",
            "Test Bank",
            "accountNo",
            "999-1",
            "currency",
            "PHP",
            "glAccountCode",
            "1120",
            "notificationFormat",
            "CSV");
    JsonNode created = ok("fmanager", post(BASE + "/bank-accounts"), body);
    long id = created.get("id").asLong();
    assertThat(created.get("recordStatus").asText()).isEqualTo("PENDING_AUTHORIZATION");
    ok("checker", post(BASE + "/bank-accounts/" + id + "/authorize"), null);
    JsonNode book =
        ok(
            "fmanager",
            post(BASE + "/bank-accounts/" + id + "/cheque-books"),
            Map.of("firstNo", 1, "lastNo", 50, "receivedOn", "2026-09-01"));
    call(
        "fmanager",
        post(BASE + "/bank-accounts/" + id + "/cheque-books"),
        Map.of("firstNo", 40, "lastNo", 60, "receivedOn", "2026-09-01"),
        status().isUnprocessableEntity());
    assertThat(ok("fmanager", get(BASE + "/bank-accounts/" + id + "/cheque-books"), null))
        .hasSize(1);
    ok(
        "fmanager",
        post(BASE + "/bank-accounts/cheque-books/" + book.get("id").asLong() + "/cancel"),
        null);
    ok("fmanager", put(BASE + "/bank-accounts/" + id), body);
    assertThat(
            ok("fmanager", get(BASE + "/bank-accounts/" + id), null).get("recordStatus").asText())
        .isEqualTo("PENDING_AUTHORIZATION");
    assertThat(
            ok(
                "fmanager",
                get(BASE + "/bank-accounts?companyId=" + company() + "&currency=USD"),
                null))
        .allSatisfy(a -> assertThat(a.get("currency").asText()).isEqualTo("USD"));
    call("uw", post(BASE + "/bank-accounts"), body, status().isForbidden());
  }

  @Test
  void invoiceAndPaymentFlowOverHttp() throws Exception {
    Map<String, Object> invoice =
        Map.of(
            "companyId",
            company(),
            "branchId",
            ho(),
            "partyCode",
            "S-0001",
            "supplierInvoiceNo",
            PayablesFixtures.unique("HTTP"),
            "invoiceDate",
            "2026-09-04",
            "vatApplicable",
            true,
            "lines",
            List.of(
                Map.of(
                    "expenseAccountCode",
                    "5608",
                    "costCenter",
                    "FIN",
                    "description",
                    "Paper",
                    "netAmount",
                    "1000.00")));
    long id = ok("accountant", post(BASE + "/invoices"), invoice).get("id").asLong();
    ok("accountant", post(BASE + "/invoices/" + id + "/submit"), null);
    ok("checker", post(BASE + "/invoices/" + id + "/reject"), Map.of("reason", "Check VAT"));
    ok("accountant", put(BASE + "/invoices/" + id), invoice);
    ok("accountant", post(BASE + "/invoices/" + id + "/submit"), null);
    JsonNode approved = ok("checker", post(BASE + "/invoices/" + id + "/approve"), null);
    assertThat(approved.get("status").asText()).isEqualTo("APPROVED");
    assertThat(ok("auditor", get(BASE + "/invoices/" + id), null).get("lines")).hasSize(1);

    JsonNode items =
        ok(
            "accountant",
            get(BASE + "/vouchers/payable-items?companyId=" + company() + "&partyCode=S-0001"),
            null);
    long itemId = -1;
    for (JsonNode i : items) {
      if (i.get("documentNo").asText().equals(approved.get("documentNo").asText())) {
        itemId = i.get("openItemId").asLong();
      }
    }
    Map<String, Object> payment =
        Map.of(
            "companyId",
            company(),
            "branchId",
            ho(),
            "partyCode",
            "S-0001",
            "mode",
            "CHEQUE",
            "bankAccountId",
            fx.bankId("BDO-CA"),
            "voucherDate",
            "2026-09-05",
            "items",
            List.of(Map.of("openItemId", itemId, "amount", "500.00")));
    long pv = ok("accountant", post(BASE + "/vouchers"), payment).get("id").asLong();
    ok("accountant", put(BASE + "/vouchers/" + pv), payment);
    ok("accountant", post(BASE + "/vouchers/" + pv + "/submit"), null);
    ok("checker", post(BASE + "/vouchers/" + pv + "/reject"), Map.of("reason", "Later"));
    ok("accountant", post(BASE + "/vouchers/" + pv + "/submit"), null);
    JsonNode paid = ok("checker", post(BASE + "/vouchers/" + pv + "/approve"), null);
    assertThat(paid.get("chequeNo").asText()).isNotBlank();
    ok("checker", post(BASE + "/vouchers/" + pv + "/void"), Map.of("date", "2026-09-06"));
    ok("accountant", get(BASE + "/vouchers/" + pv), null);

    long pv2 = ok("accountant", post(BASE + "/vouchers"), payment).get("id").asLong();
    ok("accountant", post(BASE + "/vouchers/" + pv2 + "/cancel"), Map.of("reason", "Duplicate"));
    long pv3 = ok("accountant", post(BASE + "/vouchers"), payment).get("id").asLong();
    ok("accountant", post(BASE + "/vouchers/" + pv3 + "/submit"), null);
    ok("checker", post(BASE + "/vouchers/" + pv3 + "/approve"), null);
    ok("accountant", post(BASE + "/vouchers/" + pv3 + "/presented"), Map.of("date", "2026-09-07"));

    long inv2 =
        ok(
                "accountant",
                post(BASE + "/invoices"),
                Map.of(
                    "companyId",
                    company(),
                    "branchId",
                    ho(),
                    "partyCode",
                    "S-0001",
                    "supplierInvoiceNo",
                    PayablesFixtures.unique("HTTP"),
                    "invoiceDate",
                    "2026-09-04",
                    "vatApplicable",
                    false,
                    "lines",
                    List.of(
                        Map.of(
                            "expenseAccountCode",
                            "5608",
                            "costCenter",
                            "FIN",
                            "description",
                            "Pens",
                            "netAmount",
                            "10.00"))))
            .get("id")
            .asLong();
    ok(
        "accountant",
        post(BASE + "/invoices/" + inv2 + "/cancel"),
        Map.of("reason", "Wrong supplier"));
    call(
        "accountant",
        post(BASE + "/invoices"),
        Map.of("companyId", company()),
        status().isBadRequest());
  }

  @Test
  void pdcRegisterOverHttp() throws Exception {
    var inv = fx.approvedInvoice("S-0003", "3000.00", PayablesFixtures.DATE);
    Map<String, Object> payment =
        Map.of(
            "companyId",
            company(),
            "branchId",
            ho(),
            "partyCode",
            "S-0003",
            "mode",
            "PDC",
            "bankAccountId",
            fx.bankId("BDO-CA"),
            "voucherDate",
            "2026-09-05",
            "chequeDate",
            "2026-09-19",
            "department",
            "FND",
            "items",
            List.of(Map.of("openItemId", inv.getOpenItemId())));
    long pv = ok("accountant", post(BASE + "/vouchers"), payment).get("id").asLong();
    ok("accountant", post(BASE + "/vouchers/" + pv + "/submit"), null);
    ok("checker", post(BASE + "/vouchers/" + pv + "/approve"), null);
    long pdc = -1;
    for (JsonNode p :
        ok(
            "auditor",
            get(BASE + "/pdc-issued?companyId=" + company() + "&statuses=ISSUED&statuses=DUE"),
            null)) {
      if (p.get("voucherId").asLong() == pv) {
        pdc = p.get("id").asLong();
      }
    }
    assertThat(pdc).isPositive();
    ok("auditor", get(BASE + "/pdc-issued/" + pdc), null);
    JsonNode replacement =
        ok(
            "checker",
            post(BASE + "/pdc-issued/" + pdc + "/replace"),
            Map.of("chequeDate", "2026-09-20", "date", "2026-09-06", "reason", "Torn"));
    long newId = replacement.get("id").asLong();
    ok("accountant", post(BASE + "/pdc-issued/refresh-due?asOf=2026-09-20"), null);
    ok("checker", post(BASE + "/pdc-issued/" + newId + "/present"), Map.of("date", "2026-09-21"));
    ok("accountant", post(BASE + "/pdc-issued/" + newId + "/clear"), Map.of("date", "2026-09-22"));
    assertThat(ok("auditor", get(BASE + "/pdc-issued/" + newId + "/history"), null))
        .hasSizeGreaterThanOrEqualTo(3);
    call(
        "checker",
        post(BASE + "/pdc-issued/" + newId + "/cancel"),
        Map.of("date", "2026-09-22"),
        status().isUnprocessableEntity());

    var inv2 = fx.approvedInvoice("S-0003", "100.00", PayablesFixtures.DATE);
    Map<String, Object> second = new java.util.HashMap<>(payment);
    second.put("items", List.of(Map.of("openItemId", inv2.getOpenItemId())));
    long pv2 = ok("accountant", post(BASE + "/vouchers"), second).get("id").asLong();
    ok("accountant", post(BASE + "/vouchers/" + pv2 + "/submit"), null);
    ok("checker", post(BASE + "/vouchers/" + pv2 + "/approve"), null);
    for (JsonNode p : ok("auditor", get(BASE + "/pdc-issued?companyId=" + company()), null)) {
      if (p.get("voucherId").asLong() == pv2) {
        ok(
            "checker",
            post(BASE + "/pdc-issued/" + p.get("id").asLong() + "/cancel"),
            Map.of("date", "2026-09-10", "reason", "Stop"));
      }
    }

    long bank = fx.bankId("BDO-CA");
    ok(
        "accountant",
        get(
            BASE
                + "/payment-notifications?bankAccountId="
                + bank
                + "&from=2026-09-01&to=2026-09-30&includeCheques=true"),
        null);
    mvc.perform(
            post(BASE
                    + "/payment-notifications/file?bankAccountId="
                    + bank
                    + "&from=2026-09-01&to=2026-09-30")
                .with(user(users.loadUserByUsername("accountant")))
                .with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  void pettyCashOverHttp() throws Exception {
    Map<String, Object> fund =
        Map.of(
            "companyId",
            company(),
            "branchId",
            data.branch("DVO").getId(),
            "code",
            PayablesFixtures.unique("PC").substring(0, 11),
            "name",
            "HTTP box",
            "custodian",
            "Clerk",
            "glAccountCode",
            "1102",
            "replenishBankAccountId",
            fx.bankId("BDO-CA"),
            "imprestAmount",
            "2000.00");
    long id = ok("fmanager", post(BASE + "/petty-cash/funds"), fund).get("id").asLong();
    ok("fmanager", put(BASE + "/petty-cash/funds/" + id), fund);
    ok("checker", post(BASE + "/petty-cash/funds/" + id + "/authorize"), null);
    ok(
        "checker",
        post(BASE + "/petty-cash/funds/" + id + "/establish"),
        Map.of("date", "2026-09-01"));
    Map<String, Object> voucher =
        Map.of(
            "date",
            "2026-09-02",
            "payee",
            "Driver",
            "expenseAccountCode",
            "5606",
            "costCenter",
            "UW",
            "description",
            "Fuel",
            "amount",
            "250.00");
    long v1 =
        ok("accountant", post(BASE + "/petty-cash/funds/" + id + "/disbursements"), voucher)
            .get("id")
            .asLong();
    long v2 =
        ok("accountant", post(BASE + "/petty-cash/funds/" + id + "/disbursements"), voucher)
            .get("id")
            .asLong();
    ok("checker", post(BASE + "/petty-cash/disbursements/" + v1 + "/approve"), null);
    ok(
        "checker",
        post(BASE + "/petty-cash/disbursements/" + v2 + "/reject"),
        Map.of("reason", "No receipt"));
    long claim =
        ok(
                "accountant",
                post(BASE + "/petty-cash/funds/" + id + "/reimbursements"),
                Map.of("date", "2026-09-03"))
            .get("id")
            .asLong();
    assertThat(
            ok(
                "auditor",
                get(BASE + "/petty-cash/reimbursements/" + claim + "/disbursements"),
                null))
        .hasSize(1);
    ok(
        "checker",
        post(BASE + "/petty-cash/reimbursements/" + claim + "/reject"),
        Map.of("reason", "Redo"));
    long claim2 =
        ok(
                "accountant",
                post(BASE + "/petty-cash/funds/" + id + "/reimbursements"),
                Map.of("date", "2026-09-04", "disbursementIds", List.of(v1)))
            .get("id")
            .asLong();
    ok("checker", post(BASE + "/petty-cash/reimbursements/" + claim2 + "/approve"), null);
    assertThat(ok("auditor", get(BASE + "/petty-cash/funds/" + id + "/disbursements"), null))
        .hasSize(2);
    assertThat(ok("auditor", get(BASE + "/petty-cash/funds/" + id + "/reimbursements"), null))
        .hasSize(2);
    JsonNode funds = ok("auditor", get(BASE + "/petty-cash/funds?companyId=" + company()), null);
    for (JsonNode f : funds) {
      if (f.get("id").asLong() == id) {
        assertThat(f.get("cashBalance").decimalValue()).isEqualByComparingTo("2000.00");
      }
    }
  }
}
