package com.iortatechnxt.brokerverse.opsledger.service.adapter;

import com.iortatechnxt.brokerverse.booking.service.port.IncentivePaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The payment status of a booked invoice for its incentive indicator (FR-NB-118): fully paid when
 * the ledger shows the invoice PAID.
 */
@Component
@Transactional(readOnly = true)
public class LedgerIncentivePaymentStatus implements IncentivePaymentStatus {

  private final OpsInvoiceRepository invoices;

  /**
   * Creates the adapter.
   *
   * @param invoices the invoice ledger
   */
  public LedgerIncentivePaymentStatus(OpsInvoiceRepository invoices) {
    this.invoices = invoices;
  }

  @Override
  public boolean fullyPaid(Long companyId, String invoiceNo) {
    return invoices
        .findByInvoiceNo(invoiceNo)
        .map(i -> i.getPaymentStatus() == PaymentStatus.PAID)
        .orElse(false);
  }
}
