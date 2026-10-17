package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptSource;
import com.iortatechnxt.brokerverse.cashiering.domain.OrAmounts;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt.ReceiptTender;
import com.iortatechnxt.brokerverse.cashiering.service.CashReceiptService.OrIssue;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cashiering's implementation of the ledger port {@code ReceiptIssuer} (CSHID.002/006/007): issues
 * a Head Office official receipt for a settlement of another Operations module (commission OR per
 * remittance settlement batch, incentive OR, DP commission OR). The OR is a no-cash settlement
 * document; a commission OR makes the deferred VAT due ({@code OPS_OR_ISSUE}, OPERATIONS_DESIGN
 * section 5 row 14). Idempotent on (source module, source reference); runs in the caller's
 * transaction. An OR whose request was sent to Disbursement is kept until Disbursement approves the
 * payment request when {@code CASH_SETTLEMENT_OR_TRIGGER = DISBURSEMENT_APPROVAL}
 * (FRS.CSH.07.01.01; Appendix R, C12), and issued at once with {@code REMITTANCE_APPROVAL}.
 */
@Service
@Transactional
public class CashieringReceiptIssuer implements ReceiptIssuer {

  private final CashReceiptService receipts;
  private final CashieringDecisions decisions;
  private final SettlementOrService settlementOrs;

  /**
   * Creates the issuer.
   *
   * @param receipts receipts
   * @param decisions settings (the point the settlement OR is issued, C12)
   * @param settlementOrs ORs kept until Disbursement approves
   */
  public CashieringReceiptIssuer(
      CashReceiptService receipts,
      CashieringDecisions decisions,
      SettlementOrService settlementOrs) {
    this.receipts = receipts;
    this.decisions = decisions;
    this.settlementOrs = settlementOrs;
  }

  @Override
  public IssuedReceipt issueOfficialReceipt(ReceiptRequest request) {
    String awaitRef = request.source().awaitRef();
    if (awaitRef != null && decisions.orAtDisbursementApproval()) {
      return settlementOrs.keep(request);
    }
    Receipt or = receipts.issueOr(orIssue(request, request.receiptDate()));
    settlementOrs.issuedAtOnce(request, or.getReceiptNo());
    return new IssuedReceipt(
        Status.ISSUED,
        or.getReceiptNo(),
        or.getJournalBatchNo(),
        "Official receipt " + or.getReceiptNo() + " issued");
  }

  /**
   * The OR of a request of another module: a no-cash settlement OR of Head Office.
   *
   * @param request request
   * @param receiptDate receipt date
   * @return OR to issue
   */
  static OrIssue orIssue(ReceiptRequest request, LocalDate receiptDate) {
    return new OrIssue(
        request.companyId(),
        null,
        request.orType(),
        receiptDate,
        request.payee().partyCode(),
        request.payee().name(),
        request.currency(),
        request.lines().stream()
            .map(
                l ->
                    CashReceiptService.line(
                        l.invoiceNo(),
                        l.insurerCode(),
                        new OrAmounts(l.gross(), l.vat(), l.wtax()),
                        l.description()))
            .toList(),
        new ReceiptTender(
            PaymentMode.NON_CASH,
            null,
            null,
            null,
            request.source().certificateRef(),
            ReceiptSource.SETTLEMENT,
            request.source().module(),
            request.source().reference(),
            request.source().remarks()),
        true);
  }
}
