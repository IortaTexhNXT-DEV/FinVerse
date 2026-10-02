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
 * Legacy invoices in Collections (wave DM2-A): the worklist item of an open legacy invoice carries
 * origin MIGRATED with its legacy invoice number (LEGACY badge, Origin filter), and the open
 * follow-up carried from legacy (object F03) assigns it to its collector without a notification,
 * keeps the latest disposition and flags the promise still ahead; a rolled-back batch undoes the
 * follow-up.
 */
@IntegrationTest
class MigrationCollectionStateApiIT {

  private static final String BASE = "/api/v1/migration";

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private ObjectMapper json;
  @Autowired private TestData data;
  @Autowired private JdbcTemplate jdbc;

  private MigrationTestSupport mig;
  private LegacyInvoiceFixtures fx;
  private long company;

  @BeforeEach
  void setUp() throws Exception {
    company = data.company().getId();
    mig = new MigrationTestSupport(api, mvc, users, json, jdbc, company);
    mig.accepted("R01", "R02", "R03", "R04", "R05", "P01");
    for (String o : List.of("C01", "F01", "F03")) {
      mig.signMapping(o);
    }
    fx = new LegacyInvoiceFixtures(mig, jdbc);
  }

  private static Map<String, String> row(String no, String type, int seq) {
    Map<String, String> r = new HashMap<>();
    r.put("legacy_invoice_no", no);
    r.put("record_type", type);
    r.put("seq_no", String.valueOf(seq));
    return r;
  }

  @Test
  void theOpenFollowUpOfALegacyInvoiceIsCarriedToItsWorklistItem() throws Exception {
    String t = LegacyInvoiceFixtures.token();
    fx.client(t);
    String no = "I" + String.format("%08d", Long.parseLong(t) * 10 + 5);
    mig.accept(fx.loadInvoice(no, t).get("batchNo").asText());
    String invoiceNo =
        jdbc.queryForObject(
            "select invoice_no from ops_invoice where legacy_invoice_no = ?", String.class, no);

    Map<String, String> disposition = row(no, "DISPOSITION", 1);
    disposition.put("disposition_code", "PTP");
    disposition.put("disposition_date", "2027-12-20");
    disposition.put("remarks", "Client will pay after the holidays");
    Map<String, String> promise = row(no, "PROMISE", 1);
    promise.put("promise_date", "2099-01-15");
    promise.put("promise_amount", "7200.00");
    Map<String, String> assignment = row(no, "ASSIGNMENT", 1);
    assignment.put("collector_user_id", "COL01");
    JsonNode extract =
        mig.upload(
            "F03",
            "F03",
            "F03_CMS_20271231_" + t.substring(t.length() - 2) + ".csv",
            List.of(disposition, promise, assignment),
            List.of("legacy_invoice_no", "record_type", "seq_no"));
    JsonNode batch = mig.load("F03", List.of(extract.get("extractNo").asText()));
    String batchNo = batch.get("batchNo").asText();
    assertThat(batch.get("counts").get("loaded").asInt()).as(mig.rows(batchNo)).isEqualTo(3);

    Map<String, Object> item =
        jdbc.queryForMap(
            "select origin, legacy_ref, source_system, current_handler, promise_status, remarks"
                + " from clx_item where invoice_no = ?",
            invoiceNo);
    assertThat(item.get("origin")).isEqualTo("MIGRATED");
    assertThat(item.get("legacy_ref")).isEqualTo(no);
    assertThat(item.get("current_handler")).isEqualTo("clxhandler");
    assertThat(item.get("promise_status")).isEqualTo("LEGACY");
    assertThat((String) item.get("remarks")).contains("PROMISE_TO_PAY");

    JsonNode migrated =
        mig.get(
            "clxhandler",
            "/api/v1/collections/worklist?origin=MIGRATED&q="
                + invoiceNo
                + "&companyId="
                + company);
    JsonNode rows = migrated.has("content") ? migrated.get("content") : migrated;
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).get("origin").asText()).isEqualTo("MIGRATED");
    assertThat(rows.get(0).get("legacyRef").asText()).isEqualTo(no);
    JsonNode bibs =
        mig.get(
            "clxhandler",
            "/api/v1/collections/worklist?origin=BIBS&q=" + invoiceNo + "&companyId=" + company);
    assertThat(bibs.has("content") ? bibs.get("content") : bibs).isEmpty();
    JsonNode followUp =
        mig.get(
            "clxhandler",
            "/api/v1/collections/items/" + invoiceNo + "/legacy-follow-up?companyId=" + company);
    assertThat(followUp).hasSize(3);

    mig.post("miglead", BASE + "/batches/" + batchNo + "/rollback", Map.of("reason", "Wrong list"));
    mig.post(
        "migrecon", BASE + "/batches/" + batchNo + "/rollback/approve", Map.of("comment", "Ok"));
    assertThat(
            mig.get(
                "clxhandler",
                "/api/v1/collections/items/"
                    + invoiceNo
                    + "/legacy-follow-up?companyId="
                    + company))
        .isEmpty();
  }
}
