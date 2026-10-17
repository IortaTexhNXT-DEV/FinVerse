package com.iortatechnxt.brokerverse.eb.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Invariants of the renewal advice, feedback, BOR and tracked item entities (waves E1-B, E1-C). */
class EbServicingDomainTest {

  private static final Instant NOW = Instant.parse("2026-09-26T02:00:00Z");

  private static EbCycle cycle() {
    return new EbCycle(1L, "EBC-2026-000001", 2L, BusinessType.RENEWAL, 2027, null);
  }

  @Test
  void theRenewalAdviceCountsRemindersUntilFeedback() {
    EbRenewalAdvice advice =
        new EbRenewalAdvice(
            cycle(),
            LocalDate.of(2027, 1, 1),
            new EbRenewalAdvice.Sending(
                NOW, "SYSTEM", false, List.of("a@x.example", "b@x.example")),
            5L,
            6L);
    assertThat(advice.getRecipients()).containsExactly("a@x.example", "b@x.example");
    assertThat(advice.isManual()).isFalse();
    advice.remind(NOW.plusSeconds(60));
    assertThat(advice.getRemindersSent()).isEqualTo(1);
    assertThat(advice.getLastReminderAt()).isEqualTo(NOW.plusSeconds(60));
    assertThat(advice.awaitingFeedback()).isTrue();
    advice.stopReminders(NOW.plusSeconds(120));
    advice.stopReminders(NOW.plusSeconds(180));
    assertThat(advice.getFeedbackAt()).isEqualTo(NOW.plusSeconds(120));
    assertThat(advice.awaitingFeedback()).isFalse();
    assertThat(advice.getMessageId()).isEqualTo(5L);
    assertThat(advice.getAttachmentId()).isEqualTo(6L);
  }

  @Test
  void feedbackNeedsTextOrAFile() {
    assertThatThrownBy(
            () -> new EbFeedback(cycle(), EbFeedbackChannel.AO, LocalDate.now(), null, 0))
        .extracting("code")
        .isEqualTo("EB_FEEDBACK_EMPTY");
    assertThatThrownBy(
            () ->
                new EbFeedback(cycle(), EbFeedbackChannel.AO, LocalDate.now(), "x".repeat(4001), 0))
        .extracting("code")
        .isEqualTo("EB_FEEDBACK_TOO_LONG");
    EbFeedback files = new EbFeedback(cycle(), EbFeedbackChannel.PHONE, LocalDate.now(), " ", 2);
    assertThat(files.getText()).isNull();
    assertThat(files.getFileCount()).isEqualTo(2);
    assertThat(
            new EbFeedback(cycle(), EbFeedbackChannel.LETTER, LocalDate.now(), " ok ", 0).getText())
        .isEqualTo("ok");
  }

  @Test
  void aBorIsValidatedOnlyWithItsChecklistAndIsActiveInItsValidity() {
    EbBor bor = new EbBor(cycle(), 1, 9L);
    LocalDate from = LocalDate.of(2026, 1, 1);
    LocalDate to = LocalDate.of(2026, 12, 31);
    assertThatThrownBy(
            () -> bor.validate(new EbBor.Checklist(true, true, true, null, to), "v", NOW))
        .extracting("code")
        .isEqualTo("EB_BOR_VALIDITY_REQUIRED");
    bor.validate(new EbBor.Checklist(true, true, true, from, to), "v", NOW);
    assertThat(bor.activeOn(LocalDate.of(2026, 6, 1))).isTrue();
    assertThat(bor.activeOn(LocalDate.of(2027, 1, 1))).isFalse();
    bor.supersede();
    assertThat(bor.getStatus()).isEqualTo(EbBorStatus.SUPERSEDED);
    EbBor rejected = new EbBor(cycle(), 2, 10L);
    rejected.reject(" Blank ", "v", NOW);
    rejected.supersede();
    assertThat(rejected.getStatus()).isEqualTo(EbBorStatus.REJECTED);
    assertThat(rejected.getRejectReason()).isEqualTo("Blank");
  }

  @Test
  void aTrackedItemMovesFromPendingToClosed() {
    EbTrackedItem item =
        new EbTrackedItem(
            1L,
            2L,
            null,
            "HMO_CARD",
            new EbTrackedItem.Details(
                "Card",
                "EMP-1",
                null,
                null,
                EbResponsibleParty.INSURER,
                "INS-MGIC",
                null,
                LocalDate.of(2026, 10, 1),
                null));
    assertThat(item.overdueOn(LocalDate.of(2026, 10, 2))).isTrue();
    assertThat(item.overdueOn(LocalDate.of(2026, 10, 1))).isFalse();
    item.followedUp(NOW);
    item.escalate(NOW);
    item.escalate(NOW.plusSeconds(5));
    assertThat(item.getFollowUpsSent()).isEqualTo(1);
    assertThat(item.getEscalatedAt()).isEqualTo(NOW);
    item.receive(LocalDate.of(2026, 10, 3), null);
    assertThat(item.overdueOn(LocalDate.of(2026, 10, 9))).isFalse();
    item.close(LocalDate.of(2026, 10, 4), null, "Done");
    assertThat(item.getStatus()).isEqualTo(EbItemStatus.CLOSED);
    assertThat(item.getClosedOn()).isEqualTo(LocalDate.of(2026, 10, 4));
    assertThatThrownBy(() -> item.close(LocalDate.of(2026, 10, 5), null, null))
        .extracting("code")
        .isEqualTo("EB_ITEM_CLOSED");
  }
}
