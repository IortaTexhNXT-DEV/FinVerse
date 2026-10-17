package com.iortatechnxt.brokerverse.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.cashiering.CashFixtures;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The unapplied-payment endpoints of Collections and the request endpoints of Cashiering through
 * the full HTTP stack (wave C1-C): every read endpoint answers, a collector disposition becomes a
 * Cashiering request that a cashier accepts or rejects, and the permissions hold.
 */
@IntegrationTest
class CollectionsUnappliedApiIT {

  private static final String CLX = "/api/v1/collections/unapplied";
  private static final String CSH = "/api/v1/cashiering";
  private static final String HANDLER = CollectionsFixtures.HANDLER;

  @Autowired private Api api;
  @Autowired private CashFixtures cash;

  private Unapplied unmatched(String amount) {
    return cash.pay("UNKNOWN-" + System.nanoTime(), new BigDecimal(amount)).unapplied();
  }

  private JsonNode dispose(String ref, String code, String invoiceNo) throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("companyId", cash.company());
    body.put("dispositionCode", code);
    body.put("invoiceNo", invoiceNo);
    body.put("remarks", "Client confirmed by phone");
    return api.read(
        api.doPost(HANDLER, CLX + "/" + ref + "/dispositions", body)
            .andExpect(status().isCreated()));
  }

  private long queuedRequest(String ref) throws Exception {
    JsonNode page =
        api.read(
            api.doGet(
                    "cashier",
                    CSH + "/collector-requests?companyId=" + cash.company() + "&q=" + ref)
                .andExpect(status().isOk()));
    // q matches the request number, invoice or requester, so look the item up in the rows.
    for (JsonNode row : page.get("content")) {
      if (ref.equals(row.get("unappliedRef").asText())) {
        return row.get("id").asLong();
      }
    }
    JsonNode all =
        api.read(
            api.doGet(
                    "cashier",
                    CSH + "/collector-requests?companyId=" + cash.company() + "&size=200")
                .andExpect(status().isOk()));
    for (JsonNode row : all.get("content")) {
      if (ref.equals(row.get("unappliedRef").asText())) {
        return row.get("id").asLong();
      }
    }
    throw new AssertionError("No queued request for " + ref);
  }

  @Test
  void theCollectorEndpointsListDisposeAndShowTheRequest() throws Exception {
    OpsInvoice invoice = cash.motorInvoice();
    Unapplied item = unmatched("210.00");
    String ref = item.getReference();
    String c = "?companyId=" + cash.company();

    api.doGet(HANDLER, CLX + c + "&q=" + ref + "&tab=UNAPPLIED&paidFrom=2020-01-01&ageMax=30")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].unappliedRef").value(ref))
        .andExpect(jsonPath("$.content[0].cashieringTab").value("UNAPPLIED"));
    api.doGet(HANDLER, CLX + "/disposition-rules")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.invoicePattern").isNotEmpty());

    JsonNode d = dispose(ref, "FOR_APPLICATION_TO_INVOICE", invoice.getInvoiceNo());
    assertThat(d.get("cashieringAction").asText()).isEqualTo("APPLY_TO_INVOICE");
    for (String url :
        List.of(
            CLX + c + "&disposition=FOR_APPLICATION_TO_INVOICE&q=" + ref,
            CLX + "/" + ref + c,
            CLX + "/" + ref + "/history" + c,
            CLX + "/requests" + c,
            CLX + "/requests" + c + "&status=SENT&q=" + ref)) {
      api.doGet(HANDLER, url).andExpect(status().isOk());
    }
    JsonNode detail = api.read(api.doGet(HANDLER, CLX + "/" + ref + c));
    long requestId = detail.get("requests").get(0).get("id").asLong();
    assertThat(detail.get("dispositions").get(0).get("dispositionCode").asText())
        .isEqualTo("FOR_APPLICATION_TO_INVOICE");
    api.doPost(HANDLER, CLX + "/requests/" + requestId + "/refresh", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SENT"));

    long queued = queuedRequest(ref);
    api.doGet("cashier", CSH + "/unapplied/" + item.getId() + "/collector-requests")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("QUEUED"));
    api.doPost("cashier", CSH + "/collector-requests/" + queued + "/accept", Map.of("submit", true))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));
    api.doGet(HANDLER, CLX + "/" + ref + c)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requests[0].status").value("APPLIED"));

    api.doPost("clxuh", CLX + "/application-file" + c + "&day=2099-12-31", Map.of())
        .andExpect(status().isOk());
    api.doPost(
            "proc",
            CLX + "/" + ref + "/dispositions",
            Map.of("companyId", cash.company(), "dispositionCode", "COORDINATE_FURTHER"))
        .andExpect(status().isForbidden());
    api.doGet("proc", CSH + "/collector-requests" + c).andExpect(status().isForbidden());
  }

  @Test
  void cashiersRejectRequestsAndReadTheValidationAndReversalQueues() throws Exception {
    Unapplied item = unmatched("80.00");
    String ref = item.getReference();
    dispose(ref, "FOR_REFUND", null);
    long queued = queuedRequest(ref);
    api.doPost(
            "cashier",
            CSH + "/collector-requests/" + queued + "/reject",
            Map.of("reason", "Duplicate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"));
    api.doGet(HANDLER, CLX + "/" + ref + "?companyId=" + cash.company())
        .andExpect(jsonPath("$.requests[0].status").value("REJECTED"));

    String c = "?companyId=" + cash.company();
    for (String url :
        List.of(
            CSH + "/requests/counts" + c,
            CSH + "/collector-requests" + c + "&status=REJECTED&status=APPLIED",
            CSH + "/refund-validations" + c,
            CSH + "/refund-validations" + c + "&status=CONFIRMED",
            CSH + "/payment-reversals" + c,
            CSH + "/payment-reversals" + c + "&status=APPROVED")) {
      api.doGet("cashtl", url).andExpect(status().isOk());
    }
    api.doPost("cashier", CSH + "/refund-validations/999999999/reject", Map.of("reason", "x"))
        .andExpect(status().isNotFound());
    api.doGet("cashier", CSH + "/refund-validations/999999999/candidates")
        .andExpect(status().isNotFound());
    api.doPost("cashier", CSH + "/refund-validations/999999999/confirm", Map.of("unappliedId", 1))
        .andExpect(status().isNotFound());
    api.doPost("cashtl", CSH + "/payment-reversals/999999999/approve", Map.of())
        .andExpect(status().isNotFound());
    api.doPost("cashtl", CSH + "/payment-reversals/999999999/reject", Map.of("reason", "x"))
        .andExpect(status().isNotFound());
    api.doPost("cashier", CSH + "/payment-reversals/999999999/approve", Map.of())
        .andExpect(status().isForbidden());
  }
}
