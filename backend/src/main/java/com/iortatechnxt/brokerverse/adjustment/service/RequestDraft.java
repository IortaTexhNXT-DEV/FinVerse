package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestTerms;

/**
 * What a requester enters for an endorsement request (ADJID.001-004/008/023/028).
 *
 * @param invoiceNo booked invoice of the Operations ledger
 * @param terms type, request type, reason, reference, dates, TSI change and description
 * @param amounts amounts of an amount change, may be null
 * @param duplicateOverride why a duplicate request proceeds (ADJID.023), may be null
 * @param baselineOverride why the over-adjustment baseline may be exceeded (ADJID.028), may be null
 */
public record RequestDraft(
    String invoiceNo,
    RequestTerms terms,
    AmountInput amounts,
    String duplicateOverride,
    String baselineOverride) {

  /** Missing amounts are none. */
  public RequestDraft {
    amounts = amounts == null ? AmountInput.NONE : amounts;
    duplicateOverride = blankToNull(duplicateOverride);
    baselineOverride = blankToNull(baselineOverride);
  }

  /**
   * The same draft on another invoice (one request per invoice of a multi-invoice request).
   *
   * @param invoice invoice number
   * @return draft
   */
  public RequestDraft on(String invoice) {
    return new RequestDraft(invoice, terms, amounts, duplicateOverride, baselineOverride);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
