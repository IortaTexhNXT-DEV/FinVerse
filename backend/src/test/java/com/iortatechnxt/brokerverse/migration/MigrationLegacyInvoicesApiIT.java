package com.iortatechnxt.brokerverse.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.util.ArrayList;
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
  }

  private static String token() {
    return Long.toString(System.nanoTime() % 1_000_000L);
  }

  private String client(String t) throws Exception {
    Map<String, String> r = new HashMap<>();
    r.put("legacy_client_no", "E" + t);
    r.put("client_type", "I");
    r.put("last_name", "Santos" + t);
    r.put("first_name", "Lea");
    r.put("birth_date", "1980-02-11");
    r.put("tin", "");
    r.put("market_segment", "CBG");
    r.put("email", "e" + t + "@example.ph");
    r.put("kyc_status", "COMPLETE");
    r.put("kyc_verified_date", "2026-05-02");
    r.put("kyc_review_due", "2029-05-02");
    r.put("client_status", "A");
    r.put("created_date", "2021-03-01");
    r.put("last_updated", "2027-10-01 09:00:00");
    JsonNode extract =
        mig.upload(
            "C01",
            "C01",
            "C01_EBIX_20271120_" + t.substring(t.length() - 2) + ".csv",
            List.of(r),
            List.of("legacy_client_no"));
    JsonNode batch = mig.load("C01", List.of(extract.get("extractNo").asText()));
    assertThat(batch.get("counts").get("loaded").asInt())
        .as(mig.rows(batch.get("batchNo").asText()))
        .isEqualTo(1);
    mig.accept(batch.get("batchNo").asText());
    return jdbc.queryForObject(
        "select client_code from crm_client where legacy_ref = ?", String.class, "E" + t);
  }

  private static Map<String, String> header(String no, String t) {
    Map<String, String> h = new HashMap<>();
    h.put("legacy_invoice_no", no);
    h.put("invoice_kind", "BOOKING");
    h.put("policy_no", "MC-" + t);
    h.put("legacy_client_no", "E" + t);
    h.put("assured_name", "Lea Santos");
    h.put("currency", "PHP");
    h.put("booking_date", "2027-11-02");
    h.put("invoice_date", "2027-11-02");
    h.put("due_date", "2027-12-02");
    h.put("inception_date", "2027-11-01");
    h.put("expiry_date", "2028-11-01");
    h.put("risk_code", "CAR-A");
    h.put("line_code", "ENG");
    h.put("market_segment", "CBG");
    h.put("ao_user_id", "AO01");
    h.put("sales_unit_code", "U01");
    h.put("branch_code", "MKT");
    h.put("cost_center", "OPS");
    h.put("gross_premium", "11200.00");
    h.put("commission", "1500.00");
    h.put("vat_on_commission", "180.00");
    h.put("wtax_rate_pct", "10");
    h.put("commission_realised", "500.00");
    h.put("deferred_vat_open", "120.00");
    h.put("dp_flag", "N");
    h.put("cwt_flag", "N");
    return h;
  }

  private static Map<String, String> position(
      String no, String component, String booked, String paid, String open) {
    Map<String, String> p = new HashMap<>();
    p.put("legacy_invoice_no", no);
    p.put("component", component);
    p.put("booked", booked);
    p.put("adjusted", "0.00");
    p.put("paid", paid);
    p.put("remitted", "0.00");
    p.put("written_off", "0.00");
    p.put("open_balance", open);
    return p;
  }

  /** Uploads and loads one invoice; returns the batch. */
  private JsonNode loadInvoice(String no, String t) throws Exception {
    String seq = t.substring(t.length() - 2);
    JsonNode f01 =
        mig.upload(
            "F01",
            "F01",
            "F01_EBIX_20271231_" + seq + ".csv",
            List.of(header(no, t)),
            List.of("legacy_invoice_no"));
    JsonNode f01s =
        mig.upload(
            "F01",
            "F01S",
            "F01S_EBIX_20271231_" + seq + ".csv",
            List.of(
                Map.of(
                    "legacy_invoice_no", no,
                    "share_seq", "1",
                    "insurer_code", "MGIC",
                    "share_pct", "100",
                    "lead_flag", "Y")),
            List.of("legacy_invoice_no", "share_seq"));
    List<Map<String, String>> positions = new ArrayList<>();
    positions.add(position(no, "BASIC", "10000.00", "4000.00", "6000.00"));
    positions.add(position(no, "DST", "1200.00", "0.00", "1200.00"));
    positions.add(position(no, "DTIP", "11200.00", "0.00", "11200.00"));
    positions.add(position(no, "COMMISSION", "1500.00", "0.00", "1500.00"));
    positions.add(position(no, "COMMISSION_VAT", "180.00", "0.00", "180.00"));
    positions.add(position(no, "WTAX", "150.00", "0.00", "150.00"));
    JsonNode f01c =
        mig.upload(
            "F01",
            "F01C",
            "F01C_EBIX_20271231_" + seq + ".csv",
            positions,
            List.of("legacy_invoice_no", "component"));
    assertThat(f01.get("status").asText()).isEqualTo("STAGED");
    assertThat(f01c.get("status").asText()).isEqualTo("STAGED");
    JsonNode batch =
        mig.load(
            "F01",
            List.of(
                f01.get("extractNo").asText(),
                f01s.get("extractNo").asText(),
                f01c.get("extractNo").asText()));
    assertThat(batch.get("counts").get("loaded").asInt())
        .as(mig.rows(batch.get("batchNo").asText()))
        .isEqualTo(8);
    return batch;
  }

  private BigDecimal balance(String invoiceNo, String component) {
    return jdbc.queryForObject(
        "select c.balance from ops_invoice_component c join ops_invoice i on i.id = c.invoice_id"
            + " where i.invoice_no = ? and c.component = ?",
        BigDecimal.class,
        invoiceNo,
        component);
  }

  @Test
  void anOpenLegacyInvoiceIsLoadedWithItsPositionsEntryAndOpenItems() throws Exception {
    String t = token();
    String clientCode = client(t);
    String no = "I" + String.format("%08d", Long.parseLong(t));
    JsonNode batch = loadInvoice(no, t);
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
    assertThat(balance(invoiceNo, "BASIC")).isEqualByComparingTo("6000.00");
    assertThat(balance(invoiceNo, "DTIP")).isEqualByComparingTo("11200.00");
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
    String t = token();
    client(t);
    String no = "I" + String.format("%08d", Long.parseLong(t) + 1);
    String batchNo = loadInvoice(no, t).get("batchNo").asText();
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
    assertThat(balance(invoiceNo, "BASIC")).isEqualByComparingTo("0");
    assertThat(balance(invoiceNo, "DTIP")).isEqualByComparingTo("0");
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
