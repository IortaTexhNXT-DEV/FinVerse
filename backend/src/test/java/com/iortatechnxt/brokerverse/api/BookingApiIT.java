package com.iortatechnxt.brokerverse.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** Booking endpoints through the full HTTP stack. */
@IntegrationTest
class BookingApiIT {

  @Autowired private Api api;
  @Autowired private BookingFixtures fx;

  private String c() {
    return fx.company().toString();
  }

  @ParameterizedTest
  @CsvSource({
    "proc, /api/v1/booking/workbench/counts?companyId={c}",
    "proc, /api/v1/booking/workbench?companyId={c}",
    "proc, /api/v1/booking/workbench?companyId={c}&tab=QUEUED&q=ARN",
    "proc, /api/v1/booking/workbench?companyId={c}&tab=BOOKED&page=0&size=5",
    "adjust, /api/v1/booking/workbench?companyId={c}&tab=FAILED",
    "proc, /api/v1/booking/workbench?companyId={c}&tab=READY&line=MTR",
    "proc, /api/v1/booking/workbench?companyId={c}&tab=BOOKED&line=MTR",
    "proc, /api/v1/booking/invoices?companyId={c}",
    "adjust, /api/v1/booking/invoices?companyId={c}&q=ARN&status=BOOKED&kind=BOOKING&insurer=INS-MGIC"
        + "&from=2026-01-01&to=2026-12-31&line=MTR",
    "proc, /api/v1/booking/batch-runs?companyId={c}",
    "adjust, /api/v1/booking/endorsements?companyId={c}",
    "proc, /api/v1/booking/service-invoices?companyId={c}",
    "proc, /api/v1/booking/service-invoices?companyId={c}&q=SI&kind=INVOICE",
    "proc, /api/v1/booking/setup/auto-book-rules?companyId={c}",
    "badmin, /api/v1/booking/setup/incentive-rules?companyId={c}",
    "badmin, /api/v1/booking/setup/service-invoice-types",
    "proc, /api/v1/booking/accounts/ARN-2026-940005/invoices",
    "proc, /api/v1/booking/accounts/ARN-2026-940005/endorsements",
  })
  void readEndpointsRespondOk(String username, String url) throws Exception {
    api.doGet(username, url.replace("{c}", c())).andExpect(status().isOk());
  }

  @Test
  void usersWithoutBookingRightsAreRefused() throws Exception {
    api.doGet("ao", "/api/v1/booking/workbench/counts?companyId=" + c())
        .andExpect(status().isForbidden());
    api.doPost(
            "ao",
            "/api/v1/booking/setup/auto-book-rules?companyId=" + c(),
            Map.of("enabled", false, "description", "Not allowed"))
        .andExpect(status().isForbidden());
  }

