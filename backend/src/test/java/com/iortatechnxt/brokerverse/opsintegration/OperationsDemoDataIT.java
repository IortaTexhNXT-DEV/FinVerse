package com.iortatechnxt.brokerverse.opsintegration;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.adjustment.demo.AdjustmentDemoData;
import com.iortatechnxt.brokerverse.cashiering.demo.CashieringDemoData;
import com.iortatechnxt.brokerverse.commission.demo.CommissionDemoData;
import com.iortatechnxt.brokerverse.opsledger.demo.OpsLedgerDemoReplay;
import com.iortatechnxt.brokerverse.opsledger.service.OperationsHomeService;
import com.iortatechnxt.brokerverse.opsledger.service.OperationsHomeService.SectionCounts;
import com.iortatechnxt.brokerverse.prodrecon.demo.ProdReconDemoData;
import com.iortatechnxt.brokerverse.remittance.demo.RemittanceDemoData;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.TestData;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Loads the demo profile in its own database and checks the Operations storyline: every Operations
 * screen has real data after start-up, each record was made by the demo user who would do the work,
 * and the runners change nothing when the application starts again.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "demo"})
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
class OperationsDemoDataIT {

  private static final List<String> COUNTED =
      List.of(
          "csh_payment",
          "csh_receipt",
          "csh_unapplied",
          "csh_cwt_tag",
          "rem_extraction_run",
          "rem_batch",
          "rem_hold_request",
          "ops_disbursement_request",
          "adj_request",
          "prc_extract",
          "prc_item",
          "cmr_dp_item",
          "cmr_billing");

  @Autowired private JdbcTemplate jdbc;
  @Autowired private OperationsHomeService home;
  @Autowired private TestData data;
  @Autowired private AsUser as;
  @Autowired private OpsLedgerDemoReplay replay;
  @Autowired private CashieringDemoData cashiering;
  @Autowired private RemittanceDemoData remittance;
  @Autowired private AdjustmentDemoData adjustment;
  @Autowired private ProdReconDemoData prodrecon;
  @Autowired private CommissionDemoData commission;

  private long count(String sql, Object... args) {
    Long n = jdbc.queryForObject(sql, Long.class, args);
    return n == null ? 0 : n;
  }

  private List<String> values(String sql) {
    return jdbc.queryForList(sql, String.class);
  }

  @Test
  void everyOperationsScreenHasTheStorylineAfterStartUp() {
    assertThat(values("select distinct payment_status from ops_invoice"))
        .contains("PAID", "PARTIALLY_PAID", "UNPAID", "NOT_APPLICABLE");
    assertThat(values("select distinct stage from csh_unapplied")).isNotEmpty();
    assertThat(count("select count(*) from csh_cwt_tag")).isPositive();
    assertThat(count("select count(*) from csh_prebooked")).isPositive();
    assertThat(values("select stage from rem_batch")).contains("OR_RECEIVED", "REVIEW_IN_PROCESS");
    assertThat(values("select or_status from rem_batch_line where or_status is not null"))
        .contains("MATCHED");
    assertThat(values("select stage from rem_hold_request")).contains("FOR_APPROVAL");
    assertThat(values("select status from ops_disbursement_request")).contains("DV_ASSIGNED");
    assertThat(values("select stage from adj_request"))
        .contains("DRAFT", "FOR_VALIDATION", "RETURNED", "FOR_APPROVAL", "FOR_POSTING", "POSTED");
    assertThat(values("select stage from prc_cycle")).contains("RECONCILING");
    assertThat(values("select status from prc_item"))
        .contains("MATCHED", "MATCHED_WITH_DISCREPANCY", "UNMATCHED_NO_BOOKING");
    assertThat(values("select stage from cmr_billing")).contains("AWAITING_INSURER");
    assertThat(values("select feed_code from ops_flow_in_run"))
        .contains("OPS_INVOICE_FEED", "INSURER_REMIT_OR", "INSURER_PRODUCTION");
  }

  @Test
  void eachStepWasDoneByTheDemoUserWhoseJobItIs() {
    assertThat(values("select distinct created_by from csh_payment")).containsOnly("cashier");
    assertThat(values("select distinct created_by from rem_hold_request")).containsOnly("mktcoll");
    assertThat(values("select distinct created_by from rem_batch")).containsOnly("remit");
    assertThat(values("select distinct approved_by from rem_batch where approved_by is not null"))
        .containsOnly("remittl");
    assertThat(values("select distinct created_by from adj_request")).containsOnly("mktcoll");
    assertThat(values("select distinct created_by from prc_cycle")).containsOnly("recon");
    assertThat(values("select distinct created_by from cmr_billing")).containsOnly("commrec");
  }

  @Test
  void theOperationsHomeCountsTheWaitingWork() {
    Long company = data.company().getId();
    Map<String, Long> remit = tiles(as.run("remittl", () -> home.sections(company)));
    assertThat(remit).containsKey("Batches in Review");
    assertThat(remit.get("Batches in Review")).isPositive();
    Map<String, Long> adj = tiles(as.run("adjtl", () -> home.sections(company)));
    assertThat(adj.get("Endorsements for Approval")).isPositive();
    assertThat(adj.get("Endorsements for Posting")).isPositive();
    Map<String, Long> cash = tiles(as.run("cashtl", () -> home.sections(company)));
    assertThat(cash.get("Unapplied Payments")).isPositive();
    Map<String, Long> recon = tiles(as.run("recon", () -> home.sections(company)));
    assertThat(recon.get("Cycles Reconciling")).isPositive();
    Map<String, Long> comm = tiles(as.run("commrec", () -> home.sections(company)));
    assertThat(comm.get("Billings Awaiting Insurer")).isPositive();
  }

  @Test
  void theRunnersChangeNothingWhenTheApplicationStartsAgain() throws Exception {
    Map<String, Long> before = counts();
    var args = new DefaultApplicationArguments();
    replay.run(args);
    cashiering.run(args);
    remittance.run(args);
    adjustment.run(args);
    prodrecon.run(args);
    commission.run(args);
    assertThat(counts()).isEqualTo(before);
  }

  private Map<String, Long> counts() {
    Map<String, Long> counts = new LinkedHashMap<>();
    COUNTED.forEach(t -> counts.put(t, count("select count(*) from " + t)));
    return counts;
  }

  private static Map<String, Long> tiles(List<SectionCounts> sections) {
    Map<String, Long> tiles = new LinkedHashMap<>();
    sections.forEach(s -> s.counts().forEach(c -> tiles.merge(c.label(), c.count(), Long::sum)));
    return tiles;
  }
}
