package com.iortatechnxt.brokerverse.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.brokerclaims.ClaimsFixtures;
import com.iortatechnxt.brokerverse.cashiering.CashFixtures;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.remittance.RemittanceFixtures;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Claims Handling (BRD-7) end to end through the HTTP API with the SIT/UAT users (wave CL2, design
 * section 14): an account is booked; a claim recorded on its unpaid cover is blocked and raises the
 * unpaid-premium alert (FR-CM-011, FR-CM-016); cashiering applies the payment, the premium check
 * turns PAID and the authorization code is generated (FR-CM-016); the Team Lead sets "With BDOI -
 * For Premium Remittance"; the special remittance request is confirmed through the claims feed,
 * approved, batched and paid by the Disbursement DV, and the handler is told the premium is FULLY
 * REMITTED; the status moves through the role / unit matrix, a temporary closure is resumed, the
 * claim is settled (LOA issued) and closed, reopened with a reason and settled again; the
 * outstanding, settled, ageing, loss ratio and activity log reports follow each step
 * (FR-CM-041-046, FR-CM-052, FR-CM-060-066).
 */
@IntegrationTest
class ClaimsEndToEndApiIT {

  private static final String BASE = "/api/v1/broker-claims";
  private static final String REMITTANCE = "/api/v1/remittance";
  private static final String OFFICER = "clmofficer";
  private static final String TL = "clmtl";
  private static final String TH = "clmth";
  private static final String UH = "clmuh";
  private static final String DETAIL = "DETAIL";

  @Autowired private Api api;
  @Autowired private BookingFixtures booking;
  @Autowired private CashFixtures cash;
  @Autowired private InvoiceLedgerQueryService ledger;
  @Autowired private DisbursementQueueService queue;
  @Autowired private JdbcTemplate jdbc;

  private String company;

  private String c() {
    return "?companyId=" + company;
  }

