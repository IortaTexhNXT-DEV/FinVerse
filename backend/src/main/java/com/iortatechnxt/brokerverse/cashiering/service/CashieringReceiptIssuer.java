package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptSource;
import com.iortatechnxt.brokerverse.cashiering.domain.OrAmounts;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt.ReceiptTender;
import com.iortatechnxt.brokerverse.cashiering.service.CashReceiptService.OrIssue;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cashiering's implementation of the ledger port {@code ReceiptIssuer} (CSHID.002/006/007): issues
 * a Head Office official receipt for a settlement of another Operations module (commission OR per
 * remittance settlement batch, incentive OR, DP commission OR). The OR is a no-cash settlement
 * document; a commission OR makes the deferred VAT due ({@code OPS_OR_ISSUE}, OPERATIONS_DESIGN
 * section 5 row 14). Idempotent on (source module, source reference); runs in the caller's
 * transaction.
 */
@Service
@Transactional
public class CashieringReceiptIssuer implements ReceiptIssuer {

  private final CashReceiptService receipts;

  /**
   * Creates the issuer.
   *
   * @param receipts receipts
   */
  public CashieringReceiptIssuer(CashReceiptService receipts) {
    this.receipts = receipts;
  }

  @Override
  public IssuedReceipt issueOfficialReceipt(ReceiptRequest request) {
    Receipt or =
        receipts.issueOr(
            new OrIssue(
                request.companyId(),
                null,
                request.orType(),
                request.receiptDate(),
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
                true));
    return new IssuedReceipt(
        Status.ISSUED,
        or.getReceiptNo(),
        or.getJournalBatchNo(),
        "Official receipt " + or.getReceiptNo() + " issued");
  }
}
