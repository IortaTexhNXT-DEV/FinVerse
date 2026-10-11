package com.iortatechnxt.brokerverse.renewal.marketing;

import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.ADMIN;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.AO;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.TL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.renewal.RenewalFixtures;
import com.iortatechnxt.brokerverse.renewal.candidate.service.AccountHistoryService;
import com.iortatechnxt.brokerverse.renewal.domain.ReferralStatus;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalReferral;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.marketing.service.PostingApprovers;
import com.iortatechnxt.brokerverse.renewal.marketing.service.ReferralNewBusiness;
import com.iortatechnxt.brokerverse.renewal.marketing.service.ReferralService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RenewalAssignmentService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RenewalAutoAssignment;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RenewalDispositionService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.TransferModes;
import com.iortatechnxt.brokerverse.renewal.marketing.service.TransferService;
import com.iortatechnxt.brokerverse.renewal.service.RenewalStatusNames;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Assignment, transfer requests and submission for posting of BDOI's Renewal FRS against the real
 * database (FRRN.010.01, FRRN.011.01 to FRRN.011.05, FRRN.016.01).
 */
@IntegrationTest
class RenewalAssignmentReferralIT {

  private static final String RECEIVING_TL = "rnwtl";
  private static final String CORPORATE = "T-CORP1";

  @Autowired private RenewalFixtures fx;
  @Autowired private SystemParameterService parameters;
  @Autowired private ReferralService referrals;
  @Autowired private ReferralNewBusiness newBusiness;
  @Autowired private TransferService transfers;
  @Autowired private RenewalAssignmentService assignments;
  @Autowired private AccountHistoryService history;
  @Autowired private RenewalDispositionService dispositions;
  @Autowired private PostingApprovers approvers;
  @Autowired private RenewalStatusNames statusNames;
  @Autowired private AsUser as;

  private void set(String key, String value) {
    as.run(ADMIN, () -> parameters.update(key, value));
  }

  @Test
  void renewalsOfASegmentSetForAutomaticAssignmentGoToAnOfficerOfTheTeam() {
    set(RenewalAutoAssignment.SEGMENTS, "RETAIL");
    try {
      RenewalCandidate c = fx.unassignedRetail();
      assertThat(c.getAssignedAo()).isEqualTo(AO);
      assertThat(c.getStage()).isEqualTo(RenewalStage.FOR_DISPOSITION);
    } finally {
      set(RenewalAutoAssignment.SEGMENTS, "");
    }
    RenewalCandidate manual = fx.unassignedRetail();
    assertThat(manual.getAssignedAo()).isNull();
    assertThat(manual.getStage()).isEqualTo(RenewalStage.UNASSIGNED);
  }

  @Test
  void renewalsRoutedToProcessingAreAssignedInTurnWhenTheSettingIsOn() {
    set(RenewalAutoAssignment.PROCESSING, "true");
    try {
      RenewalCandidate c = fx.extractedMotor();
      as.run(TL, () -> fx.initiate(c));
      RenewalCandidate after = fx.reload(c);
      if (after.getStage() == RenewalStage.FOR_PROCESSING
          || after.getStage() == RenewalStage.IN_PROCESSING) {
        assertThat(after.getAssignedPo()).isNotNull();
      }
    } finally {
      set(RenewalAutoAssignment.PROCESSING, "false");
    }
  }

