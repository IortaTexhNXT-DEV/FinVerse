package com.iortatechnxt.brokerverse.issuance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.crm.service.ClientRecord;
import com.iortatechnxt.brokerverse.issuance.domain.AdviceStatus;
import com.iortatechnxt.brokerverse.issuance.domain.AdviceTrigger;
import com.iortatechnxt.brokerverse.issuance.domain.Epolicy;
import com.iortatechnxt.brokerverse.issuance.domain.InsuranceAdvice;
import com.iortatechnxt.brokerverse.issuance.service.AdviceDispatchService;
import com.iortatechnxt.brokerverse.issuance.service.AdviceDispatchService.AdviceEmail;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyDispatchService;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyDispatchService.DispatchEmail;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyService;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyService.ReceivedFile;
import com.iortatechnxt.brokerverse.issuance.service.InsuranceAdviceService;
import com.iortatechnxt.brokerverse.issuance.service.IssuanceBatchService;
import com.iortatechnxt.brokerverse.issuance.service.IssuanceBatchService.Outcome;
import com.iortatechnxt.brokerverse.issuance.service.IssuanceClientRecords;
import com.iortatechnxt.brokerverse.issuance.service.IssuanceQueryService;
import com.iortatechnxt.brokerverse.issuance.service.IssuanceQueryService.IssuanceCounts;
import com.iortatechnxt.brokerverse.issuance.service.IssuanceTab;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.PlacementTestData;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/** Insurance Advice (BRNB.060/070/095) and encrypted e-policy dispatch (BRNB.035/077). */
@IntegrationTest
class AdviceAndDispatchIT {

  @Autowired private InsuranceAdviceService advices;
  @Autowired private AdviceDispatchService adviceDispatch;
  @Autowired private IssuanceBatchService batch;
  @Autowired private EpolicyService epolicies;
  @Autowired private EpolicyDispatchService dispatch;
  @Autowired private IssuanceQueryService queries;
  @Autowired private IssuanceClientRecords clientRecords;
  @Autowired private MessageService messages;
  @Autowired private SystemParameterService parameters;
  @Autowired private AccountQueryService accounts;
  @Autowired private PlacementTestData fx;
  @Autowired private AsUser as;

  private Epolicy issued(String arn) {
    String number = "POL-" + PlacementTestData.token();
    Epolicy received =
        as.run(
            "proc",
            () ->
                epolicies.receive(
                    new ReceivedFile(
                        fx.company(), "e.pdf", EpolicyPdf.policy(arn, number), arn, null)));
    return as.run("proc", () -> epolicies.confirm(received.getId(), List.of(number), null));
  }

  @Test
  void adviceIsOnlyForMortgagedAccountsAndIsSentProtected() {
    String mortgaged = fx.placed(fx.fire());
    String plain = fx.placed(fx.liability());
    assertThat(advices.configuredTrigger()).isEqualTo(AdviceTrigger.ON_POLICY_ISSUE);
    assertThat(
            queries.workbench(
                fx.company(), IssuanceTab.IA_TO_GENERATE, mortgaged, PageRequest.of(0, 5)))
        .hasSize(1);
    List<Outcome> outcomes =
        as.run("proc", () -> batch.generateAdvices(List.of(mortgaged, plain, mortgaged)));
    assertThat(outcomes).extracting(Outcome::ok).containsExactly(true, false);
    assertThat(outcomes.get(1).message()).contains("no mortgagee bank");
    InsuranceAdvice advice = advices.ofAccount(mortgaged).get(0);
    assertThat(advice.getIaNo()).startsWith("IA-");
    assertThat(advice.getTriggerEvent()).isEqualTo(AdviceTrigger.MANUAL);
    assertThat(advice.getPolicyNumbers()).isNull();
    assertThat(as.run("proc", () -> advices.download(advice.getId())).getContent()).isNotEmpty();
    assertThat(advices.register(fx.company(), advice.getIaNo(), PageRequest.of(0, 5))).hasSize(1);
    Long clientId = accounts.requireByArn(mortgaged).getClientId();
    assertThat(clientRecords.recordsOf(clientId))
        .extracting(ClientRecord::reference)
        .contains(advice.getIaNo());

    List<InsuranceAdvice> sent =
        as.run(
            "ao",
            () ->
                adviceDispatch.send(
                    new AdviceEmail(
                        List.of(advice.getId()), List.of("hl@bdo.example"), null, "Birth date")));
    assertThat(sent.get(0).getStatus()).isEqualTo(AdviceStatus.SENT);
    assertThat(messages.forRecord(InsuranceAdviceService.ENTITY, String.valueOf(advice.getId())))
        .hasSize(2);
    assertThatThrownBy(
            () -> adviceDispatch.send(new AdviceEmail(List.of(), List.of("x@y.z"), null, null)))
        .extracting("code")
        .isEqualTo("IA_SELECTION");
    String awaiting = fx.awaitingPayment(fx.fire()).getArn();
    assertThatThrownBy(() -> as.run("proc", () -> advices.generate(awaiting, AdviceTrigger.MANUAL)))
        .extracting("code")
        .isEqualTo("IA_ACCOUNT_STATUS");
  }

