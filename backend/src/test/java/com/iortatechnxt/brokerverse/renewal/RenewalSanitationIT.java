package com.iortatechnxt.brokerverse.renewal;

import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.ADMIN;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.PROC_TL;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.TL;
import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateQueryService;
import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.CheckOutcome;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResult;
import com.iortatechnxt.brokerverse.renewal.domain.CheckTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerRenewableRisk;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.rules.service.BdoiSanitation;
import com.iortatechnxt.brokerverse.renewal.rules.service.ReevaluationService;
import com.iortatechnxt.brokerverse.renewal.setup.service.InsurerRenewableListService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The sanitation criteria of the Walkthrough addendum (Annex BRRN.020; FR-RN-020, 022, 023): the
 * TSI threshold with the proposal for TSU or proposal handling (the Review setting of the TSI
 * rule), and the insurer renewable list.
 */
@IntegrationTest
class RenewalSanitationIT {

  @Autowired private RenewalFixtures fx;
  @Autowired private CandidateQueryService queries;
  @Autowired private ReevaluationService reevaluation;
  @Autowired private RenewalCandidateRepository candidates;
  @Autowired private InsurerRenewableListService lists;
  @Autowired private AsUser as;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private TransactionTemplate tx;
  @Autowired private SystemParameterService parameters;

  private CheckOutcome outcome(RenewalCandidate c, String check) {
    return as.run(PROC_TL, () -> queries.latestResults(fx.reload(c))).stream()
        .filter(r -> r.getCheckCode().equals(check))
        .map(CheckResult::getOutcome)
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
  void aSumInsuredAboveTheThresholdIsReviewedAndProposedForProposal() {
    RenewalCandidate c = fx.extractedMotor();
    assertThat(outcome(c, "TSI_THRESHOLD")).isEqualTo(CheckOutcome.PASS);
    as.run("badmin", () -> parameters.update(BdoiSanitation.TSI_ROUTE, "REVIEW"));
    try {
      jdbc.update(
          "update rnw_check_setting set parameters = '1000' where check_code = 'TSI_THRESHOLD'");
      as.run(TL, () -> fx.initiate(c));
      RenewalCandidate after = fx.reload(c);
      assertThat(outcome(c, "TSI_THRESHOLD")).isEqualTo(CheckOutcome.FAIL);
      assertThat(after.getBucket()).isNotEqualTo(Bucket.CLEAN);
      if (after.getBucket() == Bucket.REVIEW) {
        assertThat(after.getProposal().disposition()).isEqualTo(RenewalDisposition.FOR_PROPOSAL);
        assertThat(after.getProposal().isAuto()).isFalse();
      }
    } finally {
      as.run("badmin", () -> parameters.update(BdoiSanitation.TSI_ROUTE, "QUOTATION"));
      jdbc.update(
          "update rnw_check_setting set parameters = '250000000'"
              + " where check_code = 'TSI_THRESHOLD'");
    }
  }

  @Test
  void aRiskOutsideTheInsurersRenewableListIsHeldForReview() {
    RenewalCandidate c = fx.extractedMotor();
    String insurer = c.getSnapshot().insurerCode();
    assertThat(outcome(c, "INSURER_RENEWABLE_LIST")).isEqualTo(CheckOutcome.NOT_APPLICABLE);
    LocalDate from = LocalDate.of(2020, 1, 1);
    InsurerRenewableRisk fire =
        as.run(
            ADMIN,
            () ->
                lists.create(
                    fx.company(),
                    new InsurerRenewableRisk.Data(insurer, "PAR01", "Fire only", from, null)));
    InsurerRenewableRisk motor = null;
    try {
      as.run("approver", () -> lists.authorize(fx.company(), fire.getId()));
      reevaluate(c);
      assertThat(outcome(c, "INSURER_RENEWABLE_LIST")).isEqualTo(CheckOutcome.FAIL);
      assertThat(fx.reload(c).getBucket()).isNotEqualTo(Bucket.CLEAN);

      motor =
          as.run(
              ADMIN,
              () ->
                  lists.create(
                      fx.company(),
                      new InsurerRenewableRisk.Data(insurer, "MTR10", null, from, null)));
      Long motorId = motor.getId();
      as.run("approver", () -> lists.authorize(fx.company(), motorId));
      reevaluate(c);
      assertThat(outcome(c, "INSURER_RENEWABLE_LIST")).isEqualTo(CheckOutcome.PASS);
      assertThat(as.run(PROC_TL, () -> lists.list(fx.company())))
          .extracting(InsurerRenewableRisk::getRiskCode)
          .contains("PAR01", "MTR10");
    } finally {
      for (InsurerRenewableRisk row : motor == null ? List.of(fire) : List.of(fire, motor)) {
        as.run(ADMIN, () -> lists.deactivate(fx.company(), row.getId()));
      }
    }
  }
}