  @Test
  void aTransferRequestIsAReferralThatKeepsTheOwnerAndOpensANewBusinessAccount() {
    RenewalCandidate c = fx.unassignedRetail();
    String owner = c.getOwnerUnit();
    String ref = c.getRenewalRef();
    assertThatThrownBy(
            () ->
                as.run(
                    TL,
                    () ->
                        referrals.request(
                            fx.company(), ref, new ReferralService.Request(owner, "x", true))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("other than the originating unit");

    RenewalReferral r =
        as.run(
            TL,
            () ->
                referrals.request(
                    fx.company(),
                    ref,
                    new ReferralService.Request(
                        CORPORATE, "Group cover for the client's company", true)));
    assertThat(r.getReferralNo()).startsWith("TRQ-");
    assertThat(r.getStatus()).isEqualTo(ReferralStatus.PENDING_ACCEPTANCE);
    assertThat(fx.reload(c).getOwnerUnit()).isEqualTo(owner);

    assertThatThrownBy(
            () ->
                as.run(
                    RECEIVING_TL,
                    () ->
                        referrals.decide(
                            fx.company(),
                            r.getId(),
                            new ReferralService.Decision(ReferralStatus.REJECTED, " "))))
        .isInstanceOf(BusinessRuleException.class);
    as.run(
        RECEIVING_TL,
        () ->
            referrals.decide(
                fx.company(),
                r.getId(),
                new ReferralService.Decision(ReferralStatus.RETURNED, "Which company?")));
    as.run(TL, () -> referrals.submit(fx.company(), r.getId(), "The client's company, ACME"));
    RenewalReferral accepted =
        as.run(
            RECEIVING_TL,
            () ->
                referrals.decide(
                    fx.company(),
                    r.getId(),
                    new ReferralService.Decision(ReferralStatus.ACCEPTED, null)));
    assertThat(accepted.getStatus()).isEqualTo(ReferralStatus.ACCEPTED);
    assertThat(accepted.getDecidedBy()).isEqualTo(RECEIVING_TL);
    assertThat(accepted.getDecidedAt()).isNotNull();

    var proposed = as.run(RECEIVING_TL, () -> newBusiness.proposal(fx.company(), r.getId()));
    var account =
        as.run(
            RECEIVING_TL,
            () ->
                newBusiness.createNewBusiness(
                    fx.company(),
                    r.getId(),
                    new ReferralNewBusiness.NewBusinessInput(
                        proposed.productCode(),
                        "RETAIL",
                        proposed.periodFrom(),
                        proposed.periodTo(),
                        RECEIVING_TL,
                        false)));
    assertThat(account.getArn()).isNotBlank();
    List<RenewalReferral> monitored = as.run(TL, () -> referrals.list(fx.company()));
    assertThat(monitored)
        .anyMatch(x -> x.getId().equals(r.getId()) && account.getArn().equals(x.getNbArn()));
    assertThat(fx.reload(c).getOwnerUnit()).isEqualTo(owner);
  }

  @Test
  void theOwnershipTransferIsRefusedWhenOnlyTheReferralIsInUse() {
    RenewalCandidate c = fx.unassignedRetail();
    set(TransferModes.MODE, "REFERRAL");
    try {
      assertThatThrownBy(
              () ->
                  as.run(
                      TL,
                      () ->
                          transfers.request(
                              fx.company(),
                              c.getRenewalRef(),
                              new TransferService.Request(CORPORATE, null, "Corporate client"))))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("Transfer to Other Unit");
    } finally {
      set(TransferModes.MODE, "BOTH");
    }
  }

  @Test
  void anAccountSubmittedForPostingIsListedForTheApproverChosen() {
    RenewalCandidate c = fx.unassignedRetail();
    String ref = c.getRenewalRef();
    as.run(TL, () -> assignments.assign(fx.company(), List.of(ref), AO, null));
    as.run(AO, () -> history.open(fx.company(), ref));
    as.run(
        AO,
        () ->
            dispositions.save(
                fx.company(),
                ref,
                new RenewalDispositionService.Input(
                    RenewalDisposition.FOR_RENEWAL, null, null, null, "Client confirmed")));
    var offered = as.run(AO, () -> approvers.of(fx.company(), c.getOwnerUnit()));
    assertThat(offered).extracting(PostingApprovers.Approver::username).contains(TL);
    var outcome = as.run(AO, () -> dispositions.push(fx.company(), List.of(ref), TL));
    assertThat(outcome.refused()).isEmpty();
    RenewalCandidate after = fx.reload(c);
    assertThat(after.getStage()).isEqualTo(RenewalStage.FOR_TL_REVIEW);
    assertThat(after.getPostingApprover()).isEqualTo(TL);
    assertThat(as.run(TL, () -> statusNames.of(after.getStage().name(), false, null)))
        .isEqualTo("Submitted for Posting");
  }
}