  @Test
  void theTriggerParameterGeneratesTheAdviceOnPlacement() {
    parameters.update(InsuranceAdviceService.TRIGGER_PARAMETER, "ON_PLACEMENT");
    try {
      String arn = fx.placed(fx.fire());
      assertThat(advices.ofAccount(arn))
          .singleElement()
          .satisfies(ia -> assertThat(ia.getTriggerEvent()).isEqualTo(AdviceTrigger.ON_PLACEMENT));
      issued(arn);
      assertThat(advices.ofAccount(arn)).hasSize(1);
    } finally {
      parameters.update(InsuranceAdviceService.TRIGGER_PARAMETER, "ON_POLICY_ISSUE");
    }
  }

  @Test
  void epoliciesAreSentEncryptedWithThePasswordApart() {
    String arn = fx.placed(fx.liability());
    String unconfirmedArn = fx.placed(fx.liability());
    Epolicy confirmed = issued(arn);
    DispatchEmail draft = dispatch.draft(confirmed.getId());
    assertThat(draft.to()).isNotEmpty();
    assertThat(draft.subject()).contains(confirmed.getPolicyNumberList().get(0));
    assertThat(draft.passwordHint()).isNotBlank();
    Epolicy sent = as.run("epol", () -> dispatch.dispatch(confirmed.getId(), draft));
    assertThat(sent.getDispatchCount()).isEqualTo(1);
    assertThat(dispatch.log(arn, PageRequest.of(0, 10)))
        .anySatisfy(m -> assertThat(m.getPurpose()).isEqualTo(EpolicyDispatchService.PURPOSE));
    assertThatThrownBy(
            () ->
                as.run(
                    "epol",
                    () ->
                        dispatch.dispatch(
                            confirmed.getId(), new DispatchEmail(List.of(), null, "s", "b", null))))
        .extracting("code")
        .isEqualTo("EPOLICY_NO_RECIPIENT");

    Epolicy open =
        as.run(
            "proc",
            () ->
                epolicies.receive(
                    new ReceivedFile(
                        fx.company(), "e.pdf", EpolicyPdf.of("x"), unconfirmedArn, null)));
    List<Outcome> outcomes =
        as.run("epol", () -> batch.dispatch(List.of(confirmed.getId(), open.getId()), " "));
    assertThat(outcomes).extracting(Outcome::ok).containsExactly(true, false);
    assertThat(outcomes.get(1).message()).contains("Confirm the policy data");
    IssuanceCounts counts = queries.counts(fx.company());
    assertThat(counts.toReview()).isPositive();
    assertThat(
            queries.workbench(
                fx.company(), IssuanceTab.AWAITING_POLICY, null, PageRequest.of(0, 5)))
        .isNotEmpty();
    assertThatThrownBy(() -> batch.dispatch(List.of(), null))
        .extracting("code")
        .isEqualTo("ISSUANCE_SELECTION");
  }
}
