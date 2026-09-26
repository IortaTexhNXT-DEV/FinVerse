package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.ApplicationRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ApplicationSource;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptSource;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.UnappliedOrigin;
import com.iortatechnxt.brokerverse.cashiering.domain.Prebooked;
import com.iortatechnxt.brokerverse.cashiering.domain.PrebookedRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptAction;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied.UnappliedSpec;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedRepository;
import com.iortatechnxt.brokerverse.cashiering.service.ApplicationService.ApplyOptions;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The accounting of an approved cancellation or reinstatement (CSHID.012/013, OPERATIONS_DESIGN
 * section 5 rows 9-10).
 *
 * <ul>
 *   <li>Cancellation: every active application of the receipt is reversed (the invoices are
 *       outstanding again), the receipt event is re-posted negative ({@code ...:CANCEL:<no>}), open
 *       unapplied items and pre-booked waits of the receipt are closed.
 *   <li>Reinstatement: {@code OPS_RECEIPT_REINSTATE} for the reinstated amount (an OR re-posts its
 *       event pro rata), then the money is applied again to the encoded invoice; what is left goes
 *       to the unapplied workbench.
 * </ul>
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class ReceiptReversalService {

  private static final Set<String> BLOCKING_STAGES =
      Set.of("MONITORING", "FOR_APPROVAL", "IN_PROCESS", "COMPLETED", "FOR_REVERSAL");
  private static final int RATIO_SCALE = 10;

  private final ApplicationRepository applications;
  private final UnappliedRepository unapplied;
  private final PrebookedRepository prebooked;
  private final ApplicationService applier;
  private final UnappliedService unappliedItems;
  private final InvoiceLedgerQueryService ledger;
  private final CashReceiptService receipts;
  private final CashieringPosting posting;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param applications applications
   * @param unapplied unapplied items
   * @param prebooked pre-booked items
   * @param applier application engine
   * @param unappliedItems unapplied workbench
   * @param ledger invoice ledger
   * @param receipts receipts
   * @param posting accounting events
   * @param clock clock
   */
  public ReceiptReversalService(
      ApplicationRepository applications,
      UnappliedRepository unapplied,
      PrebookedRepository prebooked,
      ApplicationService applier,
      UnappliedService unappliedItems,
      InvoiceLedgerQueryService ledger,
      CashReceiptService receipts,
      CashieringPosting posting,
      Clock clock) {
    this.applications = applications;
    this.unapplied = unapplied;
    this.prebooked = prebooked;
    this.applier = applier;
    this.unappliedItems = unappliedItems;
    this.ledger = ledger;
    this.receipts = receipts;
    this.posting = posting;
    this.clock = clock;
  }

  /**
   * Refuses a cancellation while a disposition of the receipt's unapplied money is in progress or
   * done (the money has left the receipt).
   *
   * @param receipt receipt
   */
  @Transactional(readOnly = true)
  public void requireCancellable(Receipt receipt) {
    for (Unapplied item : unapplied.findByReceiptIdOrderByIdAsc(receipt.getId())) {
      if (BLOCKING_STAGES.contains(item.getStage())) {
        throw new BusinessRuleException(
            "RECEIPT_HAS_DISPOSITION",
            "Unapplied item " + item.getReference() + " of the receipt is " + item.getStage());
      }
    }
  }

  /**
   * Posts a cancellation (CSHID.012).
   *
   * @param receipt receipt
   * @param action approved cancellation
   * @return journal of the receipt reversal, may be null
   */
  public String cancel(Receipt receipt, ReceiptAction action) {
    requireCancellable(receipt);
    String suffix = ":" + action.getTransactionNo();
    for (Application app : applications.findByReceiptIdOrderByIdAsc(receipt.getId())) {
      if (app.isActive()) {
        OpsInvoice invoice = ledger.require(app.getInvoiceNo());
        applier.reverse(app, invoice, app.reference() + suffix, "Receipt cancelled");
      }
    }
    for (Unapplied item : unapplied.findByReceiptIdOrderByIdAsc(receipt.getId())) {
      if (Unapplied.STAGE_INITIAL.equals(item.getStage())) {
        unappliedItems.close(
            item, "receipt_cancelled", "Receipt " + receipt.getReceiptNo() + " cancelled");
      }
    }
    for (Prebooked item : prebooked.findByReceiptIdAndStatus(receipt.getId(), Prebooked.OPEN)) {
      item.resolve(Prebooked.RELEASED, null, "Receipt cancelled", clock.instant());
    }
    BigDecimal live = receipt.liveAmount();
    String batch =
        post(receipt, live.negate(), receipt.getKind() + ":" + receipt.getReceiptNo() + suffix);
    receipt.cancel();
    return batch;
  }

  /**
   * Posts a reinstatement (CSHID.013) and applies the money again.
   *
   * @param receipt cancelled receipt
   * @param action approved reinstatement
   * @return journal of the reinstatement, may be null
   */
  public String reinstate(Receipt receipt, ReceiptAction action) {
    receipt.reinstate(action.getAmount());
    String batch = post(receipt, action.getAmount(), "RIN:" + action.getTransactionNo());
    if (receipt.getKind() == ReceiptKind.AR
        && !receipts.nonPremium(receipt.getReceiptClass(), receipt.getReceiptDate())) {
      reapply(receipt, action);
    }
    return batch;
  }

  private void reapply(Receipt receipt, ReceiptAction action) {
    BigDecimal left = action.getAmount();
    Optional<OpsInvoice> invoice =
        action.getInvoiceNo() == null ? Optional.empty() : ledger.find(action.getInvoiceNo());
    if (invoice.isPresent()) {
      invoice.get().loadCollections();
      Optional<Application> app =
          applier.apply(
              invoice.get(),
              left,
              new Application.Origin(
                  receipt.getId(),
                  null,
                  ApplicationSource.REINSTATEMENT,
                  action.getTransactionNo()),
              new ApplyOptions(LocalDate.now(clock), false, receipt.getReceiptNo()));
      left = left.subtract(app.map(Application::getAmount).orElse(BigDecimal.ZERO));
    }
    if (left.signum() > 0) {
      unappliedItems.create(
          receipt.getCompanyId(),
          receipt.getBranchId(),
          new UnappliedSpec(
              UnappliedOrigin.OTHER,
              receipt.getId(),
              null,
              action.getInvoiceNo(),
              receipt.getPayorCode(),
              receipt.getPayorName(),
              receipt.getSalesUnit(),
              receipt.getCurrency(),
              left,
              null,
              CashieringSettings.MODULE,
              "RIN:" + action.getTransactionNo(),
              "Reinstated " + receipt.getReceiptNo()));
    }
  }

  private String post(Receipt receipt, BigDecimal amount, String sourceRef) {
    var context =
        receipts.context(
            receipt,
            new Receipt.ReceiptTender(
                receipt.getMode(), null, null, null, null, receipt.getSource(), null, null, null),
            (amount.signum() < 0 ? "Cancellation of " : "Reinstatement of ")
                + receipt.getReceiptNo());
    if (receipt.getKind() == ReceiptKind.OR) {
      BigDecimal factor =
          receipt.getAmount().signum() == 0
              ? BigDecimal.ZERO
              : amount.divide(receipt.getAmount(), RATIO_SCALE, RoundingMode.HALF_UP);
      return posting.publish(
          context,
          CashieringPosting.OR_ISSUE,
          sourceRef,
          CashReceiptService.orAmounts(
              receipt, receipt.getSource() == ReceiptSource.SETTLEMENT, factor));
    }
    String event;
    if (receipts.nonPremium(receipt.getReceiptClass(), receipt.getReceiptDate())) {
      event = CashieringPosting.AR_INSURANCE_RECEIPT;
    } else {
      event =
          amount.signum() < 0 ? CashieringPosting.AR_RECEIPT : CashieringPosting.RECEIPT_REINSTATE;
    }
    return posting.publish(context, event, sourceRef, Map.of(CashieringPosting.AMOUNT, amount));
  }
}
