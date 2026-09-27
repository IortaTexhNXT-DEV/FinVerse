package com.iortatechnxt.brokerverse.adjustment;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.BatchOutcome;
import com.iortatechnxt.brokerverse.adjustment.domain.Computation;
import com.iortatechnxt.brokerverse.adjustment.domain.PostingBatch;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestStage;
import com.iortatechnxt.brokerverse.adjustment.service.DocText;
import com.iortatechnxt.brokerverse.adjustment.service.RequestDraft;
import com.iortatechnxt.brokerverse.adjustment.service.WriteOffService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Pure rules of the adjustment module. */
class AdjustmentDomainTest {

  @Test
  void theRequestTypeDecidesTheComputation() {
    assertThat(Computation.forRequestType(null)).isEqualTo(Computation.NONE);
    assertThat(Computation.forRequestType(" ")).isEqualTo(Computation.NONE);
    assertThat(Computation.forRequestType("FLAT_CANCELLATION"))
        .isEqualTo(Computation.CANCELLATION_FLAT);
    assertThat(Computation.forRequestType("FLAT_CANCELLATION_RETAIN_DST"))
        .isEqualTo(Computation.CANCELLATION_FLAT_RETAIN_DST);
    assertThat(Computation.forRequestType("PARTIAL_CANCELLATION"))
        .isEqualTo(Computation.CANCELLATION_PARTIAL);
    assertThat(Computation.forRequestType("TSI_CHANGE")).isEqualTo(Computation.SUM_INSURED);
    assertThat(Computation.forRequestType("WRITE_OFF")).isEqualTo(Computation.WRITE_OFF);
    assertThat(Computation.forRequestType("COMMISSION_CHANGE")).isEqualTo(Computation.AMOUNTS);
    assertThat(Computation.CANCELLATION_PARTIAL.isCancellation()).isTrue();
    assertThat(Computation.AMOUNTS.isCancellation()).isFalse();
    assertThat(Computation.NONE.isFinancial()).isFalse();
  }

  @Test
  void stagesTellWhetherARequestIsOpenEditableOrValidated() {
    assertThat(RequestStage.DRAFT.isOpen()).isTrue();
    assertThat(RequestStage.AWAITING_REAPPLICATION.isOpen()).isFalse();
    assertThat(RequestStage.POSTED.isOpen()).isFalse();
    assertThat(RequestStage.RETURNED.isEditable()).isTrue();
    assertThat(RequestStage.FOR_POSTING.isEditable()).isFalse();
    assertThat(RequestStage.FOR_APPROVAL.isValidated()).isTrue();
    assertThat(RequestStage.FOR_VALIDATION.isValidated()).isFalse();
  }

  @Test
  void amountsTellWhatChanges() {
    assertThat(AmountInput.NONE.changesPremium()).isFalse();
    assertThat(AmountInput.NONE.changesCommission()).isFalse();
    AmountInput commission =
        new AmountInput(null, BigDecimal.ZERO, null, null, null, null, BigDecimal.ONE, null);
    assertThat(commission.changesPremium()).isFalse();
    assertThat(commission.changesCommission()).isTrue();
  }

  @Test
  void aBatchCountsItsOutcomes() {
    PostingBatch batch = new PostingBatch(1L, "VB-2026-000001", null);
    batch.add(new PostingBatch.Line(1L, "ENR-1", "BI-1", BatchOutcome.POSTED, null));
    batch.add(
        new PostingBatch.Line(2L, "ENR-2", "BI-2", BatchOutcome.AWAITING_REAPPLICATION, null));
    batch.add(new PostingBatch.Line(3L, "ENR-3", "BI-3", BatchOutcome.FAILED, "No"));
    assertThat(batch.getPostedCount()).isEqualTo(1);
    assertThat(batch.getPendingCount()).isEqualTo(1);
    assertThat(batch.getFailedCount()).isEqualTo(1);
    assertThat(batch.getLines()).hasSize(3);
  }

  @Test
  void textsAndRangesFormatAsExpected() {
    assertThat(DocText.amount(new BigDecimal("1234.5"))).isEqualTo("1,234.50");
    assertThat(DocText.amount(null)).isEqualTo(DocText.NONE);
    assertThat(DocText.text(null)).isEqualTo(DocText.NONE);
    assertThat(DocText.date(Instant.parse("2026-09-30T20:00:00Z")))
        .isEqualTo(LocalDate.of(2026, 10, 1));
    WriteOffService.Range range =
        new WriteOffService.Range(new BigDecimal("10.00"), new BigDecimal("100.00"));
    assertThat(range.contains(new BigDecimal("10.00"))).isTrue();
    assertThat(range.contains(new BigDecimal("100.01"))).isFalse();
    RequestDraft draft = new RequestDraft("A", null, null, " ", "why");
    assertThat(draft.amounts()).isEqualTo(AmountInput.NONE);
    assertThat(draft.duplicateOverride()).isNull();
    assertThat(draft.on("B").invoiceNo()).isEqualTo("B");
    assertThat(draft.on("B").baselineOverride()).isEqualTo("why");
  }
}