  @Test
  void anAccountIsPreviewedBookedAndReadBack() throws Exception {
    Account account = fx.motor();
    Map<String, Object> request =
        Map.of("arn", account.getArn(), "bookingDate", BookingFixtures.BOOKED_ON.toString());
    api.doPost("proc", "/api/v1/booking/preview", request)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.invoices[0].arn").value(account.getArn()))
        .andExpect(jsonPath("$.journal").isNotEmpty());
    JsonNode booked =
        api.read(
            api.doPost("proc", "/api/v1/booking/book", request).andExpect(status().isCreated()));
    long id = booked.get("id").asLong();
    String invoiceNo = booked.get("invoiceNo").asText();

    api.doGet("proc", "/api/v1/booking/invoices/" + id)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.invoiceNo").value(invoiceNo))
        .andExpect(jsonPath("$.shares[0].insurerCode").value("INS-MGIC"))
        .andExpect(jsonPath("$.facts.costCenter").value("NB-CBG-M"));
    api.doGet("proc", "/api/v1/booking/invoices/by-no/" + invoiceNo).andExpect(status().isOk());
    api.doGet("proc", "/api/v1/booking/invoices/by-no/" + invoiceNo + "/event")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.kind").value("BOOKING"));
    api.doGet("proc", "/api/v1/booking/invoices/" + id + "/open-items")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));
    api.doGet("proc", "/api/v1/booking/invoices/" + id + "/journal")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].batchNo").isNotEmpty());
    api.doGet("proc", "/api/v1/booking/accounts/" + account.getArn() + "/invoices")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].invoiceNo").value(invoiceNo));
    JsonNode sis =
        api.read(
            api.doGet("proc", "/api/v1/booking/service-invoices/by-invoice/" + invoiceNo)
                .andExpect(status().isOk()));
    long siId = sis.get(0).get("id").asLong();
    api.doGet("proc", "/api/v1/booking/service-invoices/" + siId).andExpect(status().isOk());
    api.doGet("proc", "/api/v1/booking/service-invoices/" + siId + "/pdf")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    api.doPost("proc", "/api/v1/booking/service-invoices/" + siId + "/resend", null)
        .andExpect(status().isOk());
    api.doPost(
            "adjust",
            "/api/v1/booking/service-invoices/" + siId + "/credit",
            Map.of("commission", 1, "vatOnCommission", 0, "reason", "Rebate"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.kind").value("CREDIT"));

    Map<String, Object> endorsement =
        Map.of(
            "arn", account.getArn(),
            "type", "POSITIVE",
            "effectiveDate", "2027-04-01",
            "basis", "PRO_RATA",
            "sumInsuredChange", 100000,
            "description", "Accessories",
            "bookingDate", "2026-09-20");
    api.doPost("proc", "/api/v1/booking/endorsements/preview", endorsement)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.invoice.kind").value("ENDORSEMENT_PLUS"));
    api.doPost("adjust", "/api/v1/booking/endorsements", endorsement)
        .andExpect(status().isForbidden());
    api.doPost("proc", "/api/v1/booking/endorsements", endorsement)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.endorsementNo").isNotEmpty());
  }

  @Test
  void theQueueAndBatchAreDrivenOverHttp() throws Exception {
    Account a = fx.motor();
    Account b = fx.motor();
    api.doPost(
            "proc",
            "/api/v1/booking/queue",
            Map.of("companyId", fx.company(), "arns", List.of(a.getArn(), b.getArn())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].queued").value(true));
    JsonNode queued =
        api.read(
            api.doGet(
                    "proc",
                    "/api/v1/booking/workbench?tab=QUEUED&companyId=" + c() + "&q=" + a.getArn())
                .andExpect(status().isOk()));
    long entryA = queued.get("content").get(0).get("id").asLong();
    JsonNode queuedB =
        api.read(
            api.doGet(
                "proc",
                "/api/v1/booking/workbench?tab=QUEUED&companyId=" + c() + "&q=" + b.getArn()));
    long entryB = queuedB.get("content").get(0).get("id").asLong();
    api.doPut(
            "proc",
            "/api/v1/booking/queue/" + entryA,
            Map.of("bookingDate", BookingFixtures.BOOKED_ON.toString(), "costCenter", "MKT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.costCenter").value("MKT"));
    api.doPost("proc", "/api/v1/booking/queue/" + entryB + "/remove", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REMOVED"));
    JsonNode run =
        api.read(
            api.doPost(
                    "proc",
                    "/api/v1/booking/batch/confirm",
                    Map.of(
                        "companyId", fx.company(),
                        "entryIds", List.of(entryA),
                        "businessDate", BookingFixtures.BOOKED_ON.toString()))
                .andExpect(status().isOk()));
    assertThat(run.get("bookedCount").asInt()).isEqualTo(1);
    api.doGet("proc", "/api/v1/booking/batch-runs/" + run.get("runNo").asText())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rows[0].arn").value(a.getArn()));
    api.doPost(
            "proc",
            "/api/v1/booking/book-now",
            Map.of(
                "companyId", fx.company(),
                "arns", List.of(b.getArn()),
                "bookingDate", BookingFixtures.BOOKED_ON.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookedCount").value(1));
    api.doPost("proc", "/api/v1/booking/batch/cancel?companyId=" + c(), null)
        .andExpect(status().isOk());
  }

  @Test
  void theSetupIsMaintainedByTheBusinessAdministrator() throws Exception {
    JsonNode rule =
        api.read(
            api.doPost(
                    "badmin",
                    "/api/v1/booking/setup/auto-book-rules?companyId=" + c(),
                    Map.of(
                        "productCode", "MTR38",
                        "marketSegment", "RETAIL",
                        "enabled", false,
                        "description", "API test rule"))
                .andExpect(status().isCreated()));
    api.doPut(
            "badmin",
            "/api/v1/booking/setup/auto-book-rules/" + rule.get("id").asLong(),
            Map.of("productCode", "MTR38", "enabled", false, "description", "API test rule (off)"))
        .andExpect(status().isOk());
    JsonNode incentive =
        api.read(
            api.doPost(
                    "badmin",
                    "/api/v1/booking/setup/incentive-rules?companyId=" + c(),
                    Map.of(
                        "productCode", "MTR38",
                        "periodFrom", "2030-01-01",
                        "active", false,
                        "description", "API test incentive"))
                .andExpect(status().isCreated()));
    api.doPut(
            "badmin",
            "/api/v1/booking/setup/incentive-rules/" + incentive.get("id").asLong(),
            Map.of(
                "periodFrom", "2030-01-01",
                "periodTo", "2029-01-01",
                "active", false,
                "description", "Invalid period"))
        .andExpect(status().isUnprocessableEntity());
    String code = "API_" + BookingFixtures.token();
    JsonNode type =
        api.read(
            api.doPost(
                    "badmin",
                    "/api/v1/booking/setup/service-invoice-types",
                    Map.of(
                        "code", code,
                        "name", "API type",
                        "recipient", "INTERNAL",
                        "trigger", "MANUAL",
                        "templateCode", "SERVICE_INVOICE_NOTE",
                        "active", true))
                .andExpect(status().isCreated()));
    api.doPut(
            "badmin",
            "/api/v1/booking/setup/service-invoice-types/" + type.get("id").asLong(),
            Map.of(
                "code", code,
                "name", "API type renamed",
                "recipient", "INTERNAL",
                "trigger", "MANUAL",
                "templateCode", "SERVICE_INVOICE_NOTE",
                "active", true))
        .andExpect(status().isOk());
    api.doPost(
            "proc",
            "/api/v1/booking/service-invoices",
            Map.of(
                "companyId",
                fx.company(),
                "typeCode",
                code,
                "recipientCode",
                "NB-CORP",
                "currency",
                "PHP",
                "commission",
                500,
                "vatOnCommission",
                60,
                "wtaxAmount",
                50))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.netAmount").value(510.0));
  }
}
