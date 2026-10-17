package com.iortatechnxt.brokerverse.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
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
 * The GL opening of the year-end cut-over (wave DM1-A): the provisional opening trial balance is
 * posted with the FY2027 result on retained earnings and the legacy control accounts on Migration
 * Clearing, which nets to zero against the open legacy invoices; a FY2027 true-up is prepared by
 * Comptrollership, approved by the Head of Comptrollership, posted with its open-item detail (a
 * legacy write-off), reconciled against the legacy trial balances and signed.
 */
@IntegrationTest
class MigrationGlOpeningApiIT {

  private static final String BASE = "/api/v1/migration";
  private static final String BANK = "1101-02";
  private static final String INCOME = "4700-00";
  private static final String CONTROL = "1210-00";

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private ObjectMapper json;
  @Autowired private TestData data;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ReportService reports;
  @Autowired private AsUser as;

  private MigrationTestSupport mig;
  private LegacyInvoiceFixtures fx;
  private long company;

  @BeforeEach
  void setUp() throws Exception {
    company = data.company().getId();
    mig = new MigrationTestSupport(api, mvc, users, json, jdbc, company);
    mig.accepted("R01", "R02", "R03", "R04", "R05", "R08", "P01", "F02");
    for (String o : List.of("C01", "F01", "G01", "G03")) {
      mig.signMapping(o);
    }
    fx = new LegacyInvoiceFixtures(mig, jdbc);
  }

  /** Balance of Migration Clearing of the head office in pesos, debit positive. */
  private BigDecimal clearing() {
    return jdbc.queryForObject(
        "select coalesce(sum(e.debit_fc - e.credit_fc), 0) from gl_ledger_entry e"
            + " join coa_account a on a.id = e.account_id join org_branch b on b.id = e.branch_id"
            + " where e.company_id = ? and a.code = 'LGC-CLR' and b.code = 'HO' and e.currency = 'PHP'",
        BigDecimal.class,
        company);
  }

  private static Map<String, String> tb(
      String version, String account, BigDecimal dr, BigDecimal cr) {
    Map<String, String> r = new HashMap<>();
    r.put("tb_version", version);
    r.put("as_of_date", "2027-12-31");
    r.put("branch_code", "MKT");
    r.put("currency", "PHP");
    r.put("legacy_account_code", account);
    r.put("cost_center", "");
    r.put("debit_fc", dr.toPlainString());
    r.put("credit_fc", cr.toPlainString());
    r.put("debit_php", dr.toPlainString());
    r.put("credit_php", cr.toPlainString());
    return r;
  }

  private static List<Map<String, String>> trialBalance(
      String version, BigDecimal clearing, BigDecimal adjustment, BigDecimal writeOff) {
    BigDecimal zero = BigDecimal.ZERO;
    BigDecimal control = clearing.add(writeOff);
    return List.of(
        tb(version, BANK, new BigDecimal("100000").add(clearing).add(adjustment), zero),
        tb(version, "3100-00", zero, new BigDecimal("80000")),
        tb(version, INCOME, zero, new BigDecimal("20000").add(adjustment).subtract(writeOff)),
        control.signum() >= 0
            ? tb(version, CONTROL, zero, control)
            : tb(version, CONTROL, control.negate(), zero));
  }

  private static Map<String, String> journalLine(
      String journal, int line, String account, String dr, String cr) {
    Map<String, String> r = new HashMap<>();
    r.put("trueup_no", "1");
    r.put("legacy_journal_no", journal);
    r.put("line_no", String.valueOf(line));
    r.put("legacy_journal_date", "2027-12-31");
    r.put("posted_at", "2028-01-18 10:00:00");
    r.put("adjustment_kind", "AUDIT");
    r.put("register_ref", "REG-" + journal);
    r.put("branch_code", "MKT");
    r.put("currency", "PHP");
    r.put("legacy_account_code", account);
    r.put("cost_center", "");
    r.put("debit_fc", dr);
    r.put("credit_fc", cr);
    r.put("debit_php", dr);
    r.put("credit_php", cr);
    r.put("description", "Audit adjustment");
    r.put("prepared_by", "gl.lead");
    r.put("approved_by", "comptroller");
    return r;
  }

