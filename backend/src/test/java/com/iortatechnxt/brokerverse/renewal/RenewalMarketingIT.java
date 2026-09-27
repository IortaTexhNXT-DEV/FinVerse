package com.iortatechnxt.brokerverse.renewal;

import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.AO;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.TL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.service.QuotationQueryService;
import com.iortatechnxt.brokerverse.renewal.candidate.service.AccountHistoryService;
import com.iortatechnxt.brokerverse.renewal.domain.OverrideKind;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalTransfer;
import com.iortatechnxt.brokerverse.renewal.marketing.service.NbPathService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.OverrideService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RenewalAssignmentService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RenewalDispositionService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RenewalDispositionService.Input;
import com.iortatechnxt.brokerverse.renewal.marketing.service.ReviewService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.TransferService;
import com.iortatechnxt.brokerverse.renewal.service.BatchOutcome;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Renewal wave R1-B: assignment, the account-history gate, disposition, push, return and post,
 * transfers between units, overrides and the New Business path.
 */
@IntegrationTest
class RenewalMarketingIT {

  @Autowired private RenewalFixtures fx;
  @Autowired private RenewalAssignmentService assignments;
  @Autowired private AccountHistoryService history;
  @Autowired private RenewalDispositionService dispositions;
  @Autowired private ReviewService review;
  @Autowired private TransferService transfers;
  @Autowired private OverrideService overrides;
  @Autowired private NbPathService nbPath;
  @Autowired private QuotationQueryService quotations;
  @Autowired private AsUser as;

  private RenewalCandidate assigned() {
    RenewalCandidate c = fx.unassignedRetail();
    assertThat(c.getStage()).isEqualTo(RenewalStage.UNASSIGNED);
    BatchOutcome outcome =
        as.run(TL, () -> assignments.assign(fx.company(), List.of(c.getRenewalRef()), AO, null));
    assertThat(outcome.refused()).isEmpty();
    return fx.reload(c);
  }

  private void dispose(RenewalCandidate c, RenewalDisposition code) {
    as.run(AO, () -> history.open(fx.company(), c.getRenewalRef()));
    as.run(
        AO,
        () ->
            dispositions.save(
                fx.company(), c.getRenewalRef(), new Input(code, null, null, null, "OK")));
    BatchOutcome pushed =
        as.run(AO, () -> dispositions.push(fx.company(), List.of(c.getRenewalRef())));
    assertThat(pushed.refused()).isEmpty();
  }

  @Test
  void assignDisposeReturnAndPostMovesTheRenewalToProcessing() {
    RenewalCandidate c = assigned();
    assertThat(c.getStage()).isEqualTo(RenewalStage.FOR_DISPOSITION);
    assertThat(c.getAssignedAo()).isEqualTo(AO);

    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () ->
                        dispositions.save(
                            fx.company(),
                            c.getRenewalRef(),
                            new Input(RenewalDisposition.FOR_RENEWAL, null, null, null, null))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("Account History");
    dispose(c, RenewalDisposition.FOR_RENEWAL);
    assertThat(fx.reload(c).getStage()).isEqualTo(RenewalStage.FOR_TL_REVIEW);

    BatchOutcome returned =
        as.run(
            TL,
            () ->
                review.returnToAo(
                    fx.company(),
                    List.of(c.getRenewalRef()),
                    "INCOMPLETE_DETAILS",
                    "Check the SI"));
    assertThat(returned.refused()).isEmpty();
    RenewalCandidate back = fx.reload(c);
    assertThat(back.getStage()).isEqualTo(RenewalStage.FOR_DISPOSITION);
    assertThat(back.getFlags().isReturned()).isTrue();

    BatchOutcome pushed =
        as.run(AO, () -> dispositions.push(fx.company(), List.of(c.getRenewalRef())));
    assertThat(pushed.refused()).isEmpty();
    assertThat(fx.reload(c).getFlags().isReturned()).isFalse();
    BatchOutcome blocked = as.run(TL, () -> review.post(fx.company(), List.of(c.getRenewalRef())));
    assertThat(blocked.refused()).containsKey(c.getRenewalRef());
    assertThat(blocked.refused().get(c.getRenewalRef())).contains("Outstanding premium");
    BatchOutcome overridden =
        as.run(
            TL,
            () ->
                overrides.override(
                    fx.company(),
                    List.of(c.getRenewalRef()),
                    new OverrideService.Request(
                        OverrideKind.OUTSTANDING_BALANCE,
                        null,
                        "OTHERS",
                        "Client pays at renewal")));
    assertThat(overridden.refused()).isEmpty();
    BatchOutcome posted = as.run(TL, () -> review.post(fx.company(), List.of(c.getRenewalRef())));
    assertThat(posted.refused()).isEmpty();
    assertThat(fx.reload(c).getStage()).isEqualTo(RenewalStage.FOR_PROCESSING);
  }

