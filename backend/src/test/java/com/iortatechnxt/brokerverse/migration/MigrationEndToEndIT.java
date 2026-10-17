package com.iortatechnxt.brokerverse.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.cashiering.service.AutomatchService;
import com.iortatechnxt.brokerverse.migration.seed.MigrationStoryline;
import com.iortatechnxt.brokerverse.migration.seed.MigrationTabsStoryline;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The data migration end to end (wave DM3): the SIT/UAT storyline goes through the real pipeline -
 * intake, validation, load, reconciliation and acceptance of reference data, a client, a policy
 * header, an open legacy invoice, a legacy unapplied payment and an archive record - then the
 * migrated data is worked in BIBS: the automatch applies the legacy payment to the legacy invoice,
 * an old legacy unapplied payment waits for Unapplied to Income, the account is endorsed on the
 * legacy accounts, Migration Clearing stays at zero, the legacy archive is searched, and the go /
 * no-go measures read the accepted objects.
 */
@IntegrationTest
class MigrationEndToEndIT {

  @Autowired private MigrationStoryline.Services services;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;
  @Autowired private TestData data;
  @Autowired private Api api;
  @Autowired private AutomatchService automatch;
  @Autowired private ReportService reports;
  @Autowired private MigrationTabsStoryline tabs;

  @Test
  void theStorylineMigratesAndIsWorkedInBibs() throws Exception {
    long company = data.company().getId();
    MigrationStoryline storyline = new MigrationStoryline(services, jdbc, as::run);
    Map<String, String> batches = storyline.run(company);
    assertThat(batches).containsKeys("R01", "C01", "P01", "F01", "F02", "H01");
    assertThat(storyline.loaded(company)).isTrue();
    for (String batchNo : batches.values()) {
      assertThat(
              jdbc.queryForObject(
                  "select status from mig_batch where batch_no = ?", String.class, batchNo))
          .as(batchNo)
          .isEqualTo("SIGNED_OFF");
    }

    Map<String, Object> invoice =
        jdbc.queryForMap(
            "select invoice_no, arn, origin, ledger_context from ops_invoice"
                + " where legacy_invoice_no = ?",
            MigrationStoryline.INVOICE);
    assertThat(invoice.get("origin")).isEqualTo("MIGRATED");
    assertThat(invoice.get("ledger_context")).isEqualTo("LEGACY");

    as.run("cashier", () -> automatch.run(LocalDate.of(2026, 9, 15)));
    assertThat(
            jdbc.queryForObject(
                "select balance from csh_unapplied where legacy_ref = 'UPP970001'",
                BigDecimal.class))
        .isEqualByComparingTo("0");

    // The old legacy unapplied payment of the seed waits for the Unapplied to Income batches.
    assertThat(storyline.loadOldUpp(company)).isPresent();
    assertThat(storyline.loadOldUpp(company)).isEmpty();
    String oldUpp =
        jdbc.queryForObject(
            "select reference from csh_unapplied where legacy_ref = ? and origin = 'MIGRATED'",
            String.class,
            MigrationStoryline.OLD_UPP);
    JsonNode toIncome =
        api.read(
            api.doGet(
                    "cashier",
                    "/api/v1/cashiering/legacy-batches/candidates?companyId="
                        + company
                        + "&origin=MIGRATED&minAgeDays=365")
                .andExpect(status().isOk()));
    assertThat(toIncome.findValuesAsText("reference")).contains(oldUpp);

    JsonNode endorsed =
        api.read(
            api.doPost(
                    "proc",
                    "/api/v1/booking/endorsements",
                    Map.of(
                        "arn", invoice.get("arn"),
                        "type", "POSITIVE",
                        "effectiveDate", "2028-02-01",
                        "premium", Map.of("basic", 300, "dst", 36, "total", 336),
                        "description", "Additional cover",
                        "bookingDate", "2026-09-20"))
                .andExpect(status().isCreated()));
    assertThat(
            jdbc.queryForObject(
                "select ledger_context from ops_invoice where invoice_no = ?",
                String.class,
                endorsed.get("invoiceNo").asText()))
        .isEqualTo("LEGACY");

    Map<String, String> params = Map.of("companyId", String.valueOf(company));
    ReportResult clearing = as.run("miglead", () -> reports.run("MIG-GL-CLEARING", params));
    assertThat(clearing.rows()).isNotEmpty();
    JsonNode archive =
        api.read(
            api.doGet(
                    "legacyaudit",
                    "/api/v1/legacy-inquiry/records?companyId="
                        + company
                        + "&invoiceNo=I95000021&reasonCode=AUDIT&reasonText=Storyline")
                .andExpect(status().isOk()));
    assertThat(archive.get("content")).hasSize(1);

    tabs.run(company, as::run);
    tabs.run(company, as::run);
    assertThat(
            jdbc.queryForList(
                "select name from mig_cutover_plan where company_id = ? and name in (?, ?)",
                String.class,
                company,
                MigrationTabsStoryline.MOCK_PLAN,
                MigrationTabsStoryline.PRODUCTION_PLAN))
        .containsExactlyInAnyOrder(
            MigrationTabsStoryline.MOCK_PLAN, MigrationTabsStoryline.PRODUCTION_PLAN);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from mig_cutover_task t join mig_cutover_plan p on p.id = t.plan_id"
                    + " where p.company_id = ? and t.status = 'DONE'",
                Integer.class,
                company))
        .isPositive();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from mig_decommission_item where company_id = ?"
                    + " and system_code = 'EBIX' and status = 'MET'",
                Integer.class,
                company))
        .isPositive();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from mig_runoff_cohort where company_id = ?",
                Integer.class,
                company))
        .isPositive();
  }
}
