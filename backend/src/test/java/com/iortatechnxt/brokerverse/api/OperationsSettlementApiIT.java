package com.iortatechnxt.brokerverse.api;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.remittance.RemittanceFixtures;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The Operations changes of BRD-5 through HTTP (A1-OPSX): remittance deductions (ACSL 2.9.2), the
 * batch settlement (CPC2, deductions, send cycle), the invoice family and the invoice search by
 * assured, inception and account officer (DIS 3.27.2), and the root invoice on booking.
 */
@IntegrationTest
class OperationsSettlementApiIT {

  private static final String DEDUCTIONS = "/api/v1/remittance/deductions";
  private static final String INVOICES = "/api/v1/ops/invoices";

  @Autowired private Api api;
  @Autowired private RemittanceFixtures fx;

  private String c() {
    return fx.company().toString();
  }

  @ParameterizedTest
  @CsvSource({
    "acsl, " + DEDUCTIONS + "?companyId={c}",
    "acsltl, " + DEDUCTIONS + "?companyId={c}&stage=FOR_CONFIRMATION&stage=CONFIRMED",
    "remit, " + DEDUCTIONS + "?companyId={c}&insurer=INS-MGIC&q=RDN",
    "remit, " + DEDUCTIONS + "/pending?companyId={c}&insurer=INS-MGIC&currency=PHP",
    "remittl, " + DEDUCTIONS + "/by-batch/0",
    "cashier, "
        + INVOICES
        + "?companyId={c}&assured=a&inceptionFrom=2026-01-01"
        + "&inceptionTo=2027-12-31&ao=ao",
  })
  void readEndpointsRespondOk(String username, String url) throws Exception {
    api.doGet(username, url.replace("{c}", c())).andExpect(status().isOk());
  }

  @ParameterizedTest
  @CsvSource({
    "ao, " + DEDUCTIONS + "?companyId={c}",
    "cashier, " + DEDUCTIONS + "/pending?companyId={c}&insurer=INS-MGIC&currency=PHP",
    "acsl, " + DEDUCTIONS + "/by-batch/0",
  })
  void usersWithoutTheRightPermissionAreRefused(String username, String url) throws Exception {
    api.doGet(username, url.replace("{c}", c())).andExpect(status().isForbidden());
  }

  @Test
  void aDeductionIsPreparedSubmittedAndConfirmedThroughTheApi() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("companyId", fx.company());
    body.put("insurerCode", "INS-VMI");
    body.put("currency", "USD");
    body.put("sourceType", "AR_INSURER_REFUND");
    body.put("sourceRef", "ARI-" + BookingFixtures.token());
    body.put("amount", "150.00");
    api.doPost("remit", DEDUCTIONS, body).andExpect(status().isForbidden());
    body.put("amount", "0");
    api.doPost("acsl", DEDUCTIONS, body).andExpect(status().isBadRequest());
    body.put("amount", "150.00");
    JsonNode created =
        api.read(
            api.doPost("acsl", DEDUCTIONS, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stage").value("DRAFT"))
                .andExpect(jsonPath("$.remaining").value(150.0)));
    String one = DEDUCTIONS + "/" + created.get("id").asLong();
    api.doPost("acsl", one + "/submit", Map.of()).andExpect(status().isUnprocessableEntity());
    body.put("confirmationRef", "VMI-LTR-1");
    body.put("confirmationDate", "2026-09-20");
    api.doPut("acsl", one, body).andExpect(jsonPath("$.confirmationRef").value("VMI-LTR-1"));
    api.doPost("acsl", one + "/submit", Map.of("comment", "Letter attached"))
        .andExpect(jsonPath("$.stage").value("FOR_CONFIRMATION"));
    api.doPost("acsl", one + "/confirm", Map.of()).andExpect(status().isForbidden());
    api.doPost("acsltl", one + "/confirm", Map.of("comment", "OK"))
        .andExpect(jsonPath("$.stage").value("CONFIRMED"))
        .andExpect(jsonPath("$.confirmedBy").value("acsltl"));
    api.doGet("remit", one).andExpect(jsonPath("$.deductionNo").isNotEmpty());
    api.doGet("acsl", one + "/applications").andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void aBatchShowsItsSettlementAndTheInvoiceSearchFindsItsFamily() throws Exception {
    OpsInvoice paid = fx.paidInvoice();
    RemittanceBatch batch = fx.approvedBatch(paid);
    api.doGet("remit", "/api/v1/remittance/batches/" + batch.getId())
        .andExpect(jsonPath("$.settlement.sendCycle").value(1))
        .andExpect(jsonPath("$.settlement.reference").value(batch.getBatchNo()))
        .andExpect(jsonPath("$.summary.totals.cpc2").isNumber())
        .andExpect(jsonPath("$.lines[0].cpc2Code").value("CPC2"));
    api.doGet("remit", DEDUCTIONS + "/by-batch/" + batch.getId())
        .andExpect(jsonPath("$").isEmpty());

    String no = paid.getInvoiceNo();
    String ao = paid.getClassification().aoUsername();
    api.doGet(
            "cashier",
            INVOICES
                + "?companyId="
                + c()
                + "&q="
                + no
                + "&assured="
                + paid.getAssuredName().substring(0, 3)
                + "&inceptionFrom="
                + paid.getClassification().inceptionDate()
                + "&inceptionTo="
                + paid.getClassification().inceptionDate()
                + (ao == null ? "" : "&ao=" + ao))
        .andExpect(jsonPath("$.content[0].invoiceNo").value(no))
        .andExpect(jsonPath("$.content[0].rootInvoiceNo").value(no));
    api.doGet("cashier", INVOICES + "?companyId=" + c() + "&q=" + no + "&inceptionFrom=2099-01-01")
        .andExpect(jsonPath("$.content").isEmpty());
    api.doGet("cashier", INVOICES + "/" + no + "/family")
        .andExpect(jsonPath("$[0].keys.rootInvoiceNo").value(no));
    api.doGet("proc", "/api/v1/booking/invoices/by-no/" + no)
        .andExpect(jsonPath("$.rootInvoiceNo").value(no));
  }
}
