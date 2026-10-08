package com.iortatechnxt.brokerverse.issuance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.issuance.domain.AdviceRecipient;
import com.iortatechnxt.brokerverse.issuance.domain.AdviceSendMode;
import com.iortatechnxt.brokerverse.issuance.domain.AdviceStatus;
import com.iortatechnxt.brokerverse.issuance.domain.AdviceTrigger;
import com.iortatechnxt.brokerverse.issuance.domain.InsuranceAdvice;
import com.iortatechnxt.brokerverse.issuance.service.AdviceRecipientService;
import com.iortatechnxt.brokerverse.issuance.service.InsuranceAdviceService;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.PlacementTestData;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Automatic sending of the Insurance Advice to the recipient enrolled for the bank (FR-NB-107). */
@IntegrationTest
class AdviceAutoSendIT {

  private static final String BANK = "BDO_HOME_LOANS";

  @Autowired private InsuranceAdviceService advices;
  @Autowired private AdviceRecipientService recipients;
  @Autowired private MessageService messages;
  @Autowired private PlacementTestData fx;
  @Autowired private AsUser as;

  private InsuranceAdvice generate() {
    String arn = fx.placed(fx.fire());
    return as.run("proc", () -> advices.generate(arn, AdviceTrigger.MANUAL));
  }

  private AdviceRecipient.Data data(String segment, List<String> to, boolean auto) {
    return new AdviceRecipient.Data(
        BANK, segment, to, List.of("cc@bdo.example"), auto, LocalDate.of(2026, 1, 1), null);
  }

  @Test
  void anEnrolledBankReceivesTheAdviceAtOnceAndOthersWaitInTheRegister() {
    InsuranceAdvice waiting = generate();
    assertThat(waiting.getStatus()).isEqualTo(AdviceStatus.GENERATED);
    assertThat(waiting.getSendMode()).isNull();

    assertThatThrownBy(
            () -> recipients.create(fx.company(), data(null, List.of("not an address"), true)))
        .extracting("code")
        .isEqualTo("IA_RECIPIENT_EMAIL");
    AdviceRecipient other =
        as.run(
            "badmin",
            () -> recipients.create(fx.company(), data("CORBANK", List.of("x@bdo.example"), true)));
    AdviceRecipient setup =
        as.run(
            "badmin",
            () -> recipients.create(fx.company(), data(null, List.of("hl@bdo.example"), true)));
    try {
      InsuranceAdvice pending = generate();
      assertThat(pending.getStatus()).isEqualTo(AdviceStatus.GENERATED);
      assertThatThrownBy(
              () -> as.run("badmin", () -> recipients.authorize(fx.company(), setup.getId())))
          .isInstanceOf(RuntimeException.class);
      as.run("approver", () -> recipients.authorize(fx.company(), setup.getId()));
      as.run("approver", () -> recipients.authorize(fx.company(), other.getId()));
      assertThat(recipients.applicable(fx.company(), BANK, "CBG").map(AdviceRecipient::getId))
          .contains(setup.getId());

      InsuranceAdvice sent = generate();
      assertThat(sent.getStatus()).isEqualTo(AdviceStatus.SENT);
      assertThat(sent.getSendMode()).isEqualTo(AdviceSendMode.AUTOMATIC);
      assertThat(sent.getLastSentTo()).isEqualTo("hl@bdo.example");
      assertThat(messages.forRecord(InsuranceAdviceService.ENTITY, String.valueOf(sent.getId())))
          .hasSize(2);

      as.run(
          "badmin",
          () -> recipients.update(fx.company(), setup.getId(), data(null, List.of(), true)));
      as.run("approver", () -> recipients.authorize(fx.company(), setup.getId()));
      InsuranceAdvice failed = generate();
      assertThat(failed.getStatus()).isEqualTo(AdviceStatus.GENERATED);
      assertThat(failed.getAutoSendFailure()).contains("no recipient is set up");
    } finally {
      as.run("badmin", () -> recipients.deactivate(fx.company(), setup.getId()));
      as.run("badmin", () -> recipients.deactivate(fx.company(), other.getId()));
    }
    assertThat(generate().getStatus()).isEqualTo(AdviceStatus.GENERATED);
  }
}
