package com.iortatechnxt.brokerverse.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.commission.CommissionFixtures;
import com.iortatechnxt.brokerverse.commission.domain.DpBilling;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/** Commission receivables endpoints through HTTP (CMRID.001-015). */
@IntegrationTest
class CommissionApiIT {

  private static final String BASE = "/api/v1/commission";
  private static final String HANDLER = "commrec";
  private static final String LEAD = "commtl";

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private CommissionFixtures fx;

  private String c() {
    return fx.company().toString();
  }

  @ParameterizedTest
  @CsvSource({
    "commrec, /dp/lists?companyId={c}",
    "commrec, /dp/lists?companyId={c}&from=2026-09-01&to=2026-09-30",
    "commrec, /dp/submissions?companyId={c}&from=2026-09-01&to=2026-09-30",
    "commrec, /dp/items?companyId={c}",
    "commrec, /dp/items?companyId={c}&tag=DP_FOR_BILLING&tag=BILLED&sanitation=VALID&q=BI",
    "commrec, /dp/items/counts?companyId={c}",
    "commrec, /dp/billings?companyId={c}",
    "commrec, /dp/billings?companyId={c}&stage=AWAITING_INSURER&insurer=INS-MGIC",
    "commtl, /incentives/schemes?companyId={c}",
    "commtl, /incentives/runs?companyId={c}",
    "commrec, /certificates?companyId={c}",
    "comptrol, /certificates?companyId={c}&stage=SUBMITTED",
    "commrec, /certificates/receipts?companyId={c}&insurer=INS-MGIC",
    "commrec, /estimated?companyId={c}",
    "commrec, /settings",
  })
  void readEndpointsRespondOk(String username, String url) throws Exception {
    api.doGet(username, BASE + url.replace("{c}", c())).andExpect(status().isOk());
  }

  @ParameterizedTest
  @CsvSource({
    "cashier, /dp/lists?companyId={c}",
    "recon, /dp/billings?companyId={c}",
    "ao, /incentives/schemes?companyId={c}",
    "cashier, /certificates?companyId={c}",
    "disb, /settings",
  })
  void usersWithoutCommissionRightsAreRefused(String username, String url) throws Exception {
    api.doGet(username, BASE + url.replace("{c}", c())).andExpect(status().isForbidden());
  }

