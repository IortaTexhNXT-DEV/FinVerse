package com.iortatechnxt.brokerverse.prodrecon.service;

import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconExtractLine.LastPayment;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItem.BdoiFacts;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconSide;
import java.util.List;

/** The BDOI side of a reconciliation item from a ledger invoice (PRCID.012/027). */
final class ReconFacts {

  private ReconFacts() {}

  /**
   * The facts of a booked invoice (components loaded).
   *
   * @param invoice ledger invoice
   * @param extractLineId register line it was sent on, may be null
   * @return facts
   */
  static BdoiFacts of(OpsInvoice invoice, Long extractLineId) {
    var c = invoice.getClassification();
    return new BdoiFacts(
        invoice.getInvoiceNo(),
        extractLineId,
        invoice.getArn(),
        c.bookingDate(),
        c.aoUsername(),
        c.salesUnit(),
        invoice.getBranchId(),
        c.segment(),
        c.productLine(),
        side(invoice));
  }

  /**
   * The compared fields of a booked invoice.
   *
   * @param invoice ledger invoice
   * @return side
   */
  static ReconSide side(OpsInvoice invoice) {
    var c = invoice.getClassification();
    return new ReconSide(
        invoice.getPolicyNo(),
        invoice.getInvoiceNo(),
        invoice.getPnNos(),
        c.inceptionDate(),
        c.expiryDate(),
        invoice.getAssuredName(),
        invoice.getCommission(),
        invoice.component(LedgerComponent.BASIC).due(),
        invoice.getGrossPremium());
  }

  /**
   * The last payment applied to an invoice.
   *
   * @param movements the invoice's movements in posting order
   * @return date and AR of the last application, none when unpaid
   */
  static LastPayment lastPayment(List<OpsInvoiceMovement> movements) {
    LastPayment last = LastPayment.NONE;
    for (OpsInvoiceMovement m : movements) {
      if (m.getMovementType() == MovementType.APPLIED && m.getAmount().signum() > 0) {
        last = new LastPayment(m.getValueDate(), m.getArNo());
      }
    }
    return last;
  }
}
