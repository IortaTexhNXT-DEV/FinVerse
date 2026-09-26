package com.iortatechnxt.brokerverse.brokerclaims;

import static com.iortatechnxt.brokerverse.brokerclaims.ClaimsFixtures.OFFICER;
import static com.iortatechnxt.brokerverse.brokerclaims.ClaimsFixtures.TL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.brokerclaims.claim.service.BrokerClaimQueryService;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimDetailsService;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimRecordingService;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimRecordingService.NewClaim;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimViewService;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.PremiumCheckService;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.PremiumRecheckJob;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPhase;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPremiumStatus;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimSource;
import com.iortatechnxt.brokerverse.brokerclaims.domain.LossDetails;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Recording a claim on a cover and its premium check (BRCLM.001/003/004/006/009/016/039; wave CL1-A
 * exit criterion 1): a claim on an unpaid cover is recorded and flagged, the authorization code is
 * refused, and after the premium is paid in Cashiering the claim is authorised; the loss rules, the
 * direct-payment policy, the reported date, claimant and cover refresh.
 */
@IntegrationTest
class ClaimRecordingIT {

  @Autowired private ClaimsFixtures fx;
  @Autowired private ClaimRecordingService recording;
  @Autowired private PremiumCheckService premiums;
  @Autowired private BrokerClaimQueryService claims;
  @Autowired private ClaimViewService views;
  @Autowired private ClaimDetailsService details;
  @Autowired private PremiumRecheckJob recheck;
  @Autowired private ClaimRecordedEvents recorded;
  @Autowired private AsUser as;
  @Autowired private JdbcTemplate jdbc;

  private Claim claim(Long id) {
    return claims.require(fx.company(), id);
  }

