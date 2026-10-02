package com.iortatechnxt.brokerverse.cashiering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.CwtPath;
import com.iortatechnxt.brokerverse.cashiering.domain.CwtBatch;
import com.iortatechnxt.brokerverse.cashiering.domain.CwtTag;
import com.iortatechnxt.brokerverse.cashiering.domain.CwtTag.CwtDetails;
import com.iortatechnxt.brokerverse.cashiering.service.CwtService;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * BIR 2307 (CSHID.026/027, MKTID.010/013, DBMID.001): Marketing tags the 2% of a CWT account paid
 * at 98%, Cashiering receives it with the CWT copy, validates the insurer batch (PR reclassified to
 * PR2307), routes it to Disbursement, and Disbursement's payment releases it (PR2307 against DTIP).
 * The cash path settles the 2% with an AR.
 */
@IntegrationTest
class Cwt2307IT {

  @Autowired private CashFixtures fx;
  @Autowired private CwtService cwt;
  @Autowired private DisbursementQueueService disbursements;
  @Autowired private AsUser as;

  private OpsInvoice paidAt98() {
    OpsInvoice invoice = fx.cwtInvoice();
    fx.pay(invoice.getInvoiceNo(), invoice.premiumBalance());
    return fx.invoice(invoice.getInvoiceNo());
  }

  private CwtTag tag(OpsInvoice invoice, CwtPath path) {
    return as.run(
        "mktcoll",
        () ->
            cwt.tag(
                fx.company(),
                invoice.getInvoiceNo(),
                new CwtDetails(
                    null,
                    path,
                    path == CwtPath.CERTIFICATE ? "2307-" + System.nanoTime() : null,
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 9, 30),
                    "test")));
  }

  @Test
  void aCertificateIsValidatedReclassifiedRoutedAndReleased() {
    OpsInvoice invoice = paidAt98();
    BigDecimal outstanding = invoice.premiumBalance();
    assertThat(outstanding).isPositive();
    BigDecimal dtipBefore = invoice.component(LedgerComponent.DTIP).getBalance();

    CwtTag tag = tag(invoice, CwtPath.CERTIFICATE);
    assertThat(tag.getReference()).startsWith("CWT-");
    assertThat(tag.getAmount()).isEqualByComparingTo(outstanding);
    assertThatThrownBy(() -> tag(invoice, CwtPath.CERTIFICATE))
        .extracting("code")
        .isEqualTo("CWT_ALREADY_TAGGED");

    as.run("cashier", () -> cwt.receive(tag.getId(), false));
    assertThatThrownBy(
            () -> as.run("cashier", () -> cwt.validateBatch(fx.company(), List.of(tag.getId()))))
        .extracting("code")
        .isEqualTo("CWT_COPY_MISSING");
    CwtTag received = cwt.get(tag.getId());
    assertThat(received.getStage()).isEqualTo("VALIDATING");
    as.run("cashier", () -> cwt.checklist(tag.getId(), true));

    CwtBatch batch = as.run("cashier", () -> cwt.validateBatch(fx.company(), List.of(tag.getId())));
    assertThat(batch.getBatchNo()).startsWith("CWB-");
    OpsInvoice reclassed = fx.invoice(invoice.getInvoiceNo());
    assertThat(reclassed.premiumBalance()).isZero();
    assertThat(reclassed.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(reclassed.component(LedgerComponent.PR2307).getBalance())
        .isEqualByComparingTo(outstanding);
    assertThat(cwt.get(tag.getId()).getStage()).isEqualTo("REPORT_POSTED");

    CwtBatch routed = as.run("cashier", () -> cwt.route(batch.getId()));
    assertThat(routed.getDisbursementRequestNo()).startsWith("DSQ-");
    DisbursementRequest request =
        disbursements.find("CASHIERING", batch.getBatchNo()).orElseThrow();
    assertThat(request.getRequestType()).isEqualTo(DisbursementRequest.Type.CWT2307);

    as.run("disb", () -> disbursements.assignDv(request.getId(), "DV-" + System.nanoTime()));
    as.run("disb", () -> disbursements.markPaid(request.getId()));

    CwtTag released = cwt.get(tag.getId());
    assertThat(released.getStage()).isEqualTo("RELEASED");
    assertThat(released.getOffsetJournalNo()).isNotNull();
    OpsInvoice after = fx.invoice(invoice.getInvoiceNo());
    assertThat(after.component(LedgerComponent.PR2307).getBalance()).isZero();
    assertThat(after.component(LedgerComponent.DTIP).getBalance())
        .isEqualByComparingTo(dtipBefore.subtract(outstanding));
    assertThat(cwt.batch(batch.getId()).getStatus()).isEqualTo(CwtBatch.RELEASED);
    assertThat(cwt.tagsOf(batch.getId())).hasSize(1);
    assertThat(cwt.batches(fx.company())).extracting(CwtBatch::getId).contains(batch.getId());
  }

  @Test
  void theCashPathIssuesAnArForTheTwoPercent() {
    OpsInvoice invoice = paidAt98();
    CwtTag tag = tag(invoice, CwtPath.CASH);
    as.run("cashier", () -> cwt.receive(tag.getId(), true));
    CwtTag settled = as.run("cashier", () -> cwt.settleCash(tag.getId(), fx.ho()));
    assertThat(settled.getReceiptNo()).startsWith("AR-HO-");
    assertThat(cwt.get(tag.getId()).getStage()).isEqualTo("SETTLED_CASH");
    assertThat(fx.invoice(invoice.getInvoiceNo()).premiumBalance()).isZero();
    assertThat(
            cwt.list(
                fx.company(),
                List.of("SETTLED_CASH"),
                org.springframework.data.domain.Pageable.ofSize(50)))
        .extracting(CwtTag::getId)
        .contains(tag.getId());
  }
}
