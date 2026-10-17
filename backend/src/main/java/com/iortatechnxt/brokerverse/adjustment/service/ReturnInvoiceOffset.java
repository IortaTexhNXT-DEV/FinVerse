package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequestRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest.DocumentRefs;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.OpsInvoiceBooked;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Keeps a return invoice of an adjustment out of the balances (OPERATIONS_DESIGN 2, one ledger):
 * the posting of a decrease or cancellation already moved the original invoice ({@code ADJUSTED},
 * {@code ADJ:<request>}), so when the return invoice booked by booking reaches the ledger its
 * booked amounts are offset ({@code ADJ:<request>:OFFSET}) and it stays as information only. Runs
 * inside the ledger feed's transaction; return invoices booked outside adjustment are untouched.
 */
@Component
public class ReturnInvoiceOffset {

  private final EndorsementRequestRepository requests;
  private final InvoiceLedgerQueryService queries;
  private final InvoiceLedgerService ledger;

  /**
   * Creates the listener.
   *
   * @param requests requests (by the invoice their posting booked)
   * @param queries ledger reads
   * @param ledger ledger writes
   */
  public ReturnInvoiceOffset(
      EndorsementRequestRepository requests,
      InvoiceLedgerQueryService queries,
      InvoiceLedgerService ledger) {
    this.requests = requests;
    this.queries = queries;
    this.ledger = ledger;
  }

  /**
   * Offsets a return invoice booked by an adjustment posting.
   *
   * @param event invoice copied into the ledger
   */
  @EventListener
  public void on(OpsInvoiceBooked event) {
    if (event.kind().isNegative()) {
      requests
          .findFirstByOutcomeNewInvoiceNo(event.invoiceNo())
          .ifPresent(r -> offset(r, event.invoiceNo()));
    }
  }

  private void offset(EndorsementRequest request, String returnInvoiceNo) {
    OpsInvoice invoice = queries.require(returnInvoiceNo);
    Map<LedgerComponent, BigDecimal> offset = new EnumMap<>(LedgerComponent.class);
    for (OpsInvoiceComponent c : invoice.getComponents()) {
      offset.put(c.getComponent(), c.getBooked().negate());
    }
    String requestNo = request.getRequestNo();
    ledger.post(
        new MovementRequest(
            invoice.getInvoiceNo(),
            MovementType.ADJUSTED,
            Adjustments.MODULE,
            Adjustments.sourceRef(requestNo) + ":OFFSET",
            invoice.getBookingDate(),
            offset,
            new DocumentRefs(null, null, request.outcome().batchNo(), null),
            "Return carried by the original invoice "
                + request.getSubject().invoiceNo()
                + " ("
                + requestNo
                + ")"));
  }
}
