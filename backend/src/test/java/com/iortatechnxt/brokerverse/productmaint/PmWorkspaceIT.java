package com.iortatechnxt.brokerverse.productmaint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The Product Maintenance workspace of BDOI's FRS: the dashboard KPIs with their filters,
 * drill-down and export (FRPM.001.01), the Product Matrix tabs with BDOI's columns and the export
 * (FRPM.002.02, FRPM.003.01, FRPM.004.01) and the Quotation Request List with BDOI's status names
 * (FRPM.005.01).
 */
@IntegrationTest
class PmWorkspaceIT {

  private static final String BASE = "/api/v1/product-maintenance";
  private static final Set<String> QUOTATION_STATUSES =
      Set.of(
          "Draft",
          "Pending Marketing Approval",
          "For TSU Review",
          "For Quotation Slip Creation",
          "Rejected",
          "Returned for Revision",
          "QS for Approval",
          "QS Returned",
          "QS Rejected",
          "Under Negotiation",
          "Terms Agreed",
          "Proposal Ready",
          "Proposal Returned",
          "Proposal Released",
          "Proposal Rejected",
          "For Deployment",
          "Deployed");

  @Autowired private Api api;
  @Autowired private TestData data;
  @Autowired private JdbcTemplate jdbc;

  private String company() {
    return data.company().getId().toString();
  }

  private static LocalDate today() {
    return BusinessClock.today(Clock.systemUTC());
  }

