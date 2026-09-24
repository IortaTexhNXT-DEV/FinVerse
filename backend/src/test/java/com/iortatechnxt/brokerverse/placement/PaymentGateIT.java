package com.iortatechnxt.brokerverse.placement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.placement.domain.EvidenceKind;
import com.iortatechnxt.brokerverse.placement.domain.GateRule;
import com.iortatechnxt.brokerverse.placement.domain.PaymentEvidence;
import com.iortatechnxt.brokerverse.placement.service.ConfirmedPayment;
import com.iortatechnxt.brokerverse.placement.service.PaymentConfirmationSweep;
import com.iortatechnxt.brokerverse.placement.service.PaymentConfirmationSweepJob;
import com.iortatechnxt.brokerverse.placement.service.PaymentGateService;
import com.iortatechnxt.brokerverse.placement.service.PaymentGateService.ClientConfirmation;
import com.iortatechnxt.brokerverse.placement.service.PaymentGateService.GateView;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.PlacementTestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** The payment gate: rules per segment, client confirmation, confirmed payments (BRD 2.3.1). */
@IntegrationTest
class PaymentGateIT {

  @Autowired private PaymentGateService gate;
  @Autowired private PaymentConfirmationSweep sweep;
  @Autowired private PaymentConfirmationSweepJob sweepJob;
  @Autowired private AccountQueryService queries;
  @Autowired private PlacementTestData fx;
  @Autowired private AsUser as;

  @Test
  void theRuleFollowsTheSegmentAndLine() {
    Account fire = fx.awaitingPayment(fx.fire());
    Account motor = fx.awaitingPayment(fx.motor());
    Account liability = fx.awaitingPayment(fx.liability());
    assertThat(gate.ruleFor(fire).rule()).isEqualTo(GateRule.PAYMENT_MATCHED);
    assertThat(gate.ruleFor(motor).rule()).isEqualTo(GateRule.PAYMENT_MATCHED);
    assertThat(gate.ruleFor(liability).rule()).isEqualTo(GateRule.CLIENT_CONFIRMATION);
    assertThat(gate.rules()).extracting(r -> r.getRule()).contains(GateRule.PAYMENT_MATCHED);
    GateView view = gate.view(fire.getArn());
    assertThat(view.open()).isFalse();
    assertThat(view.evidence()).isEmpty();
    assertThat(gate.ruleFor(queries.requireByArn("ARN-2026-900006")).rule())
        .isEqualTo(GateRule.DIRECT_PAYMENT);
  }

  @Test
  void otherLinesOpenOnARecordedClientConfirmation() {
    String fire = fx.awaitingPayment(fx.fire()).getArn();
    ClientConfirmation byEmail = new ClientConfirmation("EMAIL", "Confirmed by e-mail", null);
    assertThatThrownBy(() -> as.run("proc", () -> gate.confirmClient(fire, byEmail)))
        .extracting("code")
        .isEqualTo("GATE_REQUIRES_PAYMENT");

    String arn = fx.awaitingPayment(fx.liability()).getArn();
    assertThatThrownBy(
            () ->
                as.run(
                    "proc",
                    () -> gate.confirmClient(arn, new ClientConfirmation("PIGEON", null, null))))
        .isInstanceOf(RuntimeException.class);
    assertThatThrownBy(() -> as.run("proc", () -> gate.confirmDirect(arn, "no")))
        .extracting("code")
        .isEqualTo("GATE_NOT_DIRECT_PAYMENT");
    PaymentEvidence evidence = as.run("proc", () -> gate.confirmClient(arn, byEmail));
    assertThat(evidence.getKind()).isEqualTo(EvidenceKind.CLIENT_CONFIRMATION);
    assertThat(evidence.isGateOpened()).isTrue();
    assertThat(evidence.getCreatedBy()).isEqualTo("proc");
    Account account = queries.requireByArn(arn);
    assertThat(account.getStatus()).isEqualTo(AccountStatus.READY_FOR_PLACEMENT);
    assertThat(account.getLifecycle().getPaymentStatus()).isEqualTo(PaymentStatus.CLIENT_CONFIRMED);
    GateView view = gate.view(arn);
    assertThat(view.open()).isTrue();
    assertThat(view.evidence()).extracting(PaymentEvidence::getChannel).containsExactly("EMAIL");
    assertThatThrownBy(() -> as.run("proc", () -> gate.confirmClient(arn, byEmail)))
        .extracting("code")
        .isEqualTo("ACCOUNT_NOT_AWAITING_PAYMENT");
  }

  @Test
  void aConfirmedPaymentOpensTheGateOncePerSourceReference() {
    String arn = fx.awaitingPayment(fx.motor()).getArn();
    ConfirmedPayment payment =
        new ConfirmedPayment(
            arn,
            "RCPT-" + PlacementTestData.token(),
            new BigDecimal("100.00"),
            LocalDate.of(2026, 9, 1),
            "Receipt");
    assertThat(sweep.apply("CASHIERING", payment).opened()).isTrue();
    assertThat(queries.requireByArn(arn).getLifecycle().getPaymentStatus())
        .isEqualTo(PaymentStatus.PAID);
    assertThat(sweep.apply("CASHIERING", payment).message()).isEqualTo("Already recorded");
    ConfirmedPayment other =
        new ConfirmedPayment(arn, "RCPT-" + PlacementTestData.token(), null, null, null);
    assertThat(sweep.apply("CASHIERING", other).message()).contains("not awaiting payment");
    assertThat(sweep.sweep(fx.company(), List.of())).isZero();
    assertThat(sweepJob.name()).isEqualTo("PAYMENT_CONFIRMATION_SWEEP");
    assertThat(sweepJob.cron()).isNotBlank();
    assertThat(sweepJob.description()).isNotBlank();
    assertThat(sweepJob.execute(LocalDate.of(2026, 9, 24)).itemsProcessed()).isNotNegative();
  }
}
