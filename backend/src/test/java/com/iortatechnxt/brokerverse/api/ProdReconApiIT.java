package com.iortatechnxt.brokerverse.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.prodrecon.ReconFixtures;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
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

/** Production reconciliation endpoints through HTTP (PRCID.001-039). */
@IntegrationTest
class ProdReconApiIT {

  private static final String BASE = "/api/v1/prodrecon";
  private static final String RECON = "recon";

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private ReconFixtures fx;

  private String c() {
    return fx.company().toString();
  }

  @ParameterizedTest
  @CsvSource({
    "recon, /cycles?companyId={c}",
    "recon, /cycles?companyId={c}&insurer=INS-MGIC&month=2026-09-01&stage=RECONCILING",
    "recon, /extracts?companyId={c}",
    "recon, /extracts?companyId={c}&insurer=INS-MGIC",
    "recon, /uploads?companyId={c}",
    "recon, /uploads?companyId={c}&insurer=INS-MGIC",
    "recon, /unbooked?companyId={c}",
    "recon, /unbooked?companyId={c}&insurer=INS-MGIC&status=OPEN&q=pol",
    "recon, /schedules?companyId={c}",
    "recon, /settings",
  })
  void readEndpointsRespondOk(String username, String url) throws Exception {
    api.doGet(username, BASE + url.replace("{c}", c())).andExpect(status().isOk());
  }

  @ParameterizedTest
  @CsvSource({
    "cashier, /cycles?companyId={c}",
    "commrec, /extracts?companyId={c}",
    "ao, /settings",
  })
  void usersWithoutReconciliationRightsAreRefused(String username, String url) throws Exception {
    api.doGet(username, BASE + url.replace("{c}", c())).andExpect(status().isForbidden());
  }

  @Test
  void aCycleIsWorkedThroughTheApi() throws Exception {
    OpsInvoice a = fx.invoice();
    fx.invoice();
    JsonNode extract =
        api.read(
            api.doPost(
                    RECON,
                    BASE + "/extracts",
                    Map.of(
                        "companyId", fx.company(),
                        "insurerCode", ReconFixtures.INSURER,
                        "from", BookingFixtures.BOOKED_ON.toString(),
                        "to", BookingFixtures.BOOKED_ON.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trigger").value("MANUAL")));
    long extractId = extract.get("id").asLong();
    long cycleId = extract.get("cycleId").asLong();
    api.doGet(RECON, BASE + "/extracts/" + extractId + "/lines").andExpect(status().isOk());
    api.doGet(RECON, BASE + "/extracts/" + extractId + "/file").andExpect(status().isOk());
    api.doGet(RECON, BASE + "/cycles/" + cycleId + "/extracts").andExpect(status().isOk());
    api.doPost(RECON, BASE + "/extracts/" + extractId + "/send", Map.of("to", List.of("x@y.ph")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sentAt").isNotEmpty());
    api.doPost("cashier", BASE + "/extracts/" + extractId + "/send", Map.of())
        .andExpect(status().isForbidden());

    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "api-" + BookingFixtures.token() + ".csv",
            "text/csv",
            ReconFixtures.file(List.of(ReconFixtures.line(a, new BigDecimal("2.50")))));
    mvc.perform(
            multipart(BASE + "/uploads")
                .file(file)
                .param("companyId", c())
                .with(user(users.loadUserByUsername(RECON)))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runStatus").value("SUCCEEDED"))
        .andExpect(jsonPath("$.attempts[0].status").value("PROCESSED"));

    api.doGet(RECON, BASE + "/cycles/" + cycleId)
        .andExpect(jsonPath("$.stage").value("RECONCILING"))
        .andExpect(jsonPath("$.counts.discrepancy").isNumber());
    JsonNode discrepant =
        api.read(
            api.doGet(
                    RECON,
                    BASE
                        + "/cycles/"
                        + cycleId
                        + "/items?bucket=DISCREPANCY&q="
                        + a.getInvoiceNo()
                        + "&segment=CBG&unit=")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].discrepancies[0]").value("GROSS_PREMIUM")));
    long itemId = discrepant.get("content").get(0).get("id").asLong();
    Map<String, Object> feedback = new HashMap<>();
    feedback.put("companyConcerned", "INSURER");
    feedback.put("disposition", "INSURER_TO_CORRECT");
    feedback.put("insurerFeedback", "Premium keyed with charges");
    feedback.put("forClosure", false);
    api.doPut(RECON, BASE + "/items/" + itemId + "/feedback", feedback)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.feedback.disposition").value("INSURER_TO_CORRECT"));
    api.doPost(
            RECON,
            BASE + "/items/feedback",
            Map.of("ids", List.of(itemId), "disposition", "FOR_CLOSURE", "forClosure", true))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].feedback.forClosure").value(true));
    api.doPost(RECON, BASE + "/items/" + itemId + "/split", null).andExpect(status().isOk());
    api.doPost(RECON, BASE + "/items/" + itemId + "/pair", Map.of("insurerItemId", itemId))
        .andExpect(status().isUnprocessableEntity());
    api.doPost(RECON, BASE + "/cycles/" + cycleId + "/automatch", null)
        .andExpect(jsonPath("$.changed").isNumber());
    api.doGet(RECON, BASE + "/cycles/" + cycleId + "/early-incentive")
        .andExpect(jsonPath("$[0].outcome").isNotEmpty());
    api.doPost(RECON, BASE + "/cycles/" + cycleId + "/close", Map.of("comment", "Done"))
        .andExpect(jsonPath("$.stage").value("CLOSED"));
  }

  @Test
  void schedulesAreMaintained() throws Exception {
    String insurer = "INS-A" + BookingFixtures.token();
    JsonNode created =
        api.read(
            api.doPost(
                    RECON,
                    BASE + "/schedules",
                    Map.of(
                        "companyId",
                        fx.company(),
                        "insurerCode",
                        insurer,
                        "frequency",
                        "MONTHLY",
                        "runDay",
                        10,
                        "active",
                        true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frequency").value("MONTHLY")));
    api.doPut(
            RECON,
            BASE + "/schedules/" + created.get("id").asLong(),
            Map.of("frequency", "WEEKLY", "runDay", 2, "autoSend", true, "active", false))
        .andExpect(jsonPath("$.active").value(false));
    api.doPost(RECON, BASE + "/schedules", Map.of("frequency", "MONTHLY", "runDay", 3))
        .andExpect(status().isUnprocessableEntity());
    api.doPost(RECON, BASE + "/schedules", Map.of("frequency", "MONTHLY", "runDay", 40))
        .andExpect(status().isBadRequest());
    api.doPost(
            RECON,
            BASE + "/extracts",
            Map.of(
                "companyId",
                fx.company(),
                "insurerCode",
                insurer,
                "from",
                "2026-09-01",
                "to",
                "2026-09-30"))
        .andExpect(status().isUnprocessableEntity());
  }
}
