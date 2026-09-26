package com.iortatechnxt.brokerverse.adjustment;

import static com.iortatechnxt.brokerverse.adjustment.AdjustmentFixtures.FROM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** Adjustment endpoints through the full HTTP stack: reads, the request flow and permissions. */
@IntegrationTest
class AdjustmentApiIT {

  private static final String BASE = "/api/v1/adjustment";

  @Autowired private Api api;
  @Autowired private AdjustmentFixtures fx;

  private static Map<String, Object> cancellation(String invoiceNo) {
    return Map.of(
        "invoiceNos", List.of(invoiceNo),
        "endorsementType", "FIN_CHANGE_COVER",
        "requestType", "FLAT_CANCELLATION",
        "reasonCode", "UNIT_SOLD",
        "effectiveDate", FROM.toString(),
        "description", "Unit sold over HTTP");
  }

  @Test
  void readEndpointsRespond() throws Exception {
    EndorsementRequest request =
        fx.raiseAndPost(
            fx.invoice(),
            AdjustmentFixtures.cancellation("FLAT_CANCELLATION", FROM),
            AmountInput.NONE);
    String c = "?companyId=" + fx.company();
    String id = String.valueOf(request.getId());
    for (String url :
        List.of(
            BASE + "/requests" + c,
            BASE + "/requests" + c + "&stage=POSTED&q=enr&page=0&size=5",
            BASE + "/requests/counts" + c,
            BASE + "/requests/" + id,
            BASE + "/requests/" + id + "/recompute",
            BASE + "/requests/" + id + "/journal",
            BASE + "/invoices/" + request.getSubject().invoiceNo() + "/requests",
            BASE + "/batches" + c,
            BASE + "/batches/" + request.outcome().batchNo(),
            BASE + "/write-offs" + c)) {
      api.doGet("adjust", url).andExpect(status().isOk());
    }
    api.doGet("adjust", BASE + "/requests/" + id)
        .andExpect(jsonPath("$.stage").value("POSTED"))
        .andExpect(jsonPath("$.invoice.arn").value(request.getSubject().arn()));
    api.doGet("mktcoll", BASE + "/requests/" + id + "/validation-slip")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF));
  }

  @Test
  void aRequestIsRaisedAndMovedOverHttp() throws Exception {
    OpsInvoice invoice = fx.invoice();
    api.doPost("mktcoll", BASE + "/requests/preview", cancellation(invoice.getInvoiceNo()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.computation").value("CANCELLATION_FLAT"))
        .andExpect(jsonPath("$.negative").value(true))
        .andExpect(jsonPath("$.serviceInvoice.action").value("CREDIT"));
    JsonNode created =
        api.read(
            api.doPost("mktcoll", BASE + "/requests", cancellation(invoice.getInvoiceNo()))
                .andExpect(status().isOk()));
    long id = created.get(0).get("id").asLong();
    String url = BASE + "/requests/" + id;

    api.doPut("mktcoll", url, cancellation(invoice.getInvoiceNo())).andExpect(status().isOk());
    api.doGet("mktcoll", url + "/endorsement-slip").andExpect(status().isOk());
    api.doPost("mktcoll", url + "/submit", Map.of("comment", "Go")).andExpect(status().isOk());
    api.doPost("adjust", url + "/validate", Map.of()).andExpect(status().isOk());
    api.doPost("adjtl", url + "/approve", Map.of()).andExpect(status().isOk());
    api.doPost("adjust", url + "/post", Map.of("comment", "HTTP batch"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("POSTED"));
    api.doPost("adjust", url + "/reapply", Map.of()).andExpect(status().isUnprocessableEntity());
    api.doPost("mktcoll", url + "/quotation", Map.of("quotationRef", "QT-1"))
        .andExpect(status().isUnprocessableEntity());
    assertThat(fx.reload(invoice).isCancelled()).isTrue();
  }

  @Test
  void batchesAndReturnsOverHttp() throws Exception {
    EndorsementRequest ready =
        fx.toPosting(
            fx.raise(
                fx.invoice(),
                AdjustmentFixtures.cancellation("FLAT_CANCELLATION", FROM),
                AmountInput.NONE));
    EndorsementRequest back =
        fx.toPosting(
            fx.raise(
                fx.invoice(),
                AdjustmentFixtures.cancellation("FLAT_CANCELLATION", FROM),
                AmountInput.NONE));

    api.doPost(
            "adjust",
            BASE + "/batches/return",
            Map.of("ids", List.of(back.getId()), "reasonCode", "NOT_QUALIFIED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.returned").value(1));
    api.doPost(
            "adjust",
            BASE + "/batches",
            Map.of("companyId", fx.company(), "ids", List.of(ready.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.postedCount").value(1))
        .andExpect(jsonPath("$.lines[0].outcome").value("POSTED"));
  }

  @Test
  void permissionsAreEnforced() throws Exception {
    api.doGet("epol", BASE + "/requests?companyId=" + fx.company())
        .andExpect(status().isForbidden());
    api.doPost("remit", BASE + "/requests", cancellation("X")).andExpect(status().isForbidden());
    api.doPost("mktcoll", BASE + "/batches", Map.of("companyId", 1, "ids", List.of(1)))
        .andExpect(status().isForbidden());
    api.doPost("adjust", BASE + "/requests/1/approve", Map.of()).andExpect(status().isForbidden());
    api.doPost("mktcoll", BASE + "/requests", Map.of("endorsementType", ""))
        .andExpect(status().isBadRequest());
  }
}
