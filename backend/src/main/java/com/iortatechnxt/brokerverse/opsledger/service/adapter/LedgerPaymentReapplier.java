package com.iortatechnxt.brokerverse.opsledger.service.adapter;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReapplier;
import java.math.BigDecimal;

/**
 * Default {@link PaymentReapplier} while cashiering is not installed: an invoice without applied
 * payments needs nothing; an invoice with payments cannot be re-applied without cashiering and is
 * refused ({@code PAYMENT_REAPPLIER_UNAVAILABLE}), so no adjustment silently leaves payments
 * applied to a premium that no longer exists.
 */
public class LedgerPaymentReapplier implements PaymentReapplier {

  private final InvoiceLedgerQueryService ledger;

  /**
   * Creates the adapter.
   *
   * @param ledger ledger reads
   */
  public LedgerPaymentReapplier(InvoiceLedgerQueryService ledger) {
    this.ledger = ledger;
  }

  @Override
  public ReapplyResult reapply(ReapplyRequest request) {
    OpsInvoice invoice = ledger.require(request.invoiceNo());
    BigDecimal applied =
        invoice.getComponents().stream()
            .map(OpsInvoiceComponent::netApplied)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (applied.signum() == 0) {
      return ReapplyResult.none(request.invoiceNo());
    }
    throw new BusinessRuleException(
        "PAYMENT_REAPPLIER_UNAVAILABLE",
        "Invoice "
            + request.invoiceNo()
            + " has applied payments; they can only be re-applied by Cashiering");
  }
}
