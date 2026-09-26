package com.iortatechnxt.brokerverse.api;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.cashiering.CashFixtures;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Cashiering endpoints through HTTP (CSHID.001-027): read endpoints, permissions and main flows.
 */
@IntegrationTest
class CashieringApiIT {

  private static final String BASE = "/api/v1/cashiering";

  @Autowired private Api api;
  @Autowired private CashFixtures fx;

  private String c() {
    return fx.company().toString();
  }

  @ParameterizedTest
  @CsvSource({
    "cashier, /receipts?companyId={c}",
    "cashier, /receipts?companyId={c}&kind=AR&status=ISSUED&payor=a&from=2026-01-01&to=2027-12-31",
    "cashtl, /receipt-actions?companyId={c}",
    "cashtl, /receipt-actions?companyId={c}&stage=POSTED",
    "cashier, /payments?companyId={c}",
    "cashier, /payments?companyId={c}&channel=OTC&category=APPLIED",
    "cashier, /prebooked?companyId={c}",
    "cashier, /prebooked?companyId={c}&status=APPLIED",
    "cashier, /unapplied?companyId={c}",
    "cashier, /unapplied?companyId={c}&tab=MONITORING&q=UNP",
    "cashtl, /unapplied?companyId={c}&tab=FOR_APPROVAL",
    "cashtl, /unapplied?companyId={c}&tab=FOR_REVERSAL",
    "cashtl, /unapplied?companyId={c}&tab=DONE",
    "cashier, /disposition-types",
    "cashier, /pdc?companyId={c}",
    "cashier, /pdc?companyId={c}&status=WAREHOUSED&from=2026-01-01&to=2027-12-31",
    "cashier, /pickups?companyId={c}",
    "cashier, /pickups?companyId={c}&status=AR_PRINTED&from=2026-01-01",
    "cashier, /print-batches?companyId={c}",
    "cashtl, /series?companyId={c}",
    "cashier, /layouts",
    "cashier, /minimal-balance/rules",
    "cashier, /commission-payments?companyId={c}",
    "mktcoll, /cwt?companyId={c}",
    "cashier, /cwt?companyId={c}&stage=RELEASED",
    "cashier, /cwt/batches?companyId={c}",
  })
  void readEndpointsRespondOk(String user, String url) throws Exception {
    api.doGet(user, BASE + url.replace("{c}", c())).andExpect(status().isOk());
  }

  @Test
  void readEndpointsNeedCashieringPermissions() throws Exception {
    api.doGet("ao", BASE + "/receipts?companyId=" + c()).andExpect(status().isForbidden());
    api.doGet("mktcoll", BASE + "/unapplied?companyId=" + c()).andExpect(status().isForbidden());
    api.doPost("cashbr", BASE + "/receipt-actions/1/approve", null)
        .andExpect(status().isForbidden());
  }