  @Test
  void aPackageEndingIn45DaysIsInTheExpiringTabWith45DaysLeft() throws Exception {
    LocalDate end = today().plusDays(45);
    jdbc.update(
        "update cat_product_version set package_end_date = ? where product_code = 'PAR25'"
            + " and version_no = 1",
        Date.valueOf(end));
    JsonNode page =
        api.read(
            api.doGet("tsuhead", BASE + "/matrix?tab=EXPIRING&size=200")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noticeDays").value(90)));
    JsonNode par25 = find(page.get("page").get("content"), "productCode", "PAR25");
    assertThat(par25.get("daysLeft").asLong()).isEqualTo(45);
    assertThat(par25.get("status").asText()).isEqualTo("Expiring");
    assertThat(par25.get("expiryDate").asText()).isEqualTo(end.toString());
    assertThat(par25.get("lineOfInsurance").asText()).isNotBlank();
  }

  @Test
  void theProductMatrixShowsBdoisColumnsAndIsSearchableSortableAndExported() throws Exception {
    JsonNode page =
        api.read(
            api.doGet("ao", BASE + "/matrix?tab=ACTIVE&text=mtr&sort=name&direction=desc&size=200")
                .andExpect(status().isOk()));
    JsonNode rows = page.get("page").get("content");
    assertThat(rows.size()).isPositive();
    for (JsonNode r : rows) {
      assertThat(r.get("productCode").asText().toLowerCase(java.util.Locale.ROOT)).contains("mtr");
      for (String field :
          List.of("lineOfInsurance", "packageName", "status", "productType", "lastUpdatedBy")) {
        assertThat(r.hasNonNull(field)).as(field).isTrue();
      }
    }
    List<String> names = rows.findValuesAsText("packageName");
    assertThat(names).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER.reversed());
    // The expired products need the archive permission (Marketing does not hold it).
    api.doGet("ao", BASE + "/matrix?tab=EXPIRED").andExpect(status().isForbidden());
    api.doGet("tsuhead", BASE + "/matrix?tab=EXPIRED").andExpect(status().isOk());
    String date = today().format(DateTimeFormatter.ofPattern("MMddyyyy"));
    String csv =
        api.doPost(
                "tsuhead",
                "/api/v1/reports/PM-MATRIX/export?format=CSV",
                Map.of("companyId", company(), "tab", "ACTIVE", "packageType", "PACKAGE"))
            .andExpect(status().isOk())
            .andExpect(
                header()
                    .string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        Matchers.containsString("Product Matrix_" + date + ".csv")))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    assertThat(csv)
        .contains(
            "Line of Insurance",
            "Sub-Line",
            "Package Name",
            "Package Description",
            "Insurer",
            "Effective Date",
            "Expiry Date",
            "Last Updated By",
            "Last Updated Date");
  }

  @Test
  void theDashboardCountsTheKpisForTheFiltersAndDrillsDownOldestFirst() throws Exception {
    String from = today().minusYears(1).toString();
    String filters = "?companyId=" + company() + "&from=" + from + "&to=" + today();
    JsonNode view =
        api.read(
            api.doGet("tsuhead", BASE + "/dashboard" + filters)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.length()").value(6))
                .andExpect(jsonPath("$.kpis[0].label").value("Incoming Requests"))
                .andExpect(jsonPath("$.kpis[4].label").value("Issued Proposals")));
    JsonNode counts = view.get("counts");
    assertThat(counts.get("incoming").asLong()).isPositive();
    assertThat(counts.get("expiring").asLong()).isPositive();
    JsonNode drill =
        api.read(
            api.doGet("tsuhead", BASE + "/dashboard/rows" + filters + "&kpi=INCOMING&size=200")
                .andExpect(status().isOk()));
    assertThat(drill.get("totalElements").asLong()).isEqualTo(counts.get("incoming").asLong());
    JsonNode first = drill.get("content").get(0);
    for (String field : List.of("requestNo", "requestType", "status", "submittedAt")) {
      assertThat(first.hasNonNull(field)).as(field).isTrue();
    }
    List<Long> aging =
        drill.get("content").findValues("agingDays").stream()
            .filter(JsonNode::isNumber)
            .map(JsonNode::asLong)
            .toList();
    assertThat(aging).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    // A package line narrows every KPI; non-package quotation requests are left out.
    JsonNode packages =
        api.read(
            api.doGet("tsuhead", BASE + "/dashboard" + filters + "&packaged=true")
                .andExpect(status().isOk()));
    assertThat(packages.get("counts").get("incoming").asLong())
        .isLessThanOrEqualTo(counts.get("incoming").asLong());
    api.doGet(
            "tsuhead",
            BASE
                + "/dashboard?companyId="
                + company()
                + "&from="
                + today()
                + "&to="
                + today().minusDays(1))
        .andExpect(status().isUnprocessableEntity());
    String csv =
        api.doPost(
                "tsuhead",
                "/api/v1/reports/PM-DASHBOARD/export?format=CSV",
                Map.of("companyId", company(), "from", from, "to", today().toString()))
            .andExpect(status().isOk())
            .andExpect(
                header()
                    .string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        Matchers.containsString("Product Maintenance Dashboard_")))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    assertThat(csv)
        .contains(
            "Request Number",
            "Request Type",
            "Product Line",
            "Requested By",
            "Assigned TSU Officer",
            "Current Status",
            "Submission Date",
            "Aging");
    api.doGet("tsuhead", BASE + "/dashboard/officers")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  void marketingSeesOnlyTheRequestsItRaisedOrHolds() throws Exception {
    String filters =
        "?companyId=" + company() + "&from=" + today().minusYears(2) + "&to=" + today();
    JsonNode all =
        api.read(api.doGet("tsuhead", BASE + "/dashboard" + filters).andExpect(status().isOk()));
    JsonNode own =
        api.read(api.doGet("ao", BASE + "/dashboard" + filters).andExpect(status().isOk()));
    assertThat(own.get("counts").get("incoming").asLong())
        .isLessThanOrEqualTo(all.get("counts").get("incoming").asLong());
  }

  @Test
  void theQuotationRequestListShowsBdoisColumnsAndStatusNames() throws Exception {
    JsonNode page =
        api.read(
            api.doGet("tsu", BASE + "/quotation-requests?companyId=" + company() + "&size=200")
                .andExpect(status().isOk()));
    JsonNode rows = page.get("content");
    assertThat(rows.size()).isPositive();
    for (JsonNode r : rows) {
      assertThat(QUOTATION_STATUSES).contains(r.get("status").asText());
      assertThat(r.get("businessType").asText()).isIn("New Business", "Renewal");
      assertThat(r.hasNonNull("requestNo")).isTrue();
      assertThat(r.hasNonNull("assuredName")).isTrue();
      assertThat(r.hasNonNull("requestDate")).isTrue();
    }
    api.doGet(
            "tsu",
            BASE
                + "/quotation-requests?companyId="
                + company()
                + "&open=true&sort=assured&direction=asc")
        .andExpect(status().isOk());
  }

  @Test
  void aReturnedQuotationRequestShowsReturnedForRevisionAndItsRemarksInTheAuditLogs()
      throws Exception {
    Map<String, Object> found =
        jdbc.queryForMap(
            "select c.id, c.reference, p.client_name from wf_case c join npk_proposal p"
                + " on c.entity_type = 'ProposalRequest' and c.entity_id = cast(p.id as varchar)"
                + " where c.stage_code = 'WITH_TSU' and not c.closed order by c.id limit 1");
    String reason =
        jdbc.queryForObject(
            "select code from lov_value where type_code = 'RETURN_REASON' order by sort_order"
                + " limit 1",
            String.class);
    String reference = (String) found.get("reference");
    api.doPost(
            "tsu",
            "/api/v1/workflow/cases/" + found.get("id") + "/actions/return",
            Map.of("reasonCode", reason, "comment", "Add the loss history of three years"))
        .andExpect(status().isOk());
    JsonNode list =
        api.read(
            api.doGet(
                    "tsu",
                    BASE + "/quotation-requests?companyId=" + company() + "&text=" + reference)
                .andExpect(status().isOk()));
    assertThat(list.get("content").get(0).get("status").asText())
        .isEqualTo("Returned for Revision");
    JsonNode audit =
        api.read(
            api.doGet(
                    "tsuhead",
                    BASE
                        + "/audit-logs?from="
                        + today()
                        + "&to="
                        + today()
                        + "&entityId="
                        + reference)
                .andExpect(status().isOk()));
    assertThat(audit.get("content").findValuesAsText("remarks"))
        .contains("Add the loss history of three years");
    assertThat(audit.get("content").findValuesAsText("subject"))
        .contains((String) found.get("client_name"));
    String date = today().format(DateTimeFormatter.ofPattern("MMddyyyy"));
    String csv =
        api.doPost(
                "tsuhead",
                "/api/v1/reports/PM-AUDIT/export?format=CSV",
                Map.of(
                    "fromDate",
                    today().toString(),
                    "toDate",
                    today().toString(),
                    "reference",
                    reference))
            .andExpect(status().isOk())
            .andExpect(
                header()
                    .string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        Matchers.containsString("Audit Logs_" + date + ".csv")))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    assertThat(csv)
        .contains(
            "Client/Assured's Name",
            "Remarks",
            "Old Value",
            "New Value",
            "Reference Number",
            "Add the loss history of three years");
  }

  private static JsonNode find(JsonNode rows, String field, String value) {
    for (JsonNode r : rows) {
      if (value.equals(r.get(field).asText())) {
        return r;
      }
    }
    throw new AssertionError(value + " not found in " + rows);
  }
}
