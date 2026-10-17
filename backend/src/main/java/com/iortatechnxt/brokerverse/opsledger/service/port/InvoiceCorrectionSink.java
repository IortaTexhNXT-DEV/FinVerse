package com.iortatechnxt.brokerverse.opsledger.service.port;

import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Port: record an ACSL correction on the invoice ledger (ACSL 2.9.1, 2.16.0;
 * ACCOUNTING_DISBURSEMENT_DESIGN 2.2). When a correction entry posts, the corrected components of
 * the invoice move by a signed {@code CORRECTION} movement linked to the correcting journal; the
 * original movements stay. Implemented in {@code opsledger} itself ({@code
 * LedgerInvoiceCorrectionSink}); called by acsl inside its posting transaction.
 */
public interface InvoiceCorrectionSink {

  /**
   * Records a correction, idempotent on (source module, source reference, invoice).
   *
   * @param request invoice, signed amounts per component and source
   * @return what was recorded
   */
  CorrectionResult record(CorrectionRequest request);

  /**
   * A correction of an invoice.
   *
   * @param invoiceNo invoice
   * @param amounts signed change per component (zero components are ignored)
   * @param sourceModule module (ACSL)
   * @param sourceRef its reference (the correction case)
   * @param valueDate value date
   * @param journalBatchNo correcting journal batch
   * @param remarks remarks, may be null
   */
  record CorrectionRequest(
      String invoiceNo,
      Map<LedgerComponent, BigDecimal> amounts,
      String sourceModule,
      String sourceRef,
      LocalDate valueDate,
      String journalBatchNo,
      String remarks) {

    /** Defensive copy. */
    public CorrectionRequest {
      amounts = Map.copyOf(amounts);
    }
  }

  /**
   * What was recorded.
   *
   * @param invoiceNo invoice
   * @param rootInvoiceNo root invoice of its family
   * @param movements number of movements of the correction (the same on a repeated request)
   */
  record CorrectionResult(String invoiceNo, String rootInvoiceNo, int movements) {}
}
