package com.iortatechnxt.brokerverse.renewal.rules;

import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.ADMIN;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.PROC_TL;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.TL;
import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.renewal.RenewalFixtures;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateQueryService;
import com.iortatechnxt.brokerverse.renewal.check.service.RenewalDuplicates;
import com.iortatechnxt.brokerverse.renewal.check.service.TsiThresholdCheck;
import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.CheckOutcome;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResult;
import com.iortatechnxt.brokerverse.renewal.domain.CheckTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.ExtractionTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.extraction.service.ManualCreationService;
import com.iortatechnxt.brokerverse.renewal.rules.service.ReevaluationService;
import com.iortatechnxt.brokerverse.renewal.rules.service.RenewalUpdatesService;
import com.iortatechnxt.brokerverse.renewal.rules.service.RenewalUpdatesService.ValueRow;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * BDOI's sanitation rules, duplicate checking and automatic updates against the real database
 * (FRRN.004.04, FRRN.004.06, FRRN.005, FRRN.007, FRRN.008.01, FRRN.009.01): the total sum insured
 * above the threshold For Quotation, the total loss claim, the renewal account created by hand with
 * the duplicate warning, the refresh of the endorsements and the CBG Motor automatic values.
 */
@IntegrationTest
class RenewalSanitationRulesIT {

  @Autowired private RenewalFixtures fx;
  @Autowired private CandidateQueryService queries;
  @Autowired private ReevaluationService reevaluation;
  @Autowired private RenewalCandidateRepository candidates;
  @Autowired private ManualCreationService manual;
  @Autowired private RenewalUpdatesService updates;
  @Autowired private SystemParameterService parameters;
  @Autowired private AsUser as;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private TransactionTemplate tx;

  private CheckResult result(RenewalCandidate c, String check) {
    return as.run(PROC_TL, () -> queries.latestResults(fx.reload(c))).stream()
        .filter(r -> r.getCheckCode().equals(check))
        .findFirst()
        .orElseThrow();
  }

  private void reevaluate(RenewalCandidate c) {
    tx.executeWithoutResult(
        s ->
            reevaluation.reevaluate(
                candidates.findById(c.getId()).orElseThrow(), CheckTrigger.NIGHTLY));
  }

  @Test
  void aSumInsuredAboveTheThresholdIsForQuotationWithTheTsu() {
    RenewalCandidate c = fx.extractedMotor();
    try {
      jdbc.update(
          "update rnw_check_setting set parameters = '1000' where check_code = 'TSI_THRESHOLD'");
      as.run(TL, () -> fx.initiate(c));
      RenewalCandidate after = fx.reload(c);
      CheckResult tsi = result(c, TsiThresholdCheck.CODE);
      assertThat(tsi.getOutcome()).isEqualTo(CheckOutcome.INFO);
      assertThat(tsi.getMessage()).contains("For Quotation");
      if (after.getBucket() == Bucket.CLEAN) {
        assertThat(after.getDisposition().code()).isEqualTo(RenewalDisposition.FOR_QUOTATION);
      }
    } finally {
      jdbc.update(
          "update rnw_check_setting set parameters = '250000000'"
              + " where check_code = 'TSI_THRESHOLD'");
    }
  }

