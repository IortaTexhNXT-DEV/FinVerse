package com.iortatechnxt.brokerverse.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.disbursement.DisbursementFixtures;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * HTTP contract of Disbursement (DIS 2.2-3.28): the workbench counts, requests, vouchers with their
 * document and instrument, status edits, payees with masked account numbers, payee requests, end of
 * day with its files, account funding, bank accounts, and every Disbursement report in PDF, XLSX
 * and CSV; with the permission checks of each group.
 */
@IntegrationTest
class DisbursementApiIT {

  private static final String BASE = "/api/v1/disbursement";
  private static final String COMPANY = "?companyId=";

  @Autowired private Api api;
  @Autowired private DisbursementFixtures fx;
  @Autowired private ReportService reports;
  @Autowired private AsUser as;

  private String company() {
    return COMPANY + fx.company();
  }

  @Test
  void theWorkbenchRequestsAndVouchersAreReadAndProcessedThroughTheApi() throws Exception {
    Payee supplier = fx.supplierPayee();
    api.doGet("disb", BASE + "/summary" + company())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inProcess").isNumber());
    api.doGet("uw", BASE + "/summary" + company()).andExpect(status().isForbidden());

    Map<String, Object> encode = new HashMap<>();
    encode.put("companyId", fx.company());
    encode.put("disbursementType", "SUPPLIER");
    encode.put("payeeCode", supplier.getPayeeCode());
    encode.put("currency", "PHP");
    encode.put("amount", 4200.50);
    encode.put("purpose", "Printer toner");
    api.doPost("disbtl", BASE + "/requests", Map.of("companyId", fx.company()))
        .andExpect(status().isBadRequest());
    JsonNode request =
        api.read(
            api.doPost("disb", BASE + "/requests", encode)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_VOUCHER")));
    long requestId = request.get("id").asLong();
    long voucherId = request.get("voucherId").asLong();
    api.doGet(
            "disb",
            BASE + "/requests" + company() + "&status=IN_VOUCHER&q=" + supplier.getPayeeCode())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(requestId));
    api.doGet("disb", BASE + "/requests/" + requestId).andExpect(status().isOk());

    api.doGet("disb", BASE + "/vouchers" + company() + "&stage=IN_PROCESS")
        .andExpect(status().isOk());
    api.doGet("disb", BASE + "/vouchers/" + voucherId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary.stage").value("IN_PROCESS"))
        .andExpect(jsonPath("$.lines").isArray());
    api.doGet("disb", BASE + "/vouchers/" + voucherId + "/document")
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/pdf"));
    Map<String, Object> terms = new HashMap<>();
    terms.put("mode", "CHECK");
    terms.put("bankAccountId", fx.voucher(voucherId).getBankAccountId());
    terms.put("ewt", 42.01);
    terms.put("purpose", "Printer toner");
    terms.put("valueDate", LocalDate.now().toString());
    api.doPut("disb", BASE + "/vouchers/" + voucherId + "/terms", terms)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary.net").value(4158.49));
    api.doPost("disb", BASE + "/vouchers/" + voucherId + "/submit", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary.stage").value("FOR_REVIEW"));
    api.doPost("disb", BASE + "/vouchers/" + voucherId + "/approve", Map.of())
        .andExpect(status().isForbidden());
    api.doGet("disb", BASE + "/status-edits").andExpect(status().isOk());
  }

  @Test
  void theInstrumentOfAnApprovedVoucherIsPrintedAndDownloaded() throws Exception {
    Voucher approved = fx.approved("SUPPLIER", fx.supplierPayee(), "999.00");
    String base = BASE + "/vouchers/" + approved.getId() + "/instrument";
    api.doPost("disb", base + "/print", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.instrument.status").value("PRINTED"));
    api.doGet("disb", base + "/document")
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/pdf"));
    api.doPost("disb", base + "/release", Map.of("releasedTo", "Supplier messenger"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.instrument.status").value("RELEASED"));
    api.doPost("disb", base + "/status-edits", Map.of("toStatus", "PRINTED", "reason", "Not yet"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("REQUESTED"));
    api.doPost(
            "disb",
            BASE + "/vouchers/" + approved.getId() + "/tags/receipt",
            Map.of(
                "receiptNo",
                "OR-1",
                "receiptDate",
                LocalDate.now().toString(),
                "receivedOn",
                LocalDate.now().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tags[0].kind").value("OR_AR"));
  }

  @Test
  void payeesShowMaskedAccountsWithoutTheFullViewPermission() throws Exception {
    Payee client =
        fx.payee("API-" + BookingFixtures.token(), "CLIENT", DisbursementMode.CTA, "123456789012");
    api.doGet("disb", BASE + "/payees/" + client.getId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accounts[0].accountNo").value("********9012"));
    api.doGet("disbtl", BASE + "/payees/" + client.getId())
        .andExpect(jsonPath("$.accounts[0].accountNo").value("123456789012"));
    api.doGet("disb", BASE + "/payees" + company() + "&stage=ACTIVE&q=" + client.getPayeeCode())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].payeeCode").value(client.getPayeeCode()));
    api.doGet("disbtl", BASE + "/payee-requests" + company()).andExpect(status().isOk());

    Map<String, Object> body = new HashMap<>();
    body.put("companyId", fx.company());
    body.put("payeeCode", "APIN-" + BookingFixtures.token());
    body.put("payeeClass", "SUPPLIER");
    body.put("name", "Api Supplier");
    body.put("defaultMode", "CHECK");
    body.put("allowedModes", List.of("CHECK"));
    body.put("currency", "PHP");
    api.doPost("disb", BASE + "/payees", body).andExpect(status().isForbidden());
    JsonNode created =
        api.read(
            api.doPost("disbtl", BASE + "/payees", body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.summary.stage").value("DRAFT")));
    api.doPost(
            "disbtl", BASE + "/payees/" + created.at("/summary/id").asLong() + "/submit", Map.of())
        .andExpect(jsonPath("$.summary.stage").value("FOR_AUTHORIZATION"));
  }

  @Test
  void endOfDayFundingAndBankAccountsAreServed() throws Exception {
    fx.approved("REFUND", fx.clientPayee(), "120.00");
    api.doPost(
            "disb",
            BASE + "/eod/runs",
            Map.of(
                "companyId",
                fx.company(),
                "businessDate",
                DisbursementFixtures.uniqueDate().toString()))
        .andExpect(status().isForbidden());
    JsonNode run =
        api.read(
            api.doPost(
                    "disbtl",
                    BASE + "/eod/runs",
                    Map.of(
                        "companyId",
                        fx.company(),
                        "businessDate",
                        DisbursementFixtures.uniqueDate().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runNo").value(startsWith("EOD-"))));
    long runId = run.get("id").asLong();
    api.doGet("disbtl", BASE + "/eod/runs" + company()).andExpect(status().isOk());
    JsonNode detail =
        api.read(api.doGet("disb", BASE + "/eod/runs/" + runId).andExpect(status().isOk()));
    long outputId = detail.get("outputs").get(0).get("id").asLong();
    api.doGet("disb", BASE + "/eod/outputs/" + outputId).andExpect(status().isOk());
    api.doPost("disbtl", BASE + "/eod/runs/" + runId + "/confirm", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));

    api.doGet("disbtl", BASE + "/banks" + company())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].books").isArray());
    api.doGet("disbtl", BASE + "/funding" + company()).andExpect(status().isOk());
    JsonNode banks = api.read(api.doGet("disbtl", BASE + "/banks" + company()));
    Map<String, Object> funding = new HashMap<>();
    funding.put("companyId", fx.company());
    List<Long> php = new ArrayList<>();
    banks.forEach(
        b -> {
          if ("PHP".equals(b.get("currency").asText())
              && "ACTIVE".equals(b.get("status").asText())) {
            php.add(b.get("id").asLong());
          }
        });
    funding.put("sourceBankAccountId", php.get(1));
    funding.put("targetBankAccountId", php.get(0));
    funding.put("amount", 10.00);
    funding.put("currency", "PHP");
    funding.put("purpose", "API funding");
    funding.put("valueDate", LocalDate.now().toString());
    JsonNode created =
        api.read(
            api.doPost("disbtl", BASE + "/funding", funding)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stage").value("CREATED")));
    api.doGet("disbappr", BASE + "/funding/" + created.get("id").asLong())
        .andExpect(status().isOk());
    api.doPost("disbappr", BASE + "/funding", funding).andExpect(status().isForbidden());
  }

  @Test
  void everyDisbursementReportRunsAndExports() {
    Map<String, String> params =
        Map.of(
            "companyId", fx.company().toString(),
            "from", LocalDate.now().minusMonths(1).toString(),
            "to", LocalDate.now().toString());
    List<String> codes =
        List.of(
            "DSB-MASTERLIST",
            "DSB-UNRELEASED-CHECKS",
            "DSB-CWT-COMMISSION",
            "DSB-ATD",
            "DSB-ML-STALE",
            "DSB-CASH-FLOW",
            "DSB-PAYEE",
            "DSB-PAYEE-NOMATCH",
            "DSB-UPLOAD-FALLOUT",
            "DSB-EOD-REMIT",
            "DSB-EOD-REFUND",
            "DSB-EOD-SUMMARY",
            "DSB-EOD-SUPPLIER",
            "DSB-EOD-EMPLOYEE",
            "DSB-EOD-OTHER",
            "DSB-UNREGULARIZED");
    assertThat(as.run("disbtl", () -> reports.catalogue().stream().map(m -> m.code()).toList()))
        .containsAll(codes);
    for (String code : codes) {
      for (ExportFormat format : List.of(ExportFormat.PDF, ExportFormat.XLSX, ExportFormat.CSV)) {
        assertThat(as.run("disbtl", () -> reports.export(code, params, format)).content())
            .as(code + " " + format)
            .isNotEmpty();
      }
    }
  }
}