  @Test
  void theOpeningTrialBalanceAndATrueUpPostThroughMigrationClearing() throws Exception {
    String t = LegacyInvoiceFixtures.token();
    String seq = t.substring(t.length() - 2);
    fx.client(t);
    String no = "I" + String.format("%08d", Long.parseLong(t) * 10 + 7);
    mig.accept(fx.loadInvoice(no, t).get("batchNo").asText());
    String invoiceNo =
        jdbc.queryForObject(
            "select invoice_no from ops_invoice where legacy_invoice_no = ?", String.class, no);
    BigDecimal x = clearing();
    assertThat(x.signum()).isNotZero();

    // Provisional opening: the control account line clears the open invoices' opening entries.
    List<String> keys =
        List.of("tb_version", "branch_code", "currency", "legacy_account_code", "cost_center");
    JsonNode provisional =
        mig.upload(
            "G01",
            "G01",
            "G01_EBIX_20271231_" + seq + ".csv",
            trialBalance("PROVISIONAL", x, BigDecimal.ZERO, BigDecimal.ZERO),
            keys);
    JsonNode opening = mig.load("G01", List.of(provisional.get("extractNo").asText()));
    String openingBatch = opening.get("batchNo").asText();
    assertThat(opening.get("counts").get("loaded").asInt()).as(mig.rows(openingBatch)).isEqualTo(4);
    Map<String, Object> journal =
        jdbc.queryForMap(
            "select b.journal_type, b.status, b.source_module from jnl_batch b where b.source_reference = ?",
            "MIG-TB-2027-12-31-HO-PHP");
    assertThat(journal.get("journal_type")).isEqualTo("OPENING");
    assertThat(journal.get("source_module")).isEqualTo("MIGRATION");
    assertThat(
            jdbc.queryForObject(
                "select sum(l.amount) from jnl_line l join jnl_batch b on b.id = l.batch_id"
                    + " join coa_account a on a.id = l.account_id"
                    + " where b.source_reference = ? and a.code = '3500' and l.side = 'CREDIT'",
                BigDecimal.class,
                "MIG-TB-2027-12-31-HO-PHP"))
        .isEqualByComparingTo("20000");
    assertThat(clearing()).isEqualByComparingTo("0");
    mig.accept(openingBatch);

    // Legacy trial balance after the year-end close (true-up 1): kept, not posted.
    JsonNode tu1 =
        mig.upload(
            "G01",
            "G01",
            "G01_EBIX_20280115_" + seq + ".csv",
            trialBalance("TU1", x, new BigDecimal("500"), new BigDecimal("1000")),
            keys);
    JsonNode tuBatch = mig.load("G01", List.of(tu1.get("extractNo").asText()));
    assertThat(tuBatch.get("counts").get("loaded").asInt())
        .as(mig.rows(tuBatch.get("batchNo").asText()))
        .isEqualTo(4);

    // The adjustment journals since the freeze and the open-item detail of the control account.
    JsonNode g03 =
        mig.upload(
            "G03",
            "G03",
            "G03_EBIX_20280115_" + seq + ".csv",
            List.of(
                journalLine("J1-" + t, 1, BANK, "500.00", "0.00"),
                journalLine("J1-" + t, 2, INCOME, "0.00", "500.00"),
                journalLine("J2-" + t, 1, INCOME, "1000.00", "0.00"),
                journalLine("J2-" + t, 2, CONTROL, "0.00", "1000.00")),
            List.of("legacy_journal_no"));
    Map<String, String> detail = new HashMap<>();
    detail.put("trueup_no", "1");
    detail.put("legacy_journal_no", "J2-" + t);
    detail.put("line_no", "2");
    detail.put("item_kind", "INVOICE");
    detail.put("item_ref", no);
    detail.put("component", "BASIC");
    detail.put("movement", "WRITTEN_OFF");
    detail.put("currency", "PHP");
    detail.put("amount", "1000.00");
    detail.put("reason", "Uncollectible per audit");
    JsonNode g03d =
        mig.upload(
            "G03",
            "G03D",
            "G03D_EBIX_20280115_" + seq + ".csv",
            List.of(detail),
            List.of("legacy_journal_no", "line_no", "item_ref", "component"));
    String batchNo =
        mig.post(
                "migops",
                BASE + "/batches?companyId=" + company,
                Map.of(
                    "objectCode",
                    "G03",
                    "extractNos",
                    List.of(g03.get("extractNo").asText(), g03d.get("extractNo").asText())))
            .get("batchNo")
            .asText();
    mig.post("migops", BASE + "/batches/" + batchNo + "/validate", Map.of());
    mig.post(
        "migsteward",
        BASE + "/batches/" + batchNo + "/signoff/validation",
        Map.of("approve", true));

    Map<String, Object> prepare = new HashMap<>();
    prepare.put("trueupNo", "1");
    prepare.put("asOf", "2028-01-15");
    prepare.put("batchNo", batchNo);
    prepare.put("tbBatchNo", tuBatch.get("batchNo").asText());
    String ref =
        mig.post("comptrol", BASE + "/trueups?companyId=" + company, prepare)
            .get("reference")
            .asText();
    mig.post("comptrol", BASE + "/trueups/" + ref + "/submit", Map.of("note", "First true-up"));
    assertThat(
            mig.post(
                    "migrecon",
                    BASE + "/trueups/" + ref + "/decide",
                    Map.of("approve", true, "note", "Agreed with the audit"))
                .get("status")
                .asText())
        .isEqualTo("APPROVED");
    mig.post("miglead", BASE + "/batches/" + batchNo + "/approve-load", Map.of("comment", "Go"));
    JsonNode loaded = mig.post("migops", BASE + "/batches/" + batchNo + "/load", Map.of());
    assertThat(loaded.get("counts").get("loaded").asInt()).as(mig.rows(batchNo)).isEqualTo(5);

    assertThat(fx.balance(invoiceNo, "BASIC")).isEqualByComparingTo("5000.00");
    assertThat(clearing()).isEqualByComparingTo("0");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from jnl_batch where source_reference = ? and journal_type = 'OPENING'",
                Integer.class,
                ref + "-HO-PHP"))
        .isEqualTo(1);
    JsonNode reconciled = mig.post("comptrol", BASE + "/trueups/" + ref + "/reconcile", Map.of());
    assertThat(reconciled.get("status").asText())
        .as(mig.get("migrecon", BASE + "/batches/" + batchNo + "/reconciliation").toString())
        .isEqualTo("RECONCILED");
    assertThat(reconciled.get("journalsPosted").asInt()).isEqualTo(1);
    assertThat(reconciled.get("itemsAdjusted").asInt()).isEqualTo(1);
    assertThat(
            mig.post("migrecon", BASE + "/trueups/" + ref + "/sign", Map.of())
                .get("status")
                .asText())
        .isEqualTo("SIGNED");
    Map<String, String> params = Map.of("companyId", String.valueOf(company));
    ReportResult register = as.run("miglead", () -> reports.run("MIG-TRUEUP-REGISTER", params));
    assertThat(register.rows()).anyMatch(r -> ref.equals(r.cells().get("reference")));
    ReportResult recon = as.run("miglead", () -> reports.run("MIG-TRUEUP-RECON", params));
    assertThat(recon.rows()).isNotEmpty();
    ReportResult clearing = as.run("miglead", () -> reports.run("MIG-GL-CLEARING", params));
    assertThat(clearing.rows()).isNotEmpty();
  }
}
