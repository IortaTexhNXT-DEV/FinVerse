package com.iortatechnxt.brokerverse.cashiering;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.cashiering.domain.SettlementOr;
import com.iortatechnxt.brokerverse.cashiering.domain.SettlementOrRepository;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringDecisions;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import com.iortatechnxt.brokerverse.remittance.RemittanceFixtures;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The commission OR of an insurer settlement issued once Disbursement approves the payment request
 * (FRS.CSH.07.01.01; Appendix R, C12), cancelled with the payment request, and issued at the
 * approval of the remittance batch with the other setting.
 */
@IntegrationTest
class SettlementOrIT {

  @Autowired private RemittanceFixtures fx;
  @Autowired private RecordFixtures records;
  @Autowired private SettlementOrRepository ors;
  @Autowired private DisbursementQueueService queue;
  @Autowired private AsUser as;

  @Test
  void theCommissionOrIsIssuedOnceDisbursementApprovesThePaymentRequest() {
    RemittanceBatch approved = fx.approvedBatch(fx.paidInvoice());
    assertThat(approved.getCommissionOrStatus()).isEqualTo("DEFERRED");
    assertThat(approved.getOrMessage()).contains("once Disbursement approves");
    SettlementOr kept = kept(approved);
    assertThat(kept.getStatus()).isEqualTo(SettlementOr.PENDING);
    assertThat(kept.getAwaitRef()).isEqualTo(approved.cycleReference());

    RemittanceBatch issued = fx.disbursementApproved(approved);

    assertThat(issued.getCommissionOrStatus()).isEqualTo("ISSUED");
    assertThat(issued.getCommissionOrNo()).startsWith("OR-HO-");
    SettlementOr done = ors.findById(kept.getId()).orElseThrow();
    assertThat(done.getStatus()).isEqualTo(SettlementOr.ISSUED);
    assertThat(done.getReceiptNo()).isEqualTo(issued.getCommissionOrNo());
    assertThat(done.getIssuedAt()).isNotNull();
  }

  @Test
  void aCancelledPaymentRequestCancelsTheOrKept() {
    RemittanceBatch approved = fx.approvedBatch(fx.paidInvoice());
    Long requestId = queue.find("REMITTANCE", approved.cycleReference()).orElseThrow().getId();

    as.run("disb", () -> queue.cancel(requestId, "Wrong payee bank account"));

    SettlementOr cancelled = kept(approved);
    assertThat(cancelled.getStatus()).isEqualTo(SettlementOr.CANCELLED);
    assertThat(cancelled.getMessage()).contains("Wrong payee bank account");
    assertThat(cancelled.getReceiptNo()).isNull();
  }

  @Test
  void withTheRemittanceApprovalSettingTheOrIsIssuedAtOnce() {
    records.setting(CashieringDecisions.SETTLEMENT_OR_TRIGGER, "REMITTANCE_APPROVAL");
    try {
      OpsInvoice paid = fx.paidInvoice();
      RemittanceBatch approved = fx.approvedBatch(paid);
      assertThat(approved.getCommissionOrStatus()).isEqualTo("ISSUED");
      assertThat(approved.getCommissionOrNo()).startsWith("OR-HO-");
      assertThat(ors.findBySourceModuleAndSourceRef("REMITTANCE", source(approved))).isEmpty();
    } finally {
      records.setting(CashieringDecisions.SETTLEMENT_OR_TRIGGER, "DISBURSEMENT_APPROVAL");
    }
  }

  private SettlementOr kept(RemittanceBatch batch) {
    return ors.findBySourceModuleAndSourceRef("REMITTANCE", source(batch)).orElseThrow();
  }

  private static String source(RemittanceBatch batch) {
    return batch.getBatchNo() + ":COMMISSION";
  }
}