  @Test
  void aClaimRunsFromTheUnpaidCoverToSettlementReopenAndTheReports() throws Exception {
    company = cash.company().toString();
    String invoiceNo = bookAnAccount();
    OpsInvoice invoice = ledger.require(invoiceNo);
    String arn = invoice.getArn();

    long id = recordOnTheUnpaidCover(arn);
    String claim = BASE + "/" + id;

    payThroughCashiering(invoice);
    api.doGet(OFFICER, claim + c())
        .andExpect(jsonPath("$.premium.recordedStatus").value("PAID"))
        .andExpect(jsonPath("$.flags.unpaidPremium").value(false));
    api.doPost(OFFICER, claim + "/premium-check" + c(), Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAID"));
    JsonNode authorized =
        api.read(
            api.doPost(OFFICER, claim + "/authorize" + c(), Map.of()).andExpect(status().isOk()));
    assertThat(authorized.get("premium").get("authorizationCode").asText()).startsWith("CAC-");
    api.doPost(OFFICER, claim + "/authorize" + c(), Map.of())
        .andExpect(status().is4xxClientError());

    api.doPost(
            TL,
            claim + "/status" + c(),
            Map.of("statusCode", "BDOI_PREMIUM_REMITTANCE", "remark", "Premium to insurer"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.phase").value("IN_PROGRESS"));
    api.doGet(OFFICER, claim + c())
        .andExpect(jsonPath("$.flags.awaitingPremiumRemittance").value(true))
        .andExpect(jsonPath("$.premium.unremittedInvoices[0]").value(invoiceNo));

    remitThroughASpecialRemittance(id, invoiceNo);

    workTheStatusesThroughTheMatrix(claim);
    settleReopenAndReport(id, arn);
  }

  /** Books a motor account issued on a cover running today (BRNB booking, HTTP as Processing). */
  private String bookAnAccount() throws Exception {
    LocalDate from = ClaimsFixtures.coverFrom();
    Account account =
        booking.issued(
            new BookingFixtures.Spec(
                "MTR10", "CBG", PaymentArrangement.VIA_BDOI, from, from.plusYears(1), 1));
    JsonNode booked =
        api.read(
            api.doPost(
                    "proc",
                    "/api/v1/booking/book",
                    Map.of(
                        "arn",
                        account.getArn(),
                        "bookingDate",
                        BookingFixtures.BOOKED_ON.toString()))
                .andExpect(status().isCreated()));
    return booked.get("invoiceNo").asText();
  }

  /** FR-CM-011/016: the claim is recorded, flagged unpaid, alerted and cannot be authorised. */
  private long recordOnTheUnpaidCover(String arn) throws Exception {
    LocalDate today = LocalDate.now();
    Map<String, Object> loss = new HashMap<>();
    loss.put("lossDate", today.minusDays(3).toString());
    loss.put("reportedDate", today.minusDays(2).toString());
    loss.put("lossNature", "MOTOR_OWN_DAMAGE");
    loss.put("claimType", "MOTOR_OWN_DAMAGE");
    loss.put("lossDescription", "Rear-ended on EDSA");
    loss.put("lossPlace", "EDSA Guadalupe");
    loss.put("claimAmount", 120000);
    loss.put("deductible", 2000);
    loss.put("initialReserve", 118000);
    Map<String, Object> body = new HashMap<>();
    body.put("companyId", Long.valueOf(company));
    body.put("arn", arn);
    body.put("policyYear", 1);
    body.put("source", "BDOI_NOTICE");
    body.put("loss", loss);
    api.doPost("ao", BASE, body).andExpect(status().isForbidden());
    JsonNode claim = api.read(api.doPost(OFFICER, BASE, body).andExpect(status().isCreated()));
    long id = claim.get("id").asLong();
    assertThat(claim.get("flags").get("unpaidPremium").asBoolean()).isTrue();
    assertThat(claim.get("premium").get("canAuthorize").asBoolean()).isFalse();
    assertThat(claim.get("progress").get("statusCode").asText()).isEqualTo("NEW_INCOMPLETE_DOCS");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from alt_alert where dedup_key = ?",
                Long.class,
                "BCL_UNPAID_PREMIUM_CLAIM:" + id))
        .isEqualTo(1L);
    api.doPost(OFFICER, BASE + "/" + id + "/authorize" + c(), Map.of())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("BCL_PREMIUM_UNPAID"));
    return id;
  }

