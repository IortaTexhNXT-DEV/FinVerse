package com.iortatechnxt.brokerverse.payrequest;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.crm.domain.ClientPayoutAccount;
import com.iortatechnxt.brokerverse.crm.domain.PayoutMode;
import com.iortatechnxt.brokerverse.payrequest.domain.DisbursementTrack;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.domain.RefundLine;
import com.iortatechnxt.brokerverse.payrequest.domain.RefundLineValues;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestKind;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestStage;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestTrail;
import com.iortatechnxt.brokerverse.payrequest.domain.ValidationStatus;
import com.iortatechnxt.brokerverse.payrequest.service.PayRequestRules;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pure rules of the requests: stages, send references, trail, payment track and payout keys. */
class PayRequestDomainTest {

  @Test
  void aRequestStartsInItsKindsFirstStageAndReleasesItsArsWhenCancelled() {
    PaymentRequest refund =
        new PaymentRequest(1L, 1L, "RRF-2026-000001", RequestKind.REFUND, LocalDate.now());
    PaymentRequest cancel =
        new PaymentRequest(
            1L, 1L, "CCR-2026-000001", RequestKind.CHECK_CANCELLATION, LocalDate.now());
    assertThat(refund.getStage()).isEqualTo(RequestStage.DRAFT);
    assertThat(cancel.getStage()).isEqualTo(RequestStage.REQUESTED);
    assertThat(RequestKind.CHECK_CANCELLATION.pays()).isFalse();
    assertThat(RequestKind.CASH_ADVANCE.workflow()).isEqualTo("PRQ_CASH_ADVANCE");
    RefundLine line =
        new RefundLine(
            1,
            new RefundLineValues(
                " AR-1 ",
                "CL-1",
                "Juan",
                " ",
                BigDecimal.TEN,
                "OVERPAYMENT",
                null,
                null,
                null,
                null),
            "INV-ROOT",
            false);
    assertThat(line.getArNo()).isEqualTo("AR-1");
    assertThat(line.getInvoiceNo()).isNull();
    refund.replaceLines(List.of(line), false);
    assertThat(refund.rootInvoiceNo()).isEqualTo("INV-ROOT");
    refund.moveTo(RequestStage.CANCELLED);
    assertThat(refund.getLines().get(0).isLive()).isFalse();
    assertThat(RequestStage.CANCELLED.isEnded()).isTrue();
    assertThat(RequestStage.PREPARING.isEditable()).isTrue();
    assertThat(ValidationStatus.DEFERRED.isPending()).isTrue();
  }

  @Test
  void aResentRequestGetsANewSourceReference() {
    PaymentRequest r =
        new PaymentRequest(1L, 1L, "RRF-2026-000002", RequestKind.REFUND, LocalDate.now());
    assertThat(r.nextSendRef()).isEqualTo("RRF-2026-000002");
    assertThat(r.nextSendRef()).isEqualTo("RRF-2026-000002/2");
    assertThat(r.currentSendRef()).isEqualTo("RRF-2026-000002/2");
    assertThat(PaymentRequest.requestNoOf("RRF-2026-000002/2")).isEqualTo("RRF-2026-000002");
    assertThat(PaymentRequest.requestNoOf("RRF-2026-000002")).isEqualTo("RRF-2026-000002");
  }

  @Test
  void theTrailAndTrackKeepEarlierFacts() {
    Instant now = Instant.now();
    RequestTrail trail =
        RequestTrail.NONE
            .submitted("a", now)
            .reviewed("b", now)
            .approved("c", now)
            .hrApproved("d", now)
            .returned("OTHERS", "x");
    assertThat(trail.submittedBy()).isEqualTo("a");
    assertThat(trail.hrApprovedBy()).isEqualTo("d");
    assertThat(trail.returnReason()).isEqualTo("OTHERS");
    DisbursementTrack track =
        DisbursementTrack.NONE
            .status("DSQ-1", "DV_ASSIGNED", "DV-1", null)
            .tracked("APPROVED", null)
            .status(null, "PAID", null, null)
            .tracked(null, "RELEASED")
            .disbursed(now)
            .handedOff("HANDOFF-1");
    assertThat(track.requestNo()).isEqualTo("DSQ-1");
    assertThat(track.dvNo()).isEqualTo("DV-1");
    assertThat(track.dvStatus()).isEqualTo("APPROVED");
    assertThat(track.instrumentStatus()).isEqualTo("RELEASED");
    assertThat(track.disbursedAt()).isEqualTo(now);
    assertThat(track.handoffRef()).isEqualTo("HANDOFF-1");
  }

  @Test
  void payoutModesAndKeys() {
    assertThat(PayRequestRules.payoutMode("CTA")).isEqualTo(PayoutMode.CTA);
    assertThat(PayRequestRules.payoutMode("CHECK")).isEqualTo(PayoutMode.CHECK);
    assertThat(PayRequestRules.payoutMode("ATD")).isNull();
    assertThat(ClientPayoutAccount.key("  juan   dela cruz ")).isEqualTo("JUAN DELA CRUZ");
  }
}
