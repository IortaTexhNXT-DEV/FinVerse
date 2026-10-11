package com.iortatechnxt.brokerverse.collections;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.collections.bulk.service.CollectionsBulkActions;
import com.iortatechnxt.brokerverse.collections.bulk.service.CollectionsBulkUpdateHandler;
import com.iortatechnxt.brokerverse.collections.common.service.ClxText;
import com.iortatechnxt.brokerverse.collections.disposition.service.DispositionSupport;
import com.iortatechnxt.brokerverse.collections.disposition.service.PrDispositionService;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.Stage;
import com.iortatechnxt.brokerverse.collections.installment.service.InstallmentPlanService;
import com.iortatechnxt.brokerverse.collections.promise.service.PromiseService;
import com.iortatechnxt.brokerverse.collections.unapplied.service.UnappliedDispositionService;
import com.iortatechnxt.brokerverse.collections.worklist.service.AssignmentService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The texts a Collections user reads (screen standards): dates, times and amounts as the screens
 * show them, codes in words, and refusals without permission, role or list codes.
 */
class CollectionsTextTest {

  /**
   * A code as stored: two or more capitals joined by underscores (CLX_ESCALATE, MKT_TL, PERMANENT).
   */
  private static final String CODE =
      ".*\\b([A-Z]{2,}_[A-Z_]+|PERMANENT|TEMPORARY|CASH|CERTIFICATE)\\b.*";

  @Test
  void datesTimesAndAmountsReadAsOnTheScreens() {
    assertThat(ClxText.date(LocalDate.of(2026, 10, 5))).isEqualTo("05-Oct-2026");
    assertThat(ClxText.date(null)).isEmpty();
    assertThat(ClxText.dateTime(Instant.parse("2026-10-05T00:00:00Z")))
        .isEqualTo("05-Oct-2026 08:00");
    assertThat(ClxText.amount("PHP", new BigDecimal("1000000"))).isEqualTo("PHP 1,000,000.00");
    assertThat(ClxText.amount("PHP", new BigDecimal("5567.175"))).isEqualTo("PHP 5,567.18");
    assertThat(ClxText.amount(null, BigDecimal.TEN)).isEqualTo("10.00");
  }

  @Test
  void codesReadInWords() {
    assertThat(ClxText.words("APPLY_TO_INVOICE")).isEqualTo("Apply to invoice");
    assertThat(ClxText.words("APPLIED")).isEqualTo("Applied");
    assertThat(ClxText.words(null)).isEmpty();
  }

  @Test
  void escalationOutcomeNamesTheStageInWords() {
    assertThat(CollectionsBulkActions.escalatedText("ESC-2026-000003", Stage.WITH_TL))
        .isEqualTo("Escalated in ESC-2026-000003, with the team lead");
    for (Stage stage : Stage.values()) {
      assertThat(CollectionsBulkActions.escalatedText("ESC-2026-000001", stage))
          .doesNotMatch(CODE)
          .doesNotContain(stage.name());
    }
  }

  @Test
  void collectorDispositionHistoryNamesTheDispositionByLabel() {
    assertThat(
            UnappliedDispositionService.dispositionText(
                "For application to invoice",
                "BI-HO-2026-000008",
                "Payor confirmed by e-mail that the payment is for this invoice"))
        .isEqualTo(
            "Collector disposition: For application to invoice, invoice BI-HO-2026-000008. Payor"
                + " confirmed by e-mail that the payment is for this invoice")
        .doesNotMatch(CODE);
    assertThat(UnappliedDispositionService.dispositionText("For refund", null, null))
        .isEqualTo("Collector disposition: For refund");
  }

  @Test
  void refusalsNameNoPermissionRoleOrListCode() {
    List<String> texts =
        List.of(
            PrDispositionService.RESERVED,
            DispositionSupport.BULK_NOT_ALLOWED,
            AssignmentService.REASSIGN_KIND,
            CollectionsBulkUpdateHandler.NOT_ALLOWED_TO_ESCALATE);
    assertThat(texts).allSatisfy(text -> assertThat(text).doesNotMatch(CODE));
    assertThat(PrDispositionService.RESERVED)
        .isEqualTo("This disposition is reserved to other roles");
    assertThat(AssignmentService.REASSIGN_KIND).contains("permanent").contains("temporary");
  }

  @Test
  void refusalsShowAmountsAsOnTheScreens() {
    assertThat(PromiseService.overBalance(new BigDecimal("30000"), new BigDecimal("22268.75")))
        .isEqualTo("The promised amount 30,000.00 is above the outstanding 22,268.75");
    assertThat(
            InstallmentPlanService.totalMismatch(
                new BigDecimal("1000"), new BigDecimal("1234567.5")))
        .isEqualTo(
            "The installments add up to 1,000.00 but the outstanding premium is 1,234,567.50");
    assertThat(UnappliedDispositionService.amountOverBalance(new BigDecimal("1200")))
        .isEqualTo("The amount must be above zero and at most the balance 1,200.00");
  }
}
