package com.iortatechnxt.brokerverse.payrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** Refund and cash-advance request endpoints through the full HTTP stack (MKT 1.2-2.26). */
@IntegrationTest
class PayRequestApiIT {

  private static final String BASE = "/api/v1/payment-requests";

  @Autowired private Api api;
  @Autowired private PayRequestFixtures fx;

  private static Map<String, Object> refund(OpsInvoice invoice) {
    Map<String, Object> line = new HashMap<>();
    line.put("arNo", PayRequestFixtures.arNo());
    line.put("clientCode", invoice.getClientCode());
    line.put("assuredName", invoice.getAssuredName());
    line.put("invoiceNo", invoice.getInvoiceNo());
    line.put("amount", 120.50);
    line.put("reasonCode", "OVERPAYMENT");
    return Map.of(
        "segment", "CBG",
        "referenceText", "HTTP Refund",
        "paymentMode", "CTA",
        "accountNo", "001122334455",
        "accountName", invoice.getAssuredName(),
        "lines", List.of(line));
  }

  @Test
  void readEndpointsRespond() throws Exception {
    PaymentRequest r = fx.approvedRefund();
    String c = "?companyId=" + fx.company();
    String id = String.valueOf(r.getId());
    for (String url :
        List.of(
            BASE + "/requests" + c,
            BASE + "/requests" + c + "&stage=SENT_TO_DISBURSEMENT&kind=REFUND&q=rrf&page=0&size=5",
            BASE + "/requests" + c + "&from=2020-01-01&to=2099-12-31",
            BASE + "/requests/counts" + c,
            BASE + "/requests/" + id,
            BASE + "/requests/" + id + "/validations",
            BASE + "/liquidation-accounts" + c,
            BASE + "/payout-accounts" + c + "&clientCode=" + r.getPayee().code())) {
      api.doGet("mktrev", url).andExpect(status().isOk());
    }
    api.doGet("mktrev", BASE + "/requests/" + id)
        .andExpect(jsonPath("$.stage").value("SENT_TO_DISBURSEMENT"))
        .andExpect(jsonPath("$.lines[0].reasonCode").value("OVERPAYMENT"))
        .andExpect(jsonPath("$.track.status").value("SENT"));
    api.doGet("mktao", BASE + "/requests/" + id + "/form")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF));
  }

  @Test
  void aRefundIsRaisedAndApprovedOverHttp() throws Exception {
    OpsInvoice invoice = fx.invoice();
    JsonNode created =
        api.read(
            api.doPost(
                    "mktao", BASE + "/requests/refunds?companyId=" + fx.company(), refund(invoice))
                .andExpect(status().isOk()));
    long id = created.get("id").asLong();
    String url = BASE + "/requests/" + id;
    api.doPut("mktao", url + "/refund", refund(invoice)).andExpect(status().isOk());
    api.doPost("mktrev", url + "/assign", Map.of("username", "mktao"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("PREPARING"));
    api.doPost("mktao", url + "/submit", Map.of("comment", "Go")).andExpect(status().isOk());
    api.doPost("mktrev", url + "/endorse", Map.of()).andExpect(status().isOk());
    api.doPost("mktappr", url + "/approve", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("SENT_TO_DISBURSEMENT"))
        .andExpect(jsonPath("$.payoutRecorded").value(true));
    JsonNode accounts =
        api.read(
            api.doGet(
                    "mktao",
                    BASE
                        + "/payout-accounts?companyId="
                        + fx.company()
                        + "&clientCode="
                        + invoice.getClientCode())
                .andExpect(status().isOk()));
    assertThat(accounts.findValuesAsText("accountNo")).contains("001122334455");
  }

  @Test
  void cashAdvanceAndLiquidationOverHttp() throws Exception {
    Map<String, Object> ca = new HashMap<>();
    ca.put("purpose", "Field work");
    ca.put("employeeNo", "EMP-HTTP");
    ca.put("employeeName", "Http Employee");
    ca.put("paymentMode", "CHECK");
    ca.put("accountName", "Http Employee");
    ca.put("amount", 500);
    JsonNode created =
        api.read(
            api.doPost("mktao", BASE + "/requests/cash-advances?companyId=" + fx.company(), ca)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("CASH_ADVANCE")));
    String url = BASE + "/requests/" + created.get("id").asLong();
    api.doPut("mktao", url + "/cash-advance", ca).andExpect(status().isOk());
    api.doPut("mktao", url + "/liquidation", Map.of("days", List.of()))
        .andExpect(status().isUnprocessableEntity());
    api.doPost("mktao", url + "/liquidation/submit", Map.of()).andExpect(status().isNotFound());
    api.doPut(
            "fmanager",
            BASE + "/liquidation-accounts?companyId=" + fx.company(),
            Map.of("role", "OTHER", "accountCode", "5606"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountCode").value("5606"));
  }

  @Test
  void permissionsAndValidationAreEnforced() throws Exception {
    api.doGet("epol", BASE + "/requests?companyId=" + fx.company())
        .andExpect(status().isForbidden());
    api.doPost("mktappr", BASE + "/requests/refunds?companyId=1", refund(fx.invoice()))
        .andExpect(status().isForbidden());
    api.doPost("mktao", BASE + "/requests/1/approve", Map.of()).andExpect(status().isForbidden());
    api.doPut(
            "mktao",
            BASE + "/liquidation-accounts?companyId=1",
            Map.of("role", "OTHER", "accountCode", "5606"))
        .andExpect(status().isForbidden());
    api.doPost("mktao", BASE + "/requests/refunds?companyId=1", Map.of("paymentMode", ""))
        .andExpect(status().isBadRequest());
    api.doPost(
            "mktao",
            BASE + "/requests/check-cancellations?companyId=" + fx.company(),
            Map.of("targetRequestNo", "NO-SUCH", "reasonCode", "DUPLICATE"))
        .andExpect(status().isUnprocessableEntity());
  }
}
