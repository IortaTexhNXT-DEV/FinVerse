package com.iortatechnxt.brokerverse.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Open legacy invoices (wave DM1-A): an EBIX invoice with its shares and component positions is
 * loaded into the Operations ledger with origin MIGRATED and ledger context LEGACY, its balances at
 * the open position of the cut-over, the frozen snapshot, the opening entry on the legacy control
 * accounts balanced on migration clearing, the sub-ledger open items, the LEGACY search filter and
 * the legacy block of the invoice 360; the GL level of the reconciliation holds, and a rolled-back
 * batch reverses the invoice, its entry and its open items.
 */
@IntegrationTest
class MigrationLegacyInvoicesApiIT {

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
    mig.accepted("R01", "R02", "R03", "R04", "R05", "P01");
    mig.signMapping("C01");
    mig.signMapping("F01");
    fx = new LegacyInvoiceFixtures(mig, jdbc);
  }

  private LegacyInvoiceFixtures fx;

  @Test
  void anOpenLegacyInvoiceIsLoadedWithItsPositionsEntryAndOpenItems() throws Exception {
    String t = LegacyInvoiceFixtures.token();
    String clientCode = fx.client(t);
    String no = "I" + String.format("%08d", Long.parseLong(t));
    JsonNode batch = fx.loadInvoice(no, t);
    String batchNo = batch.get("batchNo").asText();

    Map<String, Object> invoice =
        jdbc.queryForMap(
            "select invoice_no, origin, ledger_context, feed_source, source_system, legacy_invoice_no,"
                + " migration_batch, client_code, insurer_code, arn from ops_invoice"
                + " where legacy_invoice_no = ?",
            no);
    String invoiceNo = (String) invoice.get("invoice_no");
    assertThat(invoice.get("origin")).isEqualTo("MIGRATED");
    assertThat(invoice.get("ledger_context")).isEqualTo("LEGACY");
    assertThat(invoice.get("feed_source")).isEqualTo("MIGRATION");
    assertThat(invoice.get("source_system")).isEqualTo("EBIX");
    assertThat(invoice.get("migration_batch")).isEqualTo(batchNo);
    assertThat(invoice.get("client_code")).isEqualTo(clientCode);
    assertThat(invoice.get("insurer_code")).isEqualTo("INS-MGIC");
    assertThat((String) invoice.get("arn")).startsWith("LGY-EBIX-");
    assertThat(fx.balance(invoiceNo, "BASIC")).isEqualByComparingTo("6000.00");
    assertThat(fx.balance(invoiceNo, "DTIP")).isEqualByComparingTo("11200.00");
    assertThat(
            jdbc.queryForObject(
                "select sum(m.amount) from ops_invoice_movement m join ops_invoice i on i.id = m.invoice_id"
                    + " where i.invoice_no = ? and m.movement_type = 'LEGACY_PAID'",
                BigDecimal.class,
                invoiceNo))
        .isEqualByComparingTo("4000.00");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from ops_invoice_origin_line l"
                    + " join ops_invoice_origin_snapshot s on s.id = l.snapshot_id"
                    + " where s.legacy_invoice_no = ?",
                Integer.class,
                no))
        .isEqualTo(6);

    // Opening entry: posted, balanced on migration clearing (PR 7,200 + commission 1,530
    // - DTIP 11,200 - unrealised 1,000 - deferred VAT 120 = clearing debit 3,590).
    assertThat(
            jdbc.queryForObject(
                "select status from acc_event_log where event_type = 'MIG_LEGACY_INVOICE_OPENING'"
                    + " and source_reference = ?",
                String.class,
                "MIG:INV:" + invoiceNo + ":INS-MGIC"))
        .isEqualTo("POSTED");
    assertThat(
            jdbc.queryForObject(
                "select sum(l.amount) from jnl_line l join jnl_batch b on b.id = l.batch_id"
                    + " join coa_account a on a.id = l.account_id"
                    + " where b.source_reference = ? and a.code = 'LGC-CLR' and l.side = 'DEBIT'",
                BigDecimal.class,
                "MIG:INV:" + invoiceNo + ":INS-MGIC"))
        .isEqualByComparingTo("3590.00");
    assertThat(
            jdbc.queryForList(
                "select document_type from sl_open_item where source_reference like ? order by id",
                String.class,
                "MIG:INV:" + invoiceNo + ":%"))
        .containsExactly("LEGACY_PREMIUM", "LEGACY_DTIP", "LEGACY_COMMISSION");

    // Origin filter and the legacy block of the invoice 360.
    JsonNode found =
        mig.get("recon", "/api/v1/ops/invoices?companyId=" + company + "&origin=MIGRATED&q=" + no);
    assertThat(found.get("content")).hasSize(1);
    assertThat(found.get("content").get(0).get("origin").asText()).isEqualTo("MIGRATED");
    assertThat(found.get("content").get(0).get("legacyInvoiceNo").asText()).isEqualTo(no);
    assertThat(
            mig.get("recon", "/api/v1/ops/invoices?companyId=" + company + "&origin=BIBS&q=" + no)
                .get("content"))
        .isEmpty();
    JsonNode view = mig.get("recon", "/api/v1/ops/invoices/" + invoiceNo);
    assertThat(view.get("invoice").get("ledgerContext").asText()).isEqualTo("LEGACY");
    assertThat(view.get("legacy").get("legacyInvoiceNo").asText()).isEqualTo(no);
    assertThat(view.get("legacy").get("lines")).hasSize(6);

    JsonNode recon = mig.get("migrecon", BASE + "/batches/" + batchNo + "/reconciliation");
    assertThat(recon.get("breakCount").asInt()).as(recon.get("lines").toString()).isZero();
    assertThat(recon.get("lines").toString()).contains("L5");
    mig.accept(batchNo);
  }

  @Test
  void aRolledBackInvoiceBatchReversesTheInvoiceItsEntryAndOpenItems() throws Exception {
    String t = LegacyInvoiceFixtures.token();
    fx.client(t);
    String no = "I" + String.format("%08d", Long.parseLong(t) + 1);
    String batchNo = fx.loadInvoice(no, t).get("batchNo").asText();
    String invoiceNo =
        jdbc.queryForObject(
            "select invoice_no from ops_invoice where legacy_invoice_no = ?", String.class, no);
    mig.post(
        "miglead", BASE + "/batches/" + batchNo + "/rollback", Map.of("reason", "Wrong extract"));
    JsonNode rolledBack =
        mig.post(
            "migrecon",
            BASE + "/batches/" + batchNo + "/rollback/approve",
            Map.of("comment", "Ok"));
    assertThat(rolledBack.get("status").asText()).isEqualTo("ROLLED_BACK");
    assertThat(fx.balance(invoiceNo, "BASIC")).isEqualByComparingTo("0");
    assertThat(fx.balance(invoiceNo, "DTIP")).isEqualByComparingTo("0");
    assertThat(
            jdbc.queryForObject(
                "select cancelled from ops_invoice where invoice_no = ?", Boolean.class, invoiceNo))
        .isTrue();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from acc_event_log where source_reference = ? and status = 'POSTED'",
                Integer.class,
                "MIG:INV:" + invoiceNo + ":INS-MGIC:RB"))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select coalesce(sum(amount - settled_amount), 0) from sl_open_item"
                    + " where source_reference like ?",
                BigDecimal.class,
                "MIG:INV:" + invoiceNo + ":%"))
        .isEqualByComparingTo("0");
  }
}
