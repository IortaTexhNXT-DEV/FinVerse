package com.iortatechnxt.brokerverse.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.cashiering.service.AutomatchService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
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
 * Legacy unapplied payments (wave DM1-B): an EBIX unapplied payment is carried into Cashiering with
 * origin MIGRATED and ledger context LEGACY, its legacy receipt and references, its opening entry
 * on the legacy unapplied collections account, the Origin filter of the Unapplied workbench; the
 * automatch applies it to the open legacy invoice it refers to, posting legacy unapplied
 * collections against legacy premium receivable; a rolled-back batch closes an untouched item.
 */
@IntegrationTest
class MigrationLegacyUppApiIT {

  private static final String BASE = "/api/v1/migration";

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private ObjectMapper json;
  @Autowired private TestData data;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AutomatchService automatch;
  @Autowired private AsUser as;

  private MigrationTestSupport mig;
  private LegacyInvoiceFixtures fx;
  private long company;

  @BeforeEach
  void setUp() throws Exception {
    company = data.company().getId();
    mig = new MigrationTestSupport(api, mvc, users, json, jdbc, company);
    mig.accepted("R01", "R02", "R03", "R04", "R05", "P01");
    for (String o : List.of("C01", "F01", "F02")) {
      mig.signMapping(o);
    }
    fx = new LegacyInvoiceFixtures(mig, jdbc);
  }

  private JsonNode loadUpp(String ref, String t, String invoiceRef, String balance)
      throws Exception {
    Map<String, String> r = new HashMap<>();
    r.put("legacy_upp_ref", ref);
    r.put("legacy_ar_no", "AR-" + ref);
    r.put("legacy_ar_date", "2027-12-10");
    r.put("value_date", "2027-12-10");
    r.put("channel", "OTC");
    r.put("payor_name", "Lea Santos");
    r.put("legacy_client_no", "E" + t);
    r.put("currency", "PHP");
    r.put("amount", "1500.00");
    r.put("balance", balance);
    r.put("branch_code", "MKT");
    r.put("ref_invoice_nos", invoiceRef);
    r.put("legacy_status", "OPEN");
    JsonNode extract =
        mig.upload(
            "F02",
            "F02",
            "F02_EBIX_20271231_" + t.substring(t.length() - 2) + ".csv",
            List.of(r),
            List.of("legacy_upp_ref"));
    JsonNode batch = mig.load("F02", List.of(extract.get("extractNo").asText()));
    assertThat(batch.get("counts").get("loaded").asInt())
        .as(mig.rows(batch.get("batchNo").asText()))
        .isEqualTo(1);
    return batch;
  }

  @Test
  void aLegacyUnappliedPaymentIsCarriedAndAutomatchedToALegacyInvoice() throws Exception {
    String t = LegacyInvoiceFixtures.token();
    fx.client(t);
    String no = "I" + String.format("%08d", Long.parseLong(t) * 10 + 3);
    mig.accept(fx.loadInvoice(no, t).get("batchNo").asText());
    String invoiceNo =
        jdbc.queryForObject(
            "select invoice_no from ops_invoice where legacy_invoice_no = ?", String.class, no);
    String batchNo = loadUpp("UPP-" + t, t, no, "1000.00").get("batchNo").asText();

    Map<String, Object> item =
        jdbc.queryForMap(
            "select reference, origin, ledger_context, legacy_ar_no, balance, amount, migration_batch"
                + " from csh_unapplied where legacy_ref = ?",
            "UPP-" + t);
    assertThat(item.get("origin")).isEqualTo("MIGRATED");
    assertThat(item.get("ledger_context")).isEqualTo("LEGACY");
    assertThat(item.get("legacy_ar_no")).isEqualTo("AR-UPP-" + t);
    assertThat((BigDecimal) item.get("balance")).isEqualByComparingTo("1000.00");
    assertThat(item.get("migration_batch")).isEqualTo(batchNo);
    JsonNode recon = mig.get("migrecon", BASE + "/batches/" + batchNo + "/reconciliation");
    assertThat(recon.get("breakCount").asInt()).as(recon.get("lines").toString()).isZero();

    JsonNode migrated =
        mig.get(
            "cashier",
            "/api/v1/cashiering/unapplied?companyId=" + company + "&origin=MIGRATED&q=UPP-" + t);
    assertThat(migrated.get("content")).hasSize(1);
    assertThat(migrated.get("content").get(0).get("origin").asText()).isEqualTo("MIGRATED");
    assertThat(migrated.get("content").get(0).get("legacyArNo").asText()).isEqualTo("AR-UPP-" + t);
    assertThat(
            mig.get(
                    "cashier",
                    "/api/v1/cashiering/unapplied?companyId=" + company + "&origin=BIBS&q=UPP-" + t)
                .get("content"))
        .isEmpty();

    as.run("cashier", () -> automatch.run(LocalDate.of(2026, 9, 15)));
    assertThat(
            jdbc.queryForObject(
                "select balance from csh_unapplied where legacy_ref = ?",
                BigDecimal.class,
                "UPP-" + t))
        .isEqualByComparingTo("0");
    assertThat(fx.balance(invoiceNo, "DST")).isEqualByComparingTo("200.00");
    List<String> accounts =
        jdbc.queryForList(
            "select distinct a.code from jnl_line l join jnl_batch b on b.id = l.batch_id"
                + " join coa_account a on a.id = l.account_id"
                + " where b.source_reference in (select m.source_ref from ops_invoice_movement m"
                + " join ops_invoice i on i.id = m.invoice_id where i.invoice_no = ?"
                + " and m.movement_type = 'APPLIED')",
            String.class,
            invoiceNo);
    assertThat(accounts).contains("2206", "1215.02").doesNotContain("2205", "1210.02");
  }

  @Test
  void aRolledBackUppBatchClosesTheItemAndReversesItsOpening() throws Exception {
    String t = LegacyInvoiceFixtures.token();
    fx.client(t);
    String batchNo = loadUpp("UPR-" + t, t, "", "800.00").get("batchNo").asText();
    mig.post("miglead", BASE + "/batches/" + batchNo + "/rollback", Map.of("reason", "Wrong list"));
    mig.post(
        "migrecon", BASE + "/batches/" + batchNo + "/rollback/approve", Map.of("comment", "Ok"));
    Map<String, Object> item =
        jdbc.queryForMap(
            "select stage, balance from csh_unapplied where legacy_ref = ?", "UPR-" + t);
    assertThat(item.get("stage")).isEqualTo("CLOSED");
    assertThat((BigDecimal) item.get("balance")).isEqualByComparingTo("0");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from acc_event_log where event_type = 'MIG_UPP_OPENING'"
                    + " and source_reference like ? and status = 'POSTED'",
                Integer.class,
                "MIG:UPP:%:RB"))
        .isPositive();
  }
}
