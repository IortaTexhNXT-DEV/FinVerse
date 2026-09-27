package com.iortatechnxt.brokerverse.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Migrated reference masters (waves DM1-B and DM2-A): a legacy risk code the PRODUCT map creates is
 * added to the product catalogue with origin MIGRATED, pending authorization; a legacy payee is
 * added to the payee master with origin MIGRATED as a draft for Disbursement to submit; both show
 * in the Origin filter of their lists, and a rolled-back batch removes them.
 */
@IntegrationTest
class MigrationReferenceMastersApiIT {

  private static final String BASE = "/api/v1/migration";

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private ObjectMapper json;
  @Autowired private TestData data;
  @Autowired private JdbcTemplate jdbc;

  private MigrationTestSupport mig;
  private long company;

  @BeforeEach
  void setUp() throws Exception {
    company = data.company().getId();
    mig = new MigrationTestSupport(api, mvc, users, json, jdbc, company);
    mig.accepted("R01", "R02", "R03", "R04");
    for (String o : List.of("R05", "R09")) {
      mig.signMapping(o);
    }
  }

  @Test
  void aLegacyProductCreatedByTheMapIsAddedWithItsOrigin() throws Exception {
    String t = LegacyInvoiceFixtures.token();
    Map<String, String> r = new HashMap<>();
    r.put("line_code", "ENG");
    r.put("risk_code", "LGY-EAR");
    r.put("risk_name", "Erection all risks");
    r.put("status", "A");
    JsonNode extract =
        mig.upload(
            "R05",
            "R05",
            "R05_EBIX_20271120_" + t.substring(t.length() - 2) + ".csv",
            List.of(r),
            List.of("risk_code"));
    JsonNode batch = mig.load("R05", List.of(extract.get("extractNo").asText()));
    String batchNo = batch.get("batchNo").asText();
    assertThat(batch.get("counts").get("loaded").asInt()).as(mig.rows(batchNo)).isEqualTo(1);
    Map<String, Object> product =
        jdbc.queryForMap(
            "select origin, legacy_ref, migration_batch, record_status from cat_product where code = 'EAR77'");
    assertThat(product.get("origin")).isEqualTo("MIGRATED");
    assertThat(product.get("legacy_ref")).isEqualTo("LGY-EAR");
    JsonNode migrated = mig.get("ao", "/api/v1/catalog/products?origin=MIGRATED&q=EAR77");
    assertThat(migrated).hasSize(1);
    assertThat(migrated.get(0).get("origin").asText()).isEqualTo("MIGRATED");
    assertThat(mig.get("ao", "/api/v1/catalog/products?origin=BIBS&q=EAR77")).isEmpty();

    mig.post(
        "miglead", BASE + "/batches/" + batchNo + "/rollback", Map.of("reason", "Map revised"));
    mig.post(
        "migrecon", BASE + "/batches/" + batchNo + "/rollback/approve", Map.of("comment", "Ok"));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from cat_product where code = 'EAR77'", Integer.class))
        .isZero();
  }

  @Test
  void aLegacyPayeeIsAddedWithItsOriginAsADraft() throws Exception {
    String t = LegacyInvoiceFixtures.token();
    Map<String, String> r = new HashMap<>();
    r.put("payee_code", "SUP-" + t);
    r.put("payee_class", "SUPPLIER");
    r.put("name", "Acme Office Supply " + t);
    r.put("default_mode", "CHECK");
    r.put("allowed_modes", "CHECK|CTA");
    r.put("currency", "PHP");
    r.put("bank", "BDO");
    r.put("account_no", "0012" + t);
    JsonNode extract =
        mig.upload(
            "R09",
            "R09",
            "R09_EBIX_20271120_" + t.substring(t.length() - 2) + ".csv",
            List.of(r),
            List.of("payee_code"));
    JsonNode batch = mig.load("R09", List.of(extract.get("extractNo").asText()));
    String batchNo = batch.get("batchNo").asText();
    assertThat(batch.get("counts").get("loaded").asInt()).as(mig.rows(batchNo)).isEqualTo(1);
    Map<String, Object> payee =
        jdbc.queryForMap(
            "select origin, source, stage, legacy_ref from dsb_payee where payee_code = ?",
            "SUP-" + t);
    assertThat(payee.get("origin")).isEqualTo("MIGRATED");
    assertThat(payee.get("source")).isEqualTo("MIGRATION");
    assertThat(payee.get("stage")).isEqualTo("DRAFT");
    JsonNode migrated =
        mig.get(
            "disb",
            "/api/v1/disbursement/payees?companyId=" + company + "&origin=MIGRATED&q=SUP-" + t);
    assertThat(migrated.get("content")).hasSize(1);
    assertThat(migrated.get("content").get(0).get("origin").asText()).isEqualTo("MIGRATED");
    assertThat(
            mig.get(
                    "disb",
                    "/api/v1/disbursement/payees?companyId=" + company + "&origin=BIBS&q=SUP-" + t)
                .get("content"))
        .isEmpty();

    mig.post("miglead", BASE + "/batches/" + batchNo + "/rollback", Map.of("reason", "Wrong list"));
    mig.post(
        "migrecon", BASE + "/batches/" + batchNo + "/rollback/approve", Map.of("comment", "Ok"));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dsb_payee where payee_code = ?", Integer.class, "SUP-" + t))
        .isZero();
  }
}
