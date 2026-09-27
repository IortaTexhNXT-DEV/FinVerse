package com.iortatechnxt.brokerverse.opsledger.service.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Port: un-apply and re-apply the payments of an invoice after its premium changed (ADJID.009/012/
 * 013: decrease, cancellation paid but not remitted). Implemented by cashiering, which reverses the
 * applications beyond the new outstanding premium, re-applies by the component hierarchy
 * (CSHID.022) and moves the excess to an unapplied item. Called by adjustment inside its posting
 * transaction.
 *
 * <p>The default adapter (no cashiering module) returns a zero result for an invoice without
 * applied payments and refuses ({@code PAYMENT_REAPPLIER_UNAVAILABLE}) an invoice with payments.
 */
public interface PaymentReapplier {

  /**
   * Re-applies the payments of an invoice, idempotent on (source module, source reference).
   *
   * @param request invoice and source
   * @return amounts un-applied and the excess created
   */
  ReapplyResult reapply(ReapplyRequest request);

  /**
   * A re-application.
   *
   * @param invoiceNo invoice whose premium changed
   * @param sourceModule module asking (ADJUSTMENT)
   * @param sourceRef its reference (endorsement request)
   * @param valueDate value date
   * @param reason reason shown on the reversal
   */
  record ReapplyRequest(
      String invoiceNo,
      String sourceModule,
      String sourceRef,
      LocalDate valueDate,
      String reason) {}

  /**
   * What the re-application did.
   *
   * @param invoiceNo invoice
   * @param unapplied payments taken off the invoice
   * @param excess amount moved to an unapplied item (zero when everything was re-applied)
   * @param receiptNos receipts whose applications changed
   * @param unappliedRef reference of the unapplied item created, null when no excess
   */
  record ReapplyResult(
      String invoiceNo,
      BigDecimal unapplied,
      BigDecimal excess,
      List<String> receiptNos,
      String unappliedRef) {

    /** Defensive copy. */
    public ReapplyResult {
      receiptNos = List.copyOf(receiptNos);
    }

    /**
     * Nothing to re-apply.
     *
     * @param invoiceNo invoice
     * @return zero result
     */
    public static ReapplyResult none(String invoiceNo) {
      return new ReapplyResult(invoiceNo, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), null);
    }
  }
}
