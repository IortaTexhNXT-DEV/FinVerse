package com.iortatechnxt.brokerverse.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
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
 * The batches of the legacy context after go-live: old unapplied payments reclassified to other
 * income (team lead, then top management; the maker never approves), legacy PR 2307 balances
 * reversed against the insurer, and legacy direct payment premium receivables reversed; the legacy
 * unapplied payments report; the policy transaction history of a migrated invoice.
 */
@IntegrationTest
class MigrationLegacyBatchesApiIT {

  private static final String CSH = "/api/v1/cashiering/legacy-batches";
  private static final String DPPR = "/api/v1/commission/dppr-batches";

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
    mig.accepted("R01", "R02", "R03", "R04", "R05", "P01");
    for (String o : List.of("C01", "F01", "F02")) {
      mig.signMapping(o);
    }
    fx = new LegacyInvoiceFixtures(mig, jdbc);
  }

  private String legacyInvoice(String t, int suffix) throws Exception {
    String no = "I" + String.format("%08d", Long.parseLong(t) * 10 + suffix);
    mig.accept(fx.loadInvoice(no, t).get("batchNo").asText());
    return jdbc.queryForObject(
        "select invoice_no from ops_invoice where legacy_invoice_no = ?", String.class, no);
  }

  private String legacyUpp(String t) throws Exception {
    Map<String, String> r = new HashMap<>();
    r.put("legacy_upp_ref", "UIR-" + t);
    r.put("legacy_ar_no", "AR-UIR-" + t);
    r.put("legacy_ar_date", "2024-12-10");
    r.put("value_date", "2024-12-10");
    r.put("channel", "OTC");
    r.put("payor_name", "Lea Santos");
    r.put("legacy_client_no", "E" + t);
    r.put("currency", "PHP");
    r.put("amount", "900.00");
    r.put("balance", "900.00");
    r.put("branch_code", "MKT");
    r.put("ref_invoice_nos", "");
    r.put("legacy_status", "OPEN");
    JsonNode extract =
        mig.upload(
            "F02",
            "F02",
            "F02_EBIX_20271231_" + t.substring(t.length() - 2) + ".csv",
            List.of(r),
            List.of("legacy_upp_ref"));
    JsonNode batch = mig.load("F02", List.of(extract.get("extractNo").asText()));
    mig.accept(batch.get("batchNo").asText());
    return jdbc.queryForObject(
        "select reference from csh_unapplied where legacy_ref = ?", String.class, "UIR-" + t);
  }

  private List<String> accounts(String sourceRefPrefix) {
    return jdbc.queryForList(
        "select distinct a.code from jnl_line l join jnl_batch b on b.id = l.batch_id"
            + " join coa_account a on a.id = l.account_id where b.source_reference like ?",
        String.class,
        sourceRefPrefix + "%");
  }

  @Test
  void aLegacyUnappliedPaymentIsReclassifiedToIncomeAfterTwoApprovals() throws Exception {
    String t = LegacyInvoiceFixtures.token();
    fx.client(t);
    String reference = legacyUpp(t);
    Long id =
        jdbc.queryForObject(
            "select id from csh_unapplied where reference = ?", Long.class, reference);

    JsonNode candidates =
        mig.get("cashier", CSH + "/candidates?companyId=" + company + "&origin=MIGRATED");
    assertThat(candidates.findValuesAsText("reference")).contains(reference);

    JsonNode batch =
        mig.post(
            "cashier",
            CSH + "?companyId=" + company,
            Map.of("kind", "INCOME_RECLASS", "reason", "Unclaimed for years", "currency", "PHP"));
    String no = batch.get("batchNo").asText();
    mig.post("cashier", CSH + "/" + no + "/items", Map.of("unappliedId", id, "reason", "Aged"));
    api.doPost("cashier", CSH + "/" + no + "/items", Map.of("unappliedId", id))
        .andExpect(status().is4xxClientError());
    mig.post("cashier", CSH + "/" + no + "/submit", Map.of());
    api.doPost("cashier", CSH + "/" + no + "/approve", Map.of("comment", "Mine"))
        .andExpect(status().isForbidden());

    JsonNode first = mig.post("cashtl", CSH + "/" + no + "/approve", Map.of("comment", "Ok"));
    assertThat(first.get("status").asText()).isEqualTo("FOR_TOP_MANAGEMENT");
    api.doPost("cashtl", CSH + "/" + no + "/approve", Map.of("comment", "Again"))
        .andExpect(status().isForbidden());
    JsonNode done = mig.post("topmgmt", CSH + "/" + no + "/approve", Map.of("comment", "Ok"));
    assertThat(done.get("status").asText()).isEqualTo("EXECUTED");
    assertThat(done.get("postedCount").asInt()).isEqualTo(1);

    Map<String, Object> item =
        jdbc.queryForMap("select stage, balance from csh_unapplied where id = ?", id);
    assertThat(item.get("stage")).isEqualTo("CLOSED");
    assertThat(accounts("UIR:" + no + ":")).contains("2206", "4190").doesNotContain("2205");

    as.run(
        "cashtl",
        () -> {
          Map<String, String> p = new HashMap<>();
          p.put("companyId", String.valueOf(company));
          p.put("from", "2020-01-01");
          p.put("to", "2030-12-31");
          for (String code : List.of("CSH-UPP-LEGACY", "CSH-UPP-INCOME-RECLASS")) {
            assertThat(reports.export(code, p, ExportFormat.values()[0]).content()).isNotEmpty();
          }
          assertThat(reports.run("CSH-UPP-LEGACY", p).rows())
              .anyMatch(
                  r ->
                      reference.equals(r.cells().get("reference"))
                          && no.equals(r.cells().get("income_batch")));
          assertThat(reports.run("CSH-UPP-INCOME-RECLASS", p).rows())
              .anyMatch(r -> reference.equals(r.cells().get("reference")));
          return null;
        });
  }

  @Test
  void aLegacyPr2307BalanceIsReversedAgainstTheInsurer() throws Exception {
    String t = LegacyInvoiceFixtures.token();
    fx.client(t);
    String invoiceNo = legacyInvoice(t, 5);
    JsonNode batch =
        mig.post(
            "cashier",
            CSH + "?companyId=" + company,
            Map.of(
                "kind", "PR2307_REVERSAL", "reason", "Insurer 2307 received", "currency", "PHP"));
    String no = batch.get("batchNo").asText();
    api.doPost(
            "cashier",
            CSH + "/" + no + "/invoices",
            Map.of("invoiceNo", invoiceNo, "amount", new BigDecimal("99999.00")))
        .andExpect(status().is4xxClientError());
    mig.post(
        "cashier",
        CSH + "/" + no + "/invoices",
        Map.of("invoiceNo", invoiceNo, "amount", new BigDecimal("500.00"), "reason", "2307"));
    mig.post("cashier", CSH + "/" + no + "/submit", Map.of());
    JsonNode done = mig.post("cashtl", CSH + "/" + no + "/approve", Map.of("comment", "Ok"));
    assertThat(done.get("status").asText()).isEqualTo("EXECUTED");
    JsonNode detail = mig.get("cashtl", CSH + "/" + no);
    assertThat(detail.get("lines").get(0).get("status").asText())
        .as(detail.toString())
        .isEqualTo("POSTED");
    assertThat(fx.balance(invoiceNo, "DTIP")).isEqualByComparingTo("10700.00");
    assertThat(accounts("P2R:" + no + ":")).contains("1216").doesNotContain("1210.01");
  }

  @Test
  void aLegacyDirectPaymentPremiumIsReversedAndTheHistoryShowsTheMigration() throws Exception {
    String t = LegacyInvoiceFixtures.token();
    fx.client(t);
    String invoiceNo = legacyInvoice(t, 7);

    JsonNode history = mig.get("proc", "/api/v1/ops/invoices/" + invoiceNo + "/transactions");
    JsonNode first = history.get("rows").get(0);
    assertThat(first.get("typeLabel").asText()).contains("Migrated");
    assertThat(first.get("status").asText()).isEqualTo("MIGRATED");
    assertThat(first.get("journals")).isNotEmpty();

    assertThat(
            mig.get("commrec", DPPR + "/candidates?companyId=" + company)
                .findValuesAsText("invoiceNo"))
        .contains(invoiceNo);
    String no =
        mig.post("commrec", DPPR + "?companyId=" + company, Map.of("comment", "Paid to insurer"))
            .get("batchNo")
            .asText();
    api.doPost(
            "commrec",
            DPPR + "/" + no + "/invoices",
            Map.of("invoiceNo", invoiceNo, "amount", new BigDecimal("1.00")))
        .andExpect(status().is4xxClientError());
    mig.post("commrec", DPPR + "/" + no + "/invoices", Map.of("invoiceNo", invoiceNo));
    mig.post("commrec", DPPR + "/" + no + "/submit", Map.of());
    api.doPost("commrec", DPPR + "/" + no + "/approve", Map.of("comment", "Mine"))
        .andExpect(status().isForbidden());
    JsonNode done = mig.post("commtl", DPPR + "/" + no + "/approve", Map.of("comment", "Ok"));
    assertThat(done.get("status").asText()).isEqualTo("EXECUTED");
    assertThat(done.get("postedCount").asInt())
        .as(mig.get("commtl", DPPR + "/" + no).toString())
        .isEqualTo(1);
    assertThat(fx.balance(invoiceNo, "BASIC")).isEqualByComparingTo("0");
    assertThat(fx.balance(invoiceNo, "DTIP")).isEqualByComparingTo("4000.00");
    assertThat(accounts("DPPR:" + no + ":")).contains("LGC-DTIP", "1215.01");
  }

  @Test
  void theRemittanceSchedulesTakeTheOriginFilter() {
    as.run(
        "cashtl",
        () -> {
          for (String code :
              List.of(
                  "REM-SCHEDULE-NORMAL",
                  "REM-SCHEDULE-SPECIAL",
                  "REM-SCHEDULE-INCENTIVE",
                  "REM-SPECIAL-REGISTER")) {
            assertThat(
                    reports.catalogue().stream()
                        .filter(m -> m.code().equals(code))
                        .findFirst()
                        .orElseThrow()
                        .parameters())
                .extracting(ParameterSpec::name)
                .contains("origin");
            Map<String, String> p = new HashMap<>();
            p.put("companyId", String.valueOf(company));
            p.put("from", "2020-01-01");
            p.put("to", "2030-12-31");
            p.put("origin", "MIGRATED");
            assertThat(reports.run(code, p).code()).isEqualTo(code);
          }
          return null;
        });
  }
}
