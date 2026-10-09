package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.ApplicationRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptActionType;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.UnappliedOrigin;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptAction;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptActionRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptLine;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecord;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordAccount;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.ReinstatementType;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied.UnappliedSpec;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringPosting.PostingContext;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Posting of BDOI's cancellation and reinstatement records (FRS.CSH.03.01.08, 04.01.09).
 *
 * <ul>
 *   <li>Cancellation of an AR: the AR number becomes Cancelled and the payment of each account is
 *       reversed, so the account is outstanding again; with the setting {@code
 *       CASH_REMITTED_CANCELLATION = AR_INSURER_REFUND} (Appendix R, C9) the payment of an account
 *       already remitted is not reversed but becomes an AR Insurer Refund of the account and
 *       insurer.
 *   <li>Cancellation of an OR: the OR is reversed and, with the same setting, its payment is kept
 *       as a Commission Receivable payment of each account and insurer.
 *   <li>Reinstatement of an AR: the AR stays Issued; the payment of each account selected (all for
 *       a full reinstatement) is reversed and becomes an Unapplied payment of the account, or an AR
 *       Insurer Refund when the account was remitted, whose remittance status is reversed.
 *   <li>Reinstatement of an OR: the OR stays Issued; the payment of each account becomes an AP
 *       Unapplied Commission of the account and insurer.
 * </ul>
 *
 * Every record created carries the record number and the receipt number as its source.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class RecordReversals {

  private static final Set<RemittanceStatus> REMITTED =
      Set.of(RemittanceStatus.PARTIALLY_REMITTED, RemittanceStatus.FULLY_REMITTED);
  private static final String AMOUNT = CashieringPosting.AMOUNT;

  private final CashReceiptRepository receipts;
  private final ApplicationRepository applications;
  private final ReceiptActionRepository actions;
  private final ReceiptReversalService reversal;
  private final ApplicationService applier;
  private final UnappliedService unapplied;
  private final InvoiceLedgerQueryService ledger;
  private final InvoiceLedgerService ledgerWriter;
  private final CashReceiptService receiptService;
  private final CashieringPosting posting;
  private final CashieringDecisions decisions;

  /**
   * Creates the posting.
   *
   * @param receipts receipts
   * @param applications applications
   * @param actions cancellation and reinstatement history of the receipt
   * @param reversal cancellation of a receipt
   * @param applier reversal of an application
   * @param unapplied unapplied payment records
   * @param ledger invoices
   * @param ledgerWriter remittance status of an invoice
   * @param receiptService posting facts of a receipt
   * @param posting accounting events
   * @param decisions settings
   */
  public RecordReversals(
      CashReceiptRepository receipts,
      ApplicationRepository applications,
      ReceiptActionRepository actions,
      ReceiptReversalService reversal,
      ApplicationService applier,
      UnappliedService unapplied,
      InvoiceLedgerQueryService ledger,
      InvoiceLedgerService ledgerWriter,
      CashReceiptService receiptService,
      CashieringPosting posting,
      CashieringDecisions decisions) {
    this.receipts = receipts;
    this.applications = applications;
    this.actions = actions;
    this.reversal = reversal;
    this.applier = applier;
    this.unapplied = unapplied;
    this.ledger = ledger;
    this.ledgerWriter = ledgerWriter;
    this.receiptService = receiptService;
    this.posting = posting;
    this.decisions = decisions;
  }

  /**
   * Posts a cancellation or reinstatement record.
   *
   * @param record record For Posting
   * @param poster Approver/Poster
   * @param at time of the posting
   * @return the AR or OR number
   */
  public String post(ReceiptRecord record, String poster, Instant at) {
    Receipt receipt =
        receipts
            .findById(record.getReceiptId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        CashReceiptService.ENTITY, record.getReceiptId()));
    String batch =
        switch (record.getRecordKind()) {
          case CANCELLATION -> cancel(record, receipt, poster, at);
          case REINSTATEMENT -> reinstate(record, receipt, poster, at);
          case CREATION ->
              throw new IllegalStateException("Not a reversal: " + record.getRecordNo());
        };
    record.posted(receipt, poster, at, batch);
    return receipt.getReceiptNo();
  }

  private String cancel(ReceiptRecord record, Receipt receipt, String poster, Instant at) {
    ReceiptAction action = history(record, receipt, ReceiptActionType.CANCEL);
    boolean bdoi = decisions.bdoiCancellation();
    String batch =
        reversal.cancel(
            receipt, action, app -> bdoi && receipt.getKind() == ReceiptKind.AR && remitted(app));
    if (bdoi && receipt.getKind() == ReceiptKind.AR) {
      for (Application app : applications.findByReceiptIdOrderByIdAsc(receipt.getId())) {
        if (app.isActive() && remitted(app)) {
          insurerRefund(record, receipt, app, true);
        }
      }
    }
    if (bdoi && receipt.getKind() == ReceiptKind.OR) {
      posting.publish(
          receiptService.context(
              receipt, CashReceiptService.tenderOf(receipt), record.getRecordNo()),
          "OPS_COMMISSION_PAYMENT_HELD",
          "CN:" + record.getRecordNo(),
          Map.of(AMOUNT, receipt.getAmount()));
      commissionItems(record, receipt, UnappliedOrigin.COMMISSION_RECEIVABLE);
    }
    action.approved(poster, at, batch);
    return batch;
  }

  private String reinstate(ReceiptRecord record, Receipt receipt, String poster, Instant at) {
    ReceiptActionType type =
        record.getReason().reinstatementType() == ReinstatementType.PARTIAL
            ? ReceiptActionType.REINSTATE_PARTIAL
            : ReceiptActionType.REINSTATE_FULL;
    ReceiptAction action = history(record, receipt, type);
    if (receipt.getKind() == ReceiptKind.OR) {
      String batch = apUnappliedCommission(record, receipt);
      action.approved(poster, at, batch);
      return batch;
    }
    Set<String> chosen =
        record.getAccounts().stream().map(RecordAccount::reference).collect(Collectors.toSet());
    for (Application app : applications.findByReceiptIdOrderByIdAsc(receipt.getId())) {
      if (app.isActive() && chosen.contains(app.getInvoiceNo())) {
        reinstateApplication(record, receipt, app);
      }
    }
    action.approved(poster, at, null);
    return null;
  }

  private void reinstateApplication(ReceiptRecord record, Receipt receipt, Application app) {
    OpsInvoice invoice = ledger.require(app.getInvoiceNo());
    boolean wasRemitted = REMITTED.contains(invoice.getRemittanceStatus());
    applier.reverse(
        app,
        invoice,
        app.reference() + ":" + record.getRecordNo(),
        "Reinstatement " + record.getRecordNo() + " of " + receipt.getReceiptNo());
    if (wasRemitted) {
      ledgerWriter.setRemittanceStatus(
          app.getInvoiceNo(),
          RemittanceStatus.WITH_OUTSTANDING_BALANCE,
          CashieringSettings.MODULE,
          "Reinstatement " + record.getRecordNo());
      insurerRefund(record, receipt, app, false);
    } else {
      item(
          record, receipt, UnappliedOrigin.REINSTATEMENT, app.getInvoiceNo(), app.getAmount(), app);
    }
  }

  /**
   * An AR Insurer Refund for the payment of a remitted account; with the journal when the payment
   * was not reversed (cancellation).
   */
  private void insurerRefund(ReceiptRecord record, Receipt receipt, Application app, boolean post) {
    OpsInvoice invoice = ledger.require(app.getInvoiceNo());
    if (post) {
      PostingContext base =
          receiptService.context(
              receipt, CashReceiptService.tenderOf(receipt), record.getRecordNo());
      posting.publish(
          withParty(base, invoice.getInsurerCode()),
          "OPS_AR_INSURER_REFUND",
          "CN:" + record.getRecordNo() + ":" + app.getId(),
          Map.of(AMOUNT, app.getAmount()));
    }
    item(
        record,
        receipt,
        UnappliedOrigin.AR_INSURER_REFUND,
        app.getInvoiceNo(),
        app.getAmount(),
        app);
  }

  private String apUnappliedCommission(ReceiptRecord record, Receipt receipt) {
    String batch = null;
    for (ReceiptLine line : receipt.getLines()) {
      String insurer =
          line.getInsurerCode() != null ? line.getInsurerCode() : receipt.getPayorCode();
      PostingContext base =
          receiptService.context(
              receipt, CashReceiptService.tenderOf(receipt), record.getRecordNo());
      String posted =
          posting.publish(
              withParty(base, insurer),
              "OPS_AP_UNAPPLIED_COMMISSION",
              "RE:" + record.getRecordNo() + ":" + line.getInvoiceNo(),
              Map.of(AMOUNT, line.getNet()));
      batch = batch == null ? posted : batch;
    }
    commissionItems(record, receipt, UnappliedOrigin.AP_UNAPPLIED_COMMISSION);
    return batch;
  }

  private void commissionItems(ReceiptRecord record, Receipt receipt, UnappliedOrigin origin) {
    int n = 0;
    for (ReceiptLine line : receipt.getLines()) {
      n++;
      if (line.getNet().signum() > 0) {
        unapplied.create(
            receipt.getCompanyId(),
            receipt.getBranchId(),
            new UnappliedSpec(
                origin,
                receipt.getId(),
                null,
                line.getInvoiceNo(),
                line.getInsurerCode() != null ? line.getInsurerCode() : receipt.getPayorCode(),
                receipt.getPayorName(),
                receipt.getSalesUnit(),
                receipt.getCurrency(),
                line.getNet(),
                null,
                CashieringSettings.MODULE,
                record.getRecordNo() + ":" + n,
                "From " + record.getRecordNo() + " of " + receipt.getReceiptNo()));
      }
    }
  }

  private void item(
      ReceiptRecord record,
      Receipt receipt,
      UnappliedOrigin origin,
      String invoiceNo,
      BigDecimal amount,
      Application app) {
    unapplied.create(
        receipt.getCompanyId(),
        receipt.getBranchId(),
        new UnappliedSpec(
            origin,
            receipt.getId(),
            null,
            invoiceNo,
            app.getClientCode(),
            receipt.getPayorName(),
            receipt.getSalesUnit(),
            receipt.getCurrency(),
            amount,
            null,
            CashieringSettings.MODULE,
            record.getRecordNo() + ":" + app.getId(),
            "From " + record.getRecordNo() + " of " + receipt.getReceiptNo()));
  }

  private boolean remitted(Application app) {
    return ledger
        .find(app.getInvoiceNo())
        .map(i -> REMITTED.contains(i.getRemittanceStatus()))
        .orElse(false);
  }

  private ReceiptAction history(ReceiptRecord record, Receipt receipt, ReceiptActionType type) {
    ReceiptAction action =
        new ReceiptAction(
            receipt,
            record.getRecordNo(),
            type,
            new ReceiptAction.Reason(
                record.getReason().reasonCode(), record.getReason().reasonText()),
            record.total());
    action.markStage(ReceiptAction.POSTED);
    return actions.save(action);
  }

  private static PostingContext withParty(PostingContext c, String party) {
    return new PostingContext(
        c.companyId(),
        c.branchId(),
        c.valueDate(),
        c.currency(),
        c.reference(),
        party == null ? c.partyCode() : party,
        c.businessLine(),
        c.costCenter(),
        c.narration(),
        c.bankAccount());
  }
}