  @Test
  void aDpListIsBilledAnsweredAndCollectedThroughTheApi() throws Exception {
    OpsInvoice dp = fx.directPayment();
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "HO_DP_20260930.csv",
            "text/csv",
            CommissionFixtures.file(List.of(CommissionFixtures.row(dp))));
    JsonNode list =
        api.read(
            mvc.perform(
                    multipart(BASE + "/dp/lists")
                        .file(file)
                        .param("companyId", c())
                        .with(user(users.loadUserByUsername(HANDLER)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(1))
                .andExpect(jsonPath("$.validCount").value(1)));
    JsonNode items =
        api.read(
            api.doGet(
                    HANDLER,
                    BASE + "/dp/items?companyId=" + c() + "&listId=" + list.get("id").asLong())
                .andExpect(status().isOk()));
    long itemId = items.get("content").get(0).get("id").asLong();
    api.doPost(HANDLER, BASE + "/dp/items/confirm", Map.of("ids", List.of(itemId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].tag").value("DP_FOR_BILLING"));
    api.doPost("recon", BASE + "/dp/items/confirm", Map.of("ids", List.of(itemId)))
        .andExpect(status().isForbidden());
    JsonNode billing =
        api.read(
            api.doPost(
                    HANDLER,
                    BASE + "/dp/billings",
                    Map.of("companyId", fx.company(), "ids", List.of(itemId)))
                .andExpect(status().isOk()));
    long id = billing.get(0).get("id").asLong();
    api.doGet(HANDLER, BASE + "/dp/billings/" + id).andExpect(status().isOk());
    api.doGet(HANDLER, BASE + "/dp/billings/" + id + "/items").andExpect(status().isOk());
    api.doPost(
            HANDLER,
            BASE + "/dp/billings/" + id + "/send",
            Map.of("to", List.of("billing@insurer-seed.ph")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value(DpBilling.AWAITING));
    api.doGet(HANDLER, BASE + "/dp/billings/" + id + "/file").andExpect(status().isOk());
    api.doPost(HANDLER, BASE + "/dp/billings/" + id + "/cancel", Map.of("comment", "late"))
        .andExpect(status().is4xxClientError());
    Map<String, Object> answer = new HashMap<>();
    answer.put("invoiceNo", dp.getInvoiceNo());
    answer.put("approved", true);
    api.doPost(
            HANDLER, BASE + "/dp/billings/" + id + "/answers", Map.of("answers", List.of(answer)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value(DpBilling.APPROVED));
    api.doPost(HANDLER, BASE + "/dp/billings/" + id + "/collect", Map.of("bankAccount", "1111"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("CLOSED"));
    api.doPost(
            HANDLER,
            BASE + "/dp/items/" + itemId + "/reinstate",
            Map.of("reasonCode", "DP_DOUBLE_REVERSAL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tag").value("COLLECTED"));
    api.doPost(HANDLER, BASE + "/dp/items/" + itemId + "/reverse", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tag").value("PR_REVERSED"));
  }

  @Test
  void insurerAnswersAreUploaded() throws Exception {
    OpsInvoice dp = fx.directPayment();
    DpBilling billing = fx.billed(List.of(dp));
    String csv =
        "Billing No.,Invoice No.,Decision,Reason,Comment\n"
            + billing.getBillingNo()
            + ","
            + dp.getInvoiceNo()
            + ",APPROVED,,\n";
    mvc.perform(
            multipart(BASE + "/dp/responses")
                .file(
                    new MockMultipartFile(
                        "file",
                        "answers-" + BookingFixtures.token() + ".csv",
                        "text/csv",
                        csv.getBytes(StandardCharsets.UTF_8)))
                .with(user(users.loadUserByUsername(HANDLER)))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    api.doGet(HANDLER, BASE + "/dp/billings/" + billing.getId())
        .andExpect(jsonPath("$.stage").value(DpBilling.APPROVED));
  }

  @Test
  void incentivesCertificatesAndEstimatesThroughTheApi() throws Exception {
    OpsInvoice invoice = fx.regular();
    Map<String, Object> tier = new HashMap<>();
    tier.put("minBasicPremium", 1);
    tier.put("fixedAmount", 25);
    Map<String, Object> terms = new HashMap<>();
    terms.put("name", "API scheme");
    terms.put("schemeType", "OTHER");
    terms.put("calculation", "FIXED_PER_POLICY");
    terms.put("periodType", "CUSTOM");
    terms.put("beneficiary", "BDOI");
    terms.put("insurerCode", invoice.getInsurerCode());
    terms.put("active", true);
    terms.put("tiers", List.of(tier));
    JsonNode scheme =
        api.read(
            api.doPost(
                    LEAD,
                    BASE + "/incentives/schemes",
                    Map.of(
                        "companyId",
                        fx.company(),
                        "code",
                        "API" + BookingFixtures.token(),
                        "terms",
                        terms))
                .andExpect(status().isOk()));
    long schemeId = scheme.get("id").asLong();
    api.doPost(
            HANDLER,
            BASE + "/incentives/schemes",
            Map.of("companyId", fx.company(), "code", "NOPE", "terms", terms))
        .andExpect(status().isForbidden());
    api.doPut(LEAD, BASE + "/incentives/schemes/" + schemeId, terms).andExpect(status().isOk());
    String day = invoice.getBookingDate().toString();
    JsonNode run =
        api.read(
            api.doPost(
                    LEAD,
                    BASE + "/incentives/runs",
                    Map.of("schemeId", schemeId, "from", day, "to", day))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPUTED")));
    long runId = run.get("id").asLong();
    api.doGet(LEAD, BASE + "/incentives/runs/" + runId).andExpect(status().isOk());
    api.doGet(LEAD, BASE + "/incentives/runs/" + runId + "/lines").andExpect(status().isOk());
    api.doPost(HANDLER, BASE + "/incentives/runs/" + runId + "/post", Map.of())
        .andExpect(status().isForbidden());
    api.doPost(LEAD, BASE + "/incentives/runs/" + runId + "/post", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("POSTED"));

    Map<String, Object> cert = new HashMap<>();
    cert.put("companyId", fx.company());
    cert.put("insurerCode", "INS-MGIC");
    cert.put("form", "2307");
    cert.put("number", "API-" + BookingFixtures.token());
    cert.put("periodFrom", "2026-07-01");
    cert.put("periodTo", "2026-09-30");
    cert.put("taxWithheld", 100);
    cert.put("receipts", List.of(Map.of("orNo", "OR-" + BookingFixtures.token(), "amount", 1000)));
    JsonNode submitted =
        api.read(
            api.doPost(HANDLER, BASE + "/certificates", cert)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("SUBMITTED")));
    long certId = submitted.get("id").asLong();
    api.doGet("comptrol", BASE + "/certificates/" + certId).andExpect(status().isOk());
    api.doPost(HANDLER, BASE + "/certificates/" + certId + "/acknowledge", Map.of())
        .andExpect(status().isForbidden());
    api.doPost(
            "comptrol", BASE + "/certificates/" + certId + "/reject", Map.of("reason", "Blurred"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("REJECTED"));
    api.doPut(HANDLER, BASE + "/certificates/" + certId, cert)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("SUBMITTED"));
    api.doPost("comptrol", BASE + "/certificates/" + certId + "/acknowledge", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("ACKNOWLEDGED"));

    api.doPost(
            HANDLER,
            BASE + "/estimated",
            Map.of("invoiceNo", invoice.getInvoiceNo(), "estimated", true, "reason", "Estimate"))
        .andExpect(status().isOk());
    api.doPost(
            HANDLER,
            BASE + "/estimated",
            Map.of("invoiceNo", invoice.getInvoiceNo(), "estimated", false, "reason", "Final"))
        .andExpect(status().isOk());
  }
}