  @Test
  void aTotalLossClaimOnTheExpiringTermMakesTheRenewalNotForRenewal() {
    RenewalCandidate c = fx.extractedMotor();
    jdbc.update(
        "insert into bcl_claim (company_id, claim_no, handler, source, arn, policy_year, currency,"
            + " loss_date, reported_date, phase, settlement_type_code, created_at, created_by)"
            + " values (?, ?, 'ao', 'MIGRATED', ?, ?, 'PHP', ?, ?, 'CLOSED',"
            + " 'SETTLED_TOTAL_LOSS', now(), 'TEST')",
        fx.company(),
        "TL-" + c.getId(),
        c.getExpiringArn(),
        c.getPolicyYear(),
        LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 2));
    as.run(TL, () -> fx.initiate(c));
    reevaluate(c);
    CheckResult loss = result(c, "TOTAL_LOSS");
    assertThat(loss.getOutcome()).isEqualTo(CheckOutcome.FAIL);
    assertThat(loss.getMessage()).contains("TL-" + c.getId());
    RenewalCandidate after = fx.reload(c);
    assertThat(after.getDisposition().code()).isEqualTo(RenewalDisposition.NOT_FOR_RENEWAL);
    assertThat(after.getDisposition().reasonCode()).isEqualTo(RenewalCodes.REASON_TOTAL_LOSS);
  }

  @Test
  void aRenewalAccountCreatedByHandWarnsOfAPotentialDuplicateFirst() {
    BookedInvoice first = fx.book("MTR10", "RETAIL");
    BookedInvoice second = fx.book("MTR10", "RETAIL");
    RenewalCandidate existing = fx.extracted(first);
    as.run(ADMIN, () -> parameters.update(RenewalDuplicates.CRITERIA, "*:CLIENT+RISK_CODE"));
    as.run(ADMIN, () -> parameters.update(RenewalDuplicates.MIN_MATCH, "2"));
    try {
      var warning = as.run(TL, () -> manual.create(fx.company(), second.getInvoiceNo(), false));
      assertThat(warning.renewalRef()).isNull();
      assertThat(warning.message()).isEqualTo(ManualCreationService.POTENTIAL);
      assertThat(warning.duplicates()).anyMatch(m -> existing.getRenewalRef().equals(m.ref()));

      var created = as.run(TL, () -> manual.create(fx.company(), second.getInvoiceNo(), true));
      assertThat(created.renewalRef()).isNotNull();
      RenewalCandidate c =
          candidates.findByCompanyIdAndRenewalRef(fx.company(), created.renewalRef()).orElseThrow();
      assertThat(c.getExpiringInvoiceNo()).isEqualTo(second.getInvoiceNo());
      assertThat(
              jdbc.queryForObject(
                  "select r.trigger_kind from rnw_extraction_run r join rnw_candidate c"
                      + " on c.extraction_run_id = r.id where c.id = ?",
                  String.class,
                  c.getId()))
          .isEqualTo(ExtractionTrigger.MANUAL_ACCOUNT.name());

      var again = as.run(TL, () -> manual.create(fx.company(), second.getInvoiceNo(), true));
      assertThat(again.renewalRef()).isNull();
      assertThat(again.duplicates()).allMatch(RenewalDuplicates.Match::exact);
    } finally {
      as.run(ADMIN, () -> parameters.update(RenewalDuplicates.CRITERIA, RenewalDuplicates.DEFAULT));
      as.run(ADMIN, () -> parameters.update(RenewalDuplicates.MIN_MATCH, "1"));
    }
  }

  @Test
  void refreshEndorsementsRerunsTheChecksAndListsTheEndorsementsOfTheTerm() {
    RenewalCandidate c = fx.extractedMotor();
    var rows = as.run(TL, () -> updates.refreshEndorsements(fx.company(), c.getRenewalRef()));
    assertThat(rows).isNotNull();
    assertThat(result(c, "ENDORSEMENT_PENDING").getOutcome())
        .isIn(CheckOutcome.PASS, CheckOutcome.WARN, CheckOutcome.NOT_APPLICABLE);
  }

  @Test
  void aCbgMotorRenewalShowsTheAutomaticValuesNextToTheExpiringOnes() {
    RenewalCandidate c = fx.extractedMotor();
    List<ValueRow> rows = as.run(TL, () -> updates.autoUpdate(fx.company(), c.getRenewalRef()));
    assertThat(rows).extracting(ValueRow::field).contains("OD / Theft Coverage", "Total Premium");
    ValueRow od =
        rows.stream()
            .filter(r -> r.field().equals("OD / Theft Coverage"))
            .findFirst()
            .orElseThrow();
    assertThat(new BigDecimal(od.renewal()))
        .isEqualByComparingTo(new BigDecimal(od.expiring()).multiply(new BigDecimal("0.90")));
    assertThat(od.difference()).isNegative();

    RenewalCandidate retail = fx.extracted(fx.book("MTR10", "RETAIL"));
    assertThat(as.run(TL, () -> updates.autoUpdate(fx.company(), retail.getRenewalRef())))
        .isEmpty();
  }
}