  /**
   * CSHID: an over-the-counter cash payment of the whole premium, applied to the invoice, dated
   * past the check holding period of the remittance (RMTID.017).
   */
  private void payThroughCashiering(OpsInvoice invoice) throws Exception {
    api.doPost(
            "cashier",
            "/api/v1/cashiering/payments",
            Map.of(
                "companyId",
                Long.valueOf(company),
                "branchId",
                cash.ho(),
                "references",
                List.of(invoice.getInvoiceNo()),
                "payorName",
                "Claims E2E Payor",
                "currency",
                "PHP",
                "amount",
                invoice.premiumBalance(),
                "paymentDate",
                RemittanceFixtures.PAID_ON.toString(),
                "mode",
                "CASH"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.payment.matchCategory").value("APPLIED"));
    assertThat(ledger.require(invoice.getInvoiceNo()).getPaymentStatus())
        .isEqualTo(PaymentStatus.PAID);
  }

  /**
   * OQ46 / MKTID.009: the special remittance request is confirmed by the claims feed, approved,
   * batched, approved and paid; the handler is told when the invoice is FULLY REMITTED.
   */
  private void remitThroughASpecialRemittance(long id, String invoiceNo) throws Exception {
    JsonNode request =
        api.read(
            api.doPost(
                    "mktcoll",
                    REMITTANCE + "/special",
                    Map.of(
                        "companyId",
                        Long.valueOf(company),
                        "invoiceNo",
                        invoiceNo,
                        "conditionCode",
                        "CLAIMS",
                        "remarks",
                        "Claim awaiting premium remittance"))
                .andExpect(status().isCreated()));
    assertThat(request.get("validationNote").asText())
        .contains("claim confirmed by the Claims system");
    api.doPost(
            "remittl",
            REMITTANCE + "/special/" + request.get("id").asLong() + "/approve",
            Map.of("comment", "Claims special remittance"))
        .andExpect(jsonPath("$.stage").value("IN_PROCESS_REMITTANCE"));

    JsonNode accounts =
        api.read(api.doGet("remit", REMITTANCE + "/accounts" + c() + "&q=" + invoiceNo));
    long batchId = accounts.get("content").get(0).get("batchId").asLong();
    String batch = REMITTANCE + "/batches/" + batchId;
    api.doPost("remittl", batch + "/assign", Map.of("username", "remit"))
        .andExpect(status().isOk());
    api.doPost("remit", batch + "/submit", Map.of("comment", "Checked"))
        .andExpect(jsonPath("$.summary.stage").value("FOR_APPROVAL"));
    JsonNode approved =
        api.read(
            api.doPost("remittl", batch + "/approve", Map.of("comment", "OK"))
                .andExpect(jsonPath("$.summary.stage").value("APPROVED")));
    String batchNo = approved.get("summary").get("batchNo").asText();
    Long request2 = queue.find("REMITTANCE", batchNo).orElseThrow().getId();
    api.doPost(
            "disb",
            "/api/v1/ops/disbursements/" + request2 + "/dv",
            Map.of("dvNo", "DV-CLM-" + BookingFixtures.token()))
        .andExpect(status().isOk());
    assertThat(ledger.require(invoiceNo).getRemittanceStatus())
        .isEqualTo(RemittanceStatus.FULLY_REMITTED);

    JsonNode notices = api.read(api.doGet(OFFICER, "/api/v1/notifications?size=100"));
    List<String> titles = new ArrayList<>();
    notices
        .get("content")
        .forEach(
            n -> {
              if (("/claims-handling/" + id).equals(n.get("link").asText())) {
                titles.add(n.get("title").asText());
              }
            });
    assertThat(titles).anyMatch(t -> t.endsWith(" remitted")).anyMatch(t -> t.endsWith(" paid"));
    api.doGet(OFFICER, BASE + "/" + id + c())
        .andExpect(jsonPath("$.flags.awaitingPremiumRemittance").value(false));
  }

  /** FR-CM-041/042/045: the matrix refuses the officer, the temporary closure is resumed. */
  private void workTheStatusesThroughTheMatrix(String claim) throws Exception {
    api.doPost(OFFICER, claim + "/status" + c(), Map.of("statusCode", "INSURER_LOA_ISSUANCE"))
        .andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.code").value("BCL_STATUS_NOT_ALLOWED"));
    api.doPost(TL, claim + "/status" + c(), Map.of("statusCode", "NEW_COMPLETE_DOCS"))
        .andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.code").value("BCL_STATUS_BACK_TO_NEW"));
    api.doPost(
            OFFICER,
            claim + "/status" + c(),
            Map.of("statusCode", "TEMP_CLOSED_NO_DOCS", "remark", "No documents yet"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.phase").value("TEMP_CLOSED"));
    api.doPost(
            TL,
            claim + "/status" + c(),
            Map.of("statusCode", "INSURER_LOA_ISSUANCE", "remark", "Documents received"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.phase").value("IN_PROGRESS"));
    api.doPut(OFFICER, claim + "/action-plan" + c(), Map.of("text", "Follow up the LOA"))
        .andExpect(status().isOk());
    api.doPost(
            OFFICER,
            claim + "/diary" + c(),
            Map.of("entryType", "CALL", "text", "Insurer confirms the LOA this week"))
        .andExpect(status().isCreated());
  }

  /** FR-CM-044/045 and the reports: settled (LOA) and closed, reopened, settled again. */
  private void settleReopenAndReport(long id, String arn) throws Exception {
    String claim = BASE + "/" + id;
    String claimNo = api.read(api.doGet(OFFICER, claim + c())).get("claimNo").asText();
    Map<String, Object> settlement =
        Map.of(
            "typeCode",
            "SETTLED_LOA_ISSUED",
            "amount",
            100000,
            "dateSettled",
            LocalDate.now().toString(),
            "remark",
            "LOA issued by the insurer");
    api.doPost(OFFICER, claim + "/settlement" + c(), settlement).andExpect(status().isForbidden());
    api.doPost(TL, claim + "/settlement" + c(), settlement)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.phase").value("CLOSED"));
    assertThat(claimNos("BCL-SETTLED")).contains(claimNo);
    assertThat(claimNos("BCL-OUTSTANDING")).doesNotContain(claimNo);
    api.doPost(OFFICER, claim + "/status" + c(), Map.of("statusCode", "TEMP_CLOSED_NO_DOCS"))
        .andExpect(status().is4xxClientError());

    api.doPost(TL, claim + "/reopen" + c(), Map.of("reasonCode", "ADDITIONAL_LOSS"))
        .andExpect(status().isForbidden());
    api.doPost(
            TH,
            claim + "/reopen" + c(),
            Map.of("reasonCode", "ADDITIONAL_LOSS", "remark", "Additional repair estimate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.phase").value("IN_PROGRESS"));
    assertThat(claimNos("BCL-OUTSTANDING")).contains(claimNo);
    assertThat(claimNos("BCL-AGEING")).contains(claimNo);
    assertThat(claimNos("BCL-SETTLED")).doesNotContain(claimNo);

    api.doPost(
            TL,
            claim + "/settlement" + c(),
            Map.of(
                "typeCode",
                "SETTLED_LOA_ISSUED",
                "amount",
                125000,
                "dateSettled",
                LocalDate.now().toString()))
        .andExpect(jsonPath("$.phase").value("CLOSED"));
    assertThat(claimNos("BCL-SETTLED")).contains(claimNo);

    List<JsonNode> ratio =
        details(run("BCL-LOSS-RATIO", Map.of("grouping", "CLIENT"))).stream()
            .filter(r -> arn.equals(r.get("arn").asText()))
            .toList();
    assertThat(ratio)
        .singleElement()
        .satisfies(r -> assertThat(r.get("losses").asDouble()).isEqualTo(125000.0));

    List<String> activities = new ArrayList<>();
    details(run("BCL-ACTIVITY-LOG", Map.of("claimNo", claimNo)))
        .forEach(r -> activities.add(r.get("activity").asText() + " " + r.get("detail").asText()));
    assertThat(activities)
        .anyMatch(a -> a.contains("With BDOI - For Premium Remittance"))
        .anyMatch(a -> a.contains("(CLOSED) ->"))
        .anyMatch(a -> a.startsWith("Diary: CALL"));

    api.doGet("ao", BASE + "/experience?arn=" + arn)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.claimCount").value(1))
        .andExpect(jsonPath("$.claims[0].claimNo").value(claimNo));
    api.doGet("ao", claim + c()).andExpect(status().isForbidden());
  }

  private JsonNode run(String code, Map<String, String> extra) throws Exception {
    Map<String, String> params = new HashMap<>(extra);
    params.put("companyId", company);
    params.put("handler", OFFICER);
    params.put("periodFrom", LocalDate.now().minusDays(1).toString());
    params.put("periodTo", LocalDate.now().toString());
    params.put("settledFrom", LocalDate.now().minusDays(1).toString());
    params.put("settledTo", LocalDate.now().toString());
    return api.read(
        api.doPost(UH, "/api/v1/reports/" + code + "/run", params).andExpect(status().isOk()));
  }

  private static List<JsonNode> details(JsonNode result) {
    List<JsonNode> rows = new ArrayList<>();
    result
        .get("rows")
        .forEach(
            r -> {
              if (DETAIL.equals(r.get("kind").asText())) {
                rows.add(r.get("cells"));
              }
            });
    return rows;
  }

  private List<String> claimNos(String code) throws Exception {
    return details(run(code, Map.of())).stream().map(r -> r.path("claim_no").asText()).toList();
  }
}
