package com.iortatechnxt.brokerverse.productmaint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionQueryService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.productmaint.service.PackageDateNotices;
import com.iortatechnxt.brokerverse.productmaint.service.PackageDeactivationJob;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Package deactivation requests of BDOI's FRS (FRPM.003.04 to FRPM.003.07, FRPM.017.01): the
 * request with effective date, reason and approver, its checks, the notice to the approver, the
 * approval setting the expiry date to the later of the effective date and the approval date, the
 * rejection with mandatory remarks keeping the package active, the reassignment notices, the daily
 * deactivation and the audit logs of the record.
 */
@IntegrationTest
class PackageDeactivationIT {

  private static final String BASE = "/api/v1/product-maintenance/deactivations";

  @Autowired private Api api;
  @Autowired private TestData data;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private PackageDeactivationJob job;
  @Autowired private PackageDateNotices dateNotices;
  @Autowired private ProductVersionQueryService versions;

  private static LocalDate today() {
    return BusinessClock.today(Clock.systemUTC());
  }

  private Map<String, Object> body(String product, LocalDate effective, String approver) {
    Map<String, Object> body = new HashMap<>();
    body.put("companyId", data.company().getId());
    body.put("productCode", product);
    body.put("effectiveDate", effective.toString());
    body.put("reason", "LOSS_EXPERIENCE");
    body.put("remarks", "Loss ratio above the agreed level");
    body.put("approver", approver);
    return body;
  }

