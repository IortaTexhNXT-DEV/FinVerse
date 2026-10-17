package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** The open premium receivable of a legacy invoice, for the direct payment PR reversal batches. */
@Component
public class DpprOpenPremium {
  private final InvoiceLedgerQueryService ledger;

  /**
   * Creates the lookup.
   *
   * @param ledger Operations ledger
   */
  public DpprOpenPremium(InvoiceLedgerQueryService ledger) {
    this.ledger = ledger;
  }

  /**
   * The legacy invoice.
   *
   * @param invoiceNo invoice
   * @return the invoice
   */
  @Transactional(readOnly = true)
  public OpsInvoice legacy(String invoiceNo) {
    OpsInvoice invoice =
        ledger
            .find(invoiceNo)
            .orElseThrow(() -> new ResourceNotFoundException("Operations invoice", invoiceNo));
    if (!invoice.getLegacy().isLegacy()) {
      throw new BusinessRuleException(
          "CMR_BATCH_NOT_LEGACY", invoiceNo + " is not a legacy invoice");
    }
    return invoice;
  }

  /**
   * The open premium receivable.
   *
   * @param invoiceNo invoice
   * @return the sum of the positive premium receivable balances
   */
  @Transactional(readOnly = true)
  public BigDecimal of(String invoiceNo) {
    BigDecimal total = BigDecimal.ZERO;
    for (OpsInvoiceComponent c : legacy(invoiceNo).getComponents()) {
      if (c.getComponent().isPremiumReceivable() && c.getBalance().signum() > 0) {
        total = total.add(c.getBalance());
      }
    }
    return total;
  }
}