  @Test
  void aPaymentIsPreviewedReceivedPrintedAndCancelledThroughTheApi() throws Exception {
    OpsInvoice invoice = fx.motorInvoice();
    api.doPost(
            "cashier",
            BASE + "/payments/preview",
            Map.of(
                "companyId",
                fx.company(),
                "references",
                List.of(invoice.getInvoiceNo()),
                "amount",
                500))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.match").value("BOOKED"))
        .andExpect(jsonPath("$.invoices[0].invoiceNo").value(invoice.getInvoiceNo()));

    JsonNode intake =
        api.read(
            api.doPost(
                    "cashier",
                    BASE + "/payments",
                    Map.of(
                        "companyId",
                        fx.company(),
                        "branchId",
                        fx.ho(),
                        "references",
                        List.of(invoice.getInvoiceNo()),
                        "payorName",
                        "API Payor",
                        "currency",
                        "PHP",
                        "amount",
                        500,
                        "paymentDate",
                        LocalDate.now().toString(),
                        "mode",
                        "CASH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.matchCategory").value("APPLIED")));
    long receiptId = intake.get("receiptId").asLong();

    api.doGet("cashier", BASE + "/receipts/" + receiptId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applications[0].invoiceNo").value(invoice.getInvoiceNo()));
    api.doGet("cashier", BASE + "/receipts/" + receiptId + "/pdf")
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/pdf"));
    JsonNode batch =
        api.read(
            api.doPost(
                    "cashier",
                    BASE + "/print-batches",
                    Map.of("companyId", fx.company(), "ids", List.of(receiptId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printedCount").value(1)));
    api.doGet("cashier", BASE + "/print-batches/" + batch.get("id").asLong() + "/file")
        .andExpect(status().isOk());
    api.doGet("cashier", BASE + "/print-batches/" + batch.get("id").asLong())
        .andExpect(status().isOk());

    api.doPost("cashier", BASE + "/receipts/" + receiptId + "/cancel", Map.of("reasonCode", "BAD"))
        .andExpect(status().is4xxClientError());
    JsonNode action =
        api.read(
            api.doPost(
                    "cashier",
                    BASE + "/receipts/" + receiptId + "/cancel",
                    Map.of("reasonCode", "GEN_ISSUANCE_ERROR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("FOR_APPROVAL")));
    api.doPost("cashtl", BASE + "/receipt-actions/" + action.get("id").asLong() + "/approve", null)
        .andExpect(status().isOk());
    api.doGet("cashier", BASE + "/receipts/" + receiptId)
        .andExpect(jsonPath("$.summary.status").value("CANCELLED"));
  }

  @Test
  void anUnappliedItemIsDispositionedAndSeriesAreMaintainedThroughTheApi() throws Exception {
    JsonNode intake =
        api.read(
            api.doPost(
                    "cashier",
                    BASE + "/payments",
                    Map.of(
                        "companyId",
                        fx.company(),
                        "branchId",
                        fx.ho(),
                        "references",
                        List.of("API-NONE-" + System.nanoTime()),
                        "payorName",
                        "Unknown Payor",
                        "currency",
                        "PHP",
                        "amount",
                        60,
                        "paymentDate",
                        LocalDate.now().toString(),
                        "mode",
                        "CASH"))
                .andExpect(status().isOk()));
    long item = intake.get("unappliedId").asLong();
    api.doGet("cashier", BASE + "/unapplied/" + item).andExpect(status().isOk());
    api.doPost(
            "cashier",
            BASE + "/unapplied/" + item + "/disposition",
            Map.of("dispositionType", "REFUND", "amount", 60, "payeeName", "Unknown Payor"))
        .andExpect(status().isOk());
    api.doPost("cashier", BASE + "/unapplied/bulk/submit", Map.of("ids", List.of(item)))
        .andExpect(jsonPath("$.done[0]").exists());
    api.doPost("cashtl", BASE + "/unapplied/bulk/approve", Map.of("ids", List.of(item)))
        .andExpect(jsonPath("$.done[0]").exists());
    api.doGet("cashier", BASE + "/unapplied/" + item + "/dispositions")
        .andExpect(jsonPath("$[0].status").value("COMPLETED"));

    api.doPost(
            "cashtl",
            BASE + "/series",
            Map.of(
                "companyId",
                fx.company(),
                "branchId",
                fx.branch("DVO"),
                "kind",
                "AR",
                "prefix",
                "AR-API" + (System.nanoTime() % 1000) + "-",
                "fromNo",
                1,
                "toNo",
                100,
                "warnAt",
                10))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recordStatus").value("PENDING_AUTHORIZATION"));
    api.doPost(
            "cashier",
            BASE + "/series",
            Map.of(
                "companyId",
                fx.company(),
                "branchId",
                fx.ho(),
                "kind",
                "AR",
                "prefix",
                "X-",
                "fromNo",
                1,
                "toNo",
                2,
                "warnAt",
                0))
        .andExpect(status().isForbidden());
    api.doPost("cashtl", BASE + "/matching/run", null).andExpect(status().isOk());
    api.doPost("cashtl", BASE + "/minimal-balance/sweep", null).andExpect(status().isOk());
  }
}