  private JsonNode submit(String product, LocalDate effective) throws Exception {
    return api.read(
        api.doPost("mbs", BASE, body(product, effective, "tsuhead"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.statusLabel").value("Pending Approval")));
  }

  /** A package of this test (a copy of a seed motor package), with or without version 1. */
  private String pkg(String code, boolean versioned) {
    jdbc.update(
        "insert into cat_product (code, name, line_code, cover_type_code, packaged, fleet_capable,"
            + " market_segments, mortgage_applicable, direct_payment_eligible, multi_year_allowed,"
            + " max_term_years, ffy_eligible, payment_gate, default_rate, default_commission_rate,"
            + " minimum_premium, record_status, authorized_by, authorized_at, created_at,"
            + " created_by) select ?, 'Deactivation package ' || ?, line_code, cover_type_code,"
            + " ?, fleet_capable, market_segments, mortgage_applicable, direct_payment_eligible,"
            + " multi_year_allowed, max_term_years, ffy_eligible, payment_gate, default_rate,"
            + " default_commission_rate, minimum_premium, 'ACTIVE', 'SYSTEM', now(), now(),"
            + " 'SYSTEM' from cat_product where code = 'MTR35'",
        code,
        code,
        versioned);
    if (versioned) {
      jdbc.update(
          "insert into cat_product_version (product_code, version_no, status, effective_from,"
              + " package_start_date, package_end_date, default_rate, minimum_premium,"
              + " default_commission_rate, manual_rate_allowed, change_summary, created_at,"
              + " created_by) values (?, 1, 'RELEASED', date '2020-01-01', date '2020-01-01',"
              + " ?, 1.3, 5000, 15, true, 'Deactivation test', now(), 'SYSTEM')",
          code,
          Date.valueOf(today().plusYears(1)));
    }
    return code;
  }

  private LocalDate endDate(String product) {
    Date d =
        jdbc.queryForObject(
            "select package_end_date from cat_product_version where product_code = ?"
                + " and version_no = 1",
            Date.class,
            product);
    return d == null ? null : d.toLocalDate();
  }

  @Test
  void anApprovedRequestSetsTheExpiryDateToTheEffectiveDateAndTellsBothUsers() throws Exception {
    api.doGet("mbs", BASE + "/settings")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.route").value("EFFECTIVE_DATED"))
        .andExpect(jsonPath("$.approvers").value(org.hamcrest.Matchers.hasItem("tsuhead")))
        .andExpect(
            jsonPath("$.approvers")
                .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("mbs"))));
    LocalDate effective = today().plusDays(10);
    String product = pkg("TDA01", true);
    JsonNode created = submit(product, effective);
    long id = created.get("id").asLong();
    String requestNo = created.get("requestNo").asText();
    assertThat(requestNo).startsWith("PKD-");
    assertThat(notices("tsuhead", "Package deactivation for approval: " + requestNo)).isOne();
    // One pending request per package; the effective date is today or later.
    api.doPost("mbs", BASE, body(product, effective, "tsuhead"))
        .andExpect(status().isUnprocessableEntity());
    api.doPost("mbs", BASE, body(pkg("TDB01", true), today().minusDays(1), "tsuhead"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.detail").value("The deactivation effective date must be today or later"));
    // Only the selected approver decides.
    api.doPost("mbs", BASE + "/" + id + "/approve", Map.of()).andExpect(status().isForbidden());
    api.doPost("tsuhead", BASE + "/" + id + "/approve", Map.of("remarks", "Agreed"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"))
        .andExpect(jsonPath("$.expiryDate").value(effective.toString()))
        .andExpect(jsonPath("$.decidedBy").value("tsuhead"));
    assertThat(endDate(product)).isEqualTo(effective);
    assertThat(notices("mbs", "Package deactivation approved: " + requestNo)).isOne();
    api.doGet("tsuhead", BASE + "?companyId=" + data.company().getId() + "&status=APPROVED")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].requestNo").value(requestNo))
        .andExpect(jsonPath("$.content[0].packageName").isNotEmpty());
    JsonNode audit =
        api.read(
            api.doGet(
                    "tsuhead",
                    "/api/v1/product-maintenance/audit-logs?from="
                        + today()
                        + "&to="
                        + today()
                        + "&entityId="
                        + requestNo)
                .andExpect(status().isOk()));
    assertThat(audit.get("content").findValuesAsText("subject"))
        .contains("Deactivation package " + product);
    // The approval keeps the expiry date before and after (Old Value and New Value).
    assertThat(audit.get("content").findValuesAsText("newValue")).contains(effective.toString());
  }

  @Test
  void aLateApprovalSetsTheExpiryDateToTheApprovalDate() throws Exception {
    String product = pkg("TDC01", true);
    JsonNode created = submit(product, today());
    long id = created.get("id").asLong();
    jdbc.update(
        "update pm_deactivation_request set effective_date = ? where id = ?",
        Date.valueOf(today().minusDays(3)),
        id);
    api.doPost("tsuhead", BASE + "/" + id + "/approve", Map.of())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.expiryDate").value(today().toString()));
    assertThat(endDate(product)).isEqualTo(today());
  }

  @Test
  void aRejectedRequestShowsTheRemarksAndThePackageStaysActive() throws Exception {
    String product = pkg("TDD01", true);
    LocalDate before = endDate(product);
    JsonNode created = submit(product, today().plusDays(5));
    long id = created.get("id").asLong();
    api.doPost("tsuhead", BASE + "/" + id + "/reject", Map.of("remarks", " "))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.detail").value("Enter the approval remarks of the rejection"));
    api.doPost(
            "tsuhead", BASE + "/" + id + "/reject", Map.of("remarks", "Insurer renewed the terms"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"))
        .andExpect(jsonPath("$.decisionRemarks").value("Insurer renewed the terms"));
    assertThat(endDate(product)).isEqualTo(before);
    assertThat(
            jdbc.queryForObject(
                "select lifecycle_status from cat_product where code = ?", String.class, product))
        .isEqualTo("ACTIVE");
    api.doGet("mbs", BASE + "/" + id)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decisionRemarks").value("Insurer renewed the terms"));
  }

  @Test
  void aReassignedRequestTellsTheNewAndTheOriginalApprover() throws Exception {
    Long tsulead =
        jdbc.queryForObject("select id from sec_user where username = 'tsulead'", Long.class);
    Long head = jdbc.queryForObject("select id from sec_role where code = 'TSU_HEAD'", Long.class);
    jdbc.update("insert into sec_user_role (user_id, role_id) values (?, ?)", tsulead, head);
    try {
      String product = pkg("TDE01", false);
      JsonNode created = submit(product, today());
      long id = created.get("id").asLong();
      String requestNo = created.get("requestNo").asText();
      api.doPost("tsuhead", BASE + "/" + id + "/reassign", Map.of("approver", "tsulead"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.approver").value("tsulead"));
      assertThat(notices("tsulead", "Package deactivation for approval: " + requestNo)).isOne();
      assertThat(notices("tsuhead", "Package deactivation reassigned: " + requestNo)).isOne();
      api.doPost("tsulead", BASE + "/" + id + "/approve", Map.of())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.expiryDate").value(today().toString()));
      // A product without package versions is deactivated by the daily step after its expiry.
      job.execute(today().plusDays(1));
      assertThat(
              jdbc.queryForObject(
                  "select lifecycle_status from cat_product where code = ?", String.class, product))
          .isEqualTo("RETIRED");
    } finally {
      jdbc.update("delete from sec_user_role where user_id = ? and role_id = ?", tsulead, head);
    }
  }

  @Test
  void aPackageReachingItsAnniversaryWithin90DaysAlertsTsuOncePerAnniversary() throws Exception {
    String product = pkg("TDF01", true);
    jdbc.update(
        "update cat_product_version set anniversary_date = ? where product_code = ?",
        Date.valueOf(today().plusDays(80)),
        product);
    Long company = data.company().getId();
    assertThat(dateNotices.anniversaries(versions, company)).isPositive();
    assertThat(dateNotices.anniversaries(versions, company)).isZero();
    assertThat(notices("tsu", "Package anniversary: " + product)).isOne();
    assertThat(notices("tsulead", "Package anniversary: " + product)).isOne();
    // MBS is told only when PACKAGE_EXPIRY_NOTIFY_MBS is on (BDOI's recipients by default).
    assertThat(notices("mbs", "Package anniversary: " + product)).isZero();
  }

  private int notices(String user, String title) {
    Integer count =
        jdbc.queryForObject(
            "select count(*) from msg_notification where recipient = ? and title = ?",
            Integer.class,
            user,
            title);
    return count == null ? 0 : count;
  }
}
