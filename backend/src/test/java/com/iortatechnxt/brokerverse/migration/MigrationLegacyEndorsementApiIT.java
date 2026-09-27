package com.iortatechnxt.brokerverse.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Endorsements of legacy invoices (wave DM2-A): a migrated account whose policy year is only held
 * as an open legacy invoice is endorsed in BIBS; the endorsement starts from the legacy original,
 * joins the family of the legacy invoice in the ledger context LEGACY, posts on the legacy control
 * accounts, and the production reconciliation lists it in the changes to legacy invoices with the
 * original, updated and delta amounts.
 */
@IntegrationTest
class MigrationLegacyEndorsementApiIT {

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
    mig.accepted("R01", "R02", "R03", "R04", "R05");
    for (String o : List.of("C01", "P01", "F01")) {
      mig.signMapping(o);
    }
    fx = new LegacyInvoiceFixtures(mig, jdbc);
  }

  @Test
  void aLegacyInvoiceIsEndorsedOnTheLegacyAccounts() throws Exception {
    String t = LegacyInvoiceFixtures.token();
    fx.client(t);
    String ref = fx.loadHeader(t);
    String no = "I" + String.format("%08d", Long.parseLong(t) * 10 + 5);
    mig.accept(fx.loadInvoice(no, t, ref).get("batchNo").asText());
    Map<String, Object> legacy =
        jdbc.queryForMap("select invoice_no, arn from ops_invoice where legacy_invoice_no = ?", no);
    String arn = (String) legacy.get("arn");
    assertThat(arn).doesNotStartWith("LGY-");

    Map<String, Object> endorsement =
        Map.of(
            "arn", arn,
            "type", "POSITIVE",
            "effectiveDate", "2028-03-01",
            "premium", Map.of("basic", 500, "dst", 60, "total", 560),
            "description", "Additional equipment",
            "bookingDate", "2026-09-20");
    JsonNode result =
        api.read(
            api.doPost("proc", "/api/v1/booking/endorsements", endorsement)
                .andExpect(status().isCreated()));
    String invoiceNo = result.get("invoiceNo").asText();
    assertThat(
            jdbc.queryForObject(
                "select ledger_context from bkg_invoice where invoice_no = ?",
                String.class,
                invoiceNo))
        .isEqualTo("LEGACY");
    Map<String, Object> ops =
        jdbc.queryForMap(
            "select parent_invoice_no, ledger_context from ops_invoice where invoice_no = ?",
            invoiceNo);
    assertThat(ops.get("parent_invoice_no")).isEqualTo(legacy.get("invoice_no"));
    assertThat(ops.get("ledger_context")).isEqualTo("LEGACY");
    List<String> accounts =
        jdbc.queryForList(
            "select distinct a.code from jnl_line l join jnl_batch b on b.id = l.batch_id"
                + " join coa_account a on a.id = l.account_id where b.source_reference like ?",
            String.class,
            "BKG:" + invoiceNo + ":%");
    assertThat(accounts).contains("1215.01", "LGC-DTIP").doesNotContain("1210.01", "2210");

    ReportResult changes =
        as.run(
            "recon",
            () ->
                reports.run(
                    "PRC-LEGACY-CHANGES",
                    Map.of(
                        "companyId", String.valueOf(company),
                        "from", "2026-01-01",
                        "to", "2026-12-31")));
    assertThat(changes.rows())
        .anyMatch(
            r ->
                invoiceNo.equals(r.cells().get("invoice_no"))
                    && "ENDORSEMENT".equals(r.cells().get("change_type")));
  }
}
