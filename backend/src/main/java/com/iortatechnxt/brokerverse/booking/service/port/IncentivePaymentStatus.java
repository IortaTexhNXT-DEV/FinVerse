package com.iortatechnxt.brokerverse.booking.service.port;

/**
 * Port to the payment status of a booked invoice (FR-NB-118): the incentive indicator is set only
 * when the invoice is fully paid. Operations implements it from the invoice ledger.
 */
public interface IncentivePaymentStatus {

  /**
   * Whether an invoice is fully paid.
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @return true when the balance of the invoice is cleared
   */
  boolean fullyPaid(Long companyId, String invoiceNo);
}
