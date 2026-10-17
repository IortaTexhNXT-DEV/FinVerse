package com.iortatechnxt.brokerverse.opsledger.service.adapter;

import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest.DocumentRefs;
import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceCorrectionSink;
import java.util.List;

/**
 * {@link InvoiceCorrectionSink} of the invoice ledger (ACSL 2.9.1, 2.16.0): posts one signed {@code
 * CORRECTION} movement per corrected component, linked to the correcting journal batch. The ledger
 * is idempotent on (source module, source reference, invoice), so a repeated request returns the
 * movements already posted; the invoice lock rules apply as for every movement.
 */
public class LedgerInvoiceCorrectionSink implements InvoiceCorrectionSink {

  private final InvoiceLedgerService ledger;
  private final InvoiceLedgerQueryService query;

  /**
   * Creates the adapter.
   *
   * @param ledger ledger postings
   * @param query ledger reads
   */
  public LedgerInvoiceCorrectionSink(InvoiceLedgerService ledger, InvoiceLedgerQueryService query) {
    this.ledger = ledger;
    this.query = query;
  }

  @Override
  public CorrectionResult record(CorrectionRequest request) {
    List<OpsInvoiceMovement> posted =
        ledger.post(
            new MovementRequest(
                request.invoiceNo(),
                MovementType.CORRECTION,
                request.sourceModule(),
                request.sourceRef(),
                request.valueDate(),
                request.amounts(),
                new DocumentRefs(null, null, null, request.journalBatchNo()),
                request.remarks()));
    return new CorrectionResult(
        request.invoiceNo(), query.require(request.invoiceNo()).getRootInvoiceNo(), posted.size());
  }
}