  @Test
  void aClaimOnAnUnpaidCoverIsBlockedAndAuthorisedAfterPayment() {
    OpsInvoice invoice = fx.motorInvoice();
    Claim recordedClaim = fx.motorClaim(invoice.getArn());
    Long id = recordedClaim.getId();

    assertThat(recordedClaim.getClaimNo()).matches("BCL-\\d{4}-\\d{6}");
    assertThat(recordedClaim.getProgress().getPhase()).isEqualTo(ClaimPhase.NEW);
    assertThat(recordedClaim.getHandler()).isEqualTo(OFFICER);
    assertThat(recordedClaim.getUnitCode()).isEqualTo("MOTOR_HO");
    assertThat(recordedClaim.getCover().getPremiumStatus()).isEqualTo(ClaimPremiumStatus.UNPAID);
    assertThat(recordedClaim.getCover().getPolicyNo()).startsWith("POL-");
    assertThat(recordedClaim.getCover().getCurrency()).isEqualTo("PHP");
    assertThat(recordedClaim.getCover().getCoverVersionNo()).isZero();
    assertThat(recordedClaim.getLoss().getClaimantName()).isEqualTo(invoice.getAssuredName());
    assertThat(recorded.events())
        .anySatisfy(
            e -> {
              assertThat(e.claimId()).isEqualTo(id);
              assertThat(e.initialStatus()).isEqualTo(ClaimRecordingService.DEFAULT_STATUS);
            });
    assertThat(
            jdbc.queryForObject(
                "select count(*) from alt_alert where dedup_key = ?",
                Long.class,
                PremiumCheckService.UNPAID_ALERT + ":" + id))
        .isEqualTo(1L);
    assertThat(
            jdbc.queryForObject(
                "select stage_code from wf_case where entity_type = 'BrokerClaim' and entity_id ="
                    + " ?",
                String.class,
                String.valueOf(id)))
        .isEqualTo("NEW");
    var view = views.view(fx.company(), id);
    assertThat(view.premium().blocking()).isTrue();
    assertThat(view.premium().unpaid())
        .extracting(u -> u.invoiceNo())
        .contains(invoice.getInvoiceNo());

    assertThatThrownBy(() -> as.run(OFFICER, () -> premiums.authorize(fx.company(), id, null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("is not fully paid. The authorization code cannot be generated");

    fx.pay(invoice);
    assertThat(claim(id).getCover().getPremiumStatus()).isEqualTo(ClaimPremiumStatus.PAID);
    Claim authorised = as.run(OFFICER, () -> premiums.authorize(fx.company(), id, null));
    assertThat(authorised.getCover().getAuthorizationCode()).matches("CAC-\\d{4}-\\d{6}");
    assertThat(authorised.getCover().getAuthorizedBy()).isEqualTo(OFFICER);
    assertThatThrownBy(() -> as.run(OFFICER, () -> premiums.authorize(fx.company(), id, null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("already has authorization code");
    assertThat(recheck.execute(LocalDate.now()).message()).contains("re-checked");
  }

  @Test
  void aDirectPaymentCoverNeedsTheInsurerEvidence() {
    OpsInvoice invoice = fx.directPaymentInvoice();
    Claim claim = fx.motorClaim(invoice.getArn());
    assertThat(claim.getCover().getPremiumStatus()).isEqualTo(ClaimPremiumStatus.DIRECT_PAYMENT);
    Long id = claim.getId();
    assertThatThrownBy(() -> as.run(OFFICER, () -> premiums.authorize(fx.company(), id, null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Attach the insurer's payment evidence first");
    Long otherFile = fx.attach("Account", claim.getCover().getAccountId(), "OTHERS");
    assertThatThrownBy(() -> as.run(OFFICER, () -> premiums.authorize(fx.company(), id, otherFile)))
        .isInstanceOf(BusinessRuleException.class);
    Long evidence = fx.attach("BrokerClaim", id, "OTHERS");
    Claim authorised = as.run(OFFICER, () -> premiums.authorize(fx.company(), id, evidence));
    assertThat(authorised.getCover().getDpEvidenceAttachmentId()).isEqualTo(evidence);
  }

  @Test
  void theLossRulesAreCheckedWithTheFrsMessages() {
    OpsInvoice invoice = fx.motorInvoice();
    String arn = invoice.getArn();
    LocalDate today = LocalDate.now();
    assertRefused(
        ClaimsFixtures.request(" ", ClaimsFixtures.loss("MOTOR_OWN_DAMAGE"), List.of(), List.of()),
        "Select the cover of the claim");
    assertRefused(
        ClaimsFixtures.request(
            arn,
            loss(today.plusDays(1), today.plusDays(1), "MOTOR_OWN_DAMAGE"),
            List.of(),
            List.of()),
        "Enter a loss date that is not in the future");
    assertRefused(
        ClaimsFixtures.request(
            arn,
            loss(today.minusDays(3), today.minusDays(4), "MOTOR_OWN_DAMAGE"),
            List.of(),
            List.of()),
        "The reported date must be between the loss date and today");
    assertRefused(
        ClaimsFixtures.request(arn, loss(today.minusDays(3), today, null), List.of(), List.of()),
        "Select the nature of loss");
    LossDetails.Loss beforeCover =
        loss(ClaimsFixtures.coverFrom().minusDays(1), today, "MOTOR_OWN_DAMAGE");
    assertRefused(
        ClaimsFixtures.request(arn, beforeCover, List.of(), List.of()),
        "is outside the cover period");
    NewClaim insurerReported =
        new NewClaim(
            arn,
            1,
            ClaimSource.INSURER_REPORTED,
            null,
            ClaimsFixtures.loss("MOTOR_THEFT"),
            new LossDetails.Amounts(null, null, null),
            List.of(),
            List.of(),
            false,
            false);
    assertRefused(insurerReported, "insurer's claim number of an insurer-reported claim");

    NewClaim confirmed =
        new NewClaim(
            arn,
            1,
            ClaimSource.BDOI_NOTICE,
            "NEW_COMPLETE_DOCS",
            beforeCover,
            new LossDetails.Amounts(null, null, null),
            List.of(),
            List.of(),
            true,
            false);
    Claim claim = as.run(OFFICER, () -> recording.record(fx.company(), confirmed));
    assertThat(claim.getLoss().getLossDate()).isEqualTo(ClaimsFixtures.coverFrom().minusDays(1));
  }

  @Test
  void theReportedDateClaimantAndCoverDataChangeWithAReason() {
    OpsInvoice invoice = fx.motorInvoice();
    Long id = fx.motorClaim(invoice.getArn()).getId();
    Long company = fx.company();
    LocalDate earlier = LocalDate.now().minusDays(2);

    assertThatThrownBy(
            () -> as.run(TL, () -> details.correctReportedDate(company, id, earlier, " ", null)))
        .hasMessage("Enter the reason for the change");
    assertThatThrownBy(
            () ->
                as.run(
                    TL,
                    () ->
                        details.correctReportedDate(
                            company, id, LocalDate.now().plusDays(1), "DATA_CORRECTION", null)))
        .hasMessage("The reported date must be between the loss date and today");
    Claim corrected =
        as.run(
            TL, () -> details.correctReportedDate(company, id, earlier, "DATA_CORRECTION", "Typo"));
    assertThat(corrected.getLoss().getReportedDate()).isEqualTo(earlier);

    assertThatThrownBy(() -> as.run(TL, () -> details.overrideClaimant(company, id, " ", "OTHERS")))
        .hasMessage("Enter the claimant's name");
    Claim overridden =
        as.run(
            TL,
            () -> details.overrideClaimant(company, id, "Juan Dela Cruz (third party)", "OTHERS"));
    assertThat(overridden.getLoss().isClaimantOverridden()).isTrue();
    assertThat(overridden.getLoss().getClaimantName()).isEqualTo("Juan Dela Cruz (third party)");

    Claim amended =
        as.run(
            OFFICER,
            () ->
                details.amendLoss(
                    company,
                    id,
                    new LossDetails.Loss(
                        earlier,
                        null,
                        "MOTOR_OWN_DAMAGE",
                        "MOTOR_OWN_DAMAGE",
                        "Hit a post",
                        null,
                        "TYPHOON",
                        "Typhoon Kristine"),
                    new LossDetails.Amounts(new BigDecimal("80000.00"), null, null)));
    assertThat(amended.getLoss().isCatastrophe()).isTrue();
    assertThatThrownBy(
            () ->
                as.run(
                    OFFICER,
                    () ->
                        details.amendLoss(
                            company,
                            id,
                            new LossDetails.Loss(
                                earlier,
                                null,
                                "MOTOR_OWN_DAMAGE",
                                "MOTOR_OWN_DAMAGE",
                                "x",
                                null,
                                null,
                                "Kristine"),
                            new LossDetails.Amounts(null, null, null))))
        .hasMessage("Select the catastrophe code of the event");

    assertThat(as.run(OFFICER, () -> details.refreshCover(company, id))).isEmpty();
    assertThatThrownBy(() -> as.run(OFFICER, () -> details.useLatestVersion(company, id)))
        .hasMessageContaining("latest cover version");
    assertThat(
            jdbc.queryForList(
                "select summary from audit_log where entity_type = 'BrokerClaim' and entity_id = ?",
                String.class,
                claim(id).getClaimNo()))
        .anyMatch(s -> s.startsWith("Reported date"))
        .anyMatch(s -> s.startsWith("Claimant"));

    jdbc.update("update bcl_claim set phase = 'CLOSED' where id = ?", id);
    assertThatThrownBy(
            () ->
                as.run(
                    TL,
                    () ->
                        details.correctReportedDate(company, id, earlier, "DATA_CORRECTION", null)))
        .hasMessage("The reported date of a closed claim cannot change");
  }

  private static LossDetails.Loss loss(LocalDate lossDate, LocalDate reported, String nature) {
    return new LossDetails.Loss(
        lossDate, reported, nature, "MOTOR_OWN_DAMAGE", "Loss", null, null, null);
  }

  private void assertRefused(NewClaim request, String message) {
    assertThatThrownBy(() -> as.run(OFFICER, () -> recording.record(fx.company(), request)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining(message);
  }
}
