package com.iortatechnxt.brokerverse.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Invoice 360 of a migrated invoice: the invoice of a migrated policy sits on an account imported
 * from legacy, which has no account workflow (no work item). The view opens with the legacy invoice
 * card and says the account has no work item, so the screen does not ask for the account's workflow
 * panel.
 */
@IntegrationTest
class MigrationInvoice360ApiIT {

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private ObjectMapper json;
  @Autowired private TestData data;
  @Autowired private JdbcTemplate jdbc;

  private MigrationTestSupport mig;
  private LegacyInvoiceFixtures fx;

  @BeforeEach
  void setUp() throws Exception {
    long company = data.company().getId();
    mig = new MigrationTestSupport(api, mvc, users, json, jdbc, company);
    mig.accepted("R01", "R02", "R03", "R04", "R05");
    for (String o : List.of("C01", "P01", "F01")) {
      mig.signMapping(o);
    }
    fx = new LegacyInvoiceFixtures(mig, jdbc);
  }

  @Test
  void theInvoice360OfAMigratedInvoiceOpensWithoutAnAccountWorkItem() throws Exception {
    String t = LegacyInvoiceFixtures.token();
    fx.client(t);
    String ref = fx.loadHeader(t);
    String no = "I" + String.format("%08d", Long.parseLong(t) * 10 + 7);
    mig.accept(fx.loadInvoice(no, t, ref).get("batchNo").asText());
    String invoiceNo =
        jdbc.queryForObject(
            "select invoice_no from ops_invoice where legacy_invoice_no = ?", String.class, no);

    JsonNode view = mig.get("recon", "/api/v1/ops/invoices/" + invoiceNo);
    assertThat(view.get("invoice").get("origin").asText()).isEqualTo("MIGRATED");
    assertThat(view.get("legacy").get("legacyInvoiceNo").asText()).isEqualTo(no);
    assertThat(view.get("booking").has("bookedInvoiceId")).isFalse();
    assertThat(view.get("accountWorkflow").asBoolean()).isFalse();
    JsonNode accountId = view.get("invoice").get("keys").get("accountId");
    assertThat(accountId.isNumber()).as("the migrated account is linked").isTrue();

    // The account imported from legacy has no work item: the workflow panel would answer 404.
    api.doGet(
            "admin",
            "/api/v1/workflow/cases/by-record?entityType=Account&entityId=" + accountId.asText())
        .andExpect(status().isNotFound());
  }
}