  @Test
  void notForRenewalNeedsAReasonAndGoesToTheLetterStep() {
    RenewalCandidate c = assigned();
    as.run(AO, () -> history.open(fx.company(), c.getRenewalRef()));
    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () ->
                        dispositions.save(
                            fx.company(),
                            c.getRenewalRef(),
                            new Input(RenewalDisposition.NOT_FOR_RENEWAL, null, null, null, null))))
        .isInstanceOf(BusinessRuleException.class);
    as.run(
        AO,
        () ->
            dispositions.save(
                fx.company(),
                c.getRenewalRef(),
                new Input(RenewalDisposition.NOT_FOR_RENEWAL, "UNIT_SOLD", null, null, "Sold")));
    as.run(AO, () -> dispositions.push(fx.company(), List.of(c.getRenewalRef())));
    as.run(TL, () -> review.post(fx.company(), List.of(c.getRenewalRef())));
    assertThat(fx.reload(c).getStage()).isEqualTo(RenewalStage.LETTER_PENDING);

    as.run(AO, () -> dispositions.reopen(fx.company(), c.getRenewalRef(), "Client changed mind"));
    assertThat(fx.reload(c).getStage()).isEqualTo(RenewalStage.FOR_DISPOSITION);
  }

  @Test
  void aTransferIsAcceptedIntoTheReceivingUnit() {
    RenewalCandidate c = fx.unassignedRetail();
    RenewalTransfer t =
        as.run(
            TL,
            () ->
                transfers.request(
                    fx.company(),
                    c.getRenewalRef(),
                    new TransferService.Request("T-CORP1", "WRONG_UNIT", "Corporate client")));
    assertThat(fx.reload(c).getStage()).isEqualTo(RenewalStage.TRANSFER_PENDING);
    assertThatThrownBy(
            () ->
                as.run(
                    TL,
                    () ->
                        transfers.request(
                            fx.company(),
                            c.getRenewalRef(),
                            new TransferService.Request("T-CORP1", null, "Again"))))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> as.run(TL, () -> transfers.decide(t.getId(), true, null)))
        .isInstanceOf(BusinessRuleException.class);
    as.run("rnwtl", () -> transfers.decide(t.getId(), true, null));
    RenewalCandidate after = fx.reload(c);
    assertThat(after.getStage()).isEqualTo(RenewalStage.UNASSIGNED);
    assertThat(after.getOwnerUnit()).isEqualTo("T-CORP1");
    assertThat(after.getFlags().isTransferred()).isTrue();
  }

  @Test
  void anOverrideNeedsRemarksAndNeverGivesCleanOverAFailedCheck() {
    RenewalCandidate c = assigned();
    assertThatThrownBy(
            () ->
                as.run(
                    TL,
                    () ->
                        overrides.override(
                            fx.company(),
                            List.of(c.getRenewalRef()),
                            new OverrideService.Request(
                                OverrideKind.BUCKET, "REVIEW", "OTHERS", " "))))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void theNewBusinessPathCreatesAQuotationLinkedToTheRenewal() {
    RenewalCandidate c = assigned();
    dispose(c, RenewalDisposition.FOR_QUOTATION);
    as.run(
        TL,
        () ->
            overrides.override(
                fx.company(),
                List.of(c.getRenewalRef()),
                new OverrideService.Request(
                    OverrideKind.OUTSTANDING_BALANCE, null, "OTHERS", "Paid with the new policy")));
    as.run(TL, () -> review.post(fx.company(), List.of(c.getRenewalRef())));
    assertThat(fx.reload(c).getStage()).isEqualTo(RenewalStage.NB_PATH);
    String number = as.run(AO, () -> nbPath.start(fx.company(), c.getRenewalRef(), null));
    Quotation q = as.run(AO, () -> quotations.getByRenewalRef(c.getRenewalRef()).orElseThrow());
    assertThat(q.getQuotationNo()).isEqualTo(number);
    assertThat(q.getRenewalOfRef()).isEqualTo(c.getExpiringArn());
    assertThat(fx.reload(c).getQuotationRef()).isEqualTo(number);
    assertThatThrownBy(() -> as.run(AO, () -> nbPath.start(fx.company(), c.getRenewalRef(), null)))
        .isInstanceOf(BusinessRuleException.class);
  }
}
