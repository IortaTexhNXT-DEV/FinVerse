package com.iortatechnxt.brokerverse.adjustment;

import static com.iortatechnxt.brokerverse.adjustment.AdjustmentFixtures.FROM;
import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestStage;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReapplier;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * The cashiering path of a decrease of a paid invoice (ADJID.009/012/013, row 17), with a stubbed
 * {@link PaymentReapplier} standing in for cashiering: the payments are re-applied in the posting
 * transaction, the excess is recorded on the request and the request is complete; a remitted
 * decrease keeps its AR Insurer and the pending negative adjustment for remittance.
 */
@IntegrationTest
@Import(AdjustmentReapplierIT.StubCashiering.class)
class AdjustmentReapplierIT {

  @Autowired private AdjustmentFixtures fx;
  @Autowired private StubCashiering.Calls calls;

  @Test
  void aPaidInvoiceCancelledFlatHasItsPaymentsReappliedAndTheExcessRecorded() {
    OpsInvoice invoice = fx.invoice();
    fx.payInFull(invoice);
    EndorsementRequest request =
        fx.raiseAndPost(
            invoice, AdjustmentFixtures.cancellation("FLAT_CANCELLATION", FROM), AmountInput.NONE);

    assertThat(request.getStage()).isEqualTo(RequestStage.POSTED);
    assertThat(request.outcome().excessAmount()).isEqualByComparingTo(invoice.getGrossPremium());
    assertThat(request.outcome().unappliedRef()).isEqualTo("UNA-" + invoice.getInvoiceNo());
    assertThat(request.trail().completedAt()).isNotNull();
    assertThat(calls.requests()).containsKey(invoice.getInvoiceNo());
    assertThat(calls.requests().get(invoice.getInvoiceNo()).sourceRef())
        .isEqualTo("ADJ:" + request.getRequestNo());
    OpsInvoice after = fx.reload(invoice);
    assertThat(after.isPendingNegAdj()).isFalse();
    assertThat(after.getLockOwner()).isNull();
  }

  @Test
  void aRemittedDecreaseKeepsThePendingNegativeAdjustmentForRemittance() {
    OpsInvoice invoice = fx.invoice();
    fx.payInFull(invoice);
    fx.remitInFull(invoice);
    EndorsementRequest request =
        fx.raiseAndPost(
            invoice, AdjustmentFixtures.cancellation("FLAT_CANCELLATION", FROM), AmountInput.NONE);

    assertThat(request.getStage()).isEqualTo(RequestStage.POSTED);
    assertThat(request.outcome().arInsurerAmount()).isEqualByComparingTo(invoice.getGrossPremium());
    assertThat(fx.reload(invoice).isPendingNegAdj()).isTrue();
  }

  /** Cashiering stand-in: takes the payments above the new premium off as excess. */
  @TestConfiguration
  static class StubCashiering {

    @Bean
    Calls calls() {
      return new Calls(new ConcurrentHashMap<>());
    }

    @Bean
    @Primary
    PaymentReapplier stubReapplier(Calls calls, InvoiceLedgerQueryService ledger) {
      return request -> {
        calls.requests().put(request.invoiceNo(), request);
        BigDecimal excess =
            ledger.require(request.invoiceNo()).premiumBalance().negate().max(BigDecimal.ZERO);
        return new PaymentReapplier.ReapplyResult(
            request.invoiceNo(), excess, excess, List.of("AR-TEST"), "UNA-" + request.invoiceNo());
      };
    }

    /** Re-applications asked, by invoice. */
    record Calls(Map<String, PaymentReapplier.ReapplyRequest> requests) {}
  }
}
