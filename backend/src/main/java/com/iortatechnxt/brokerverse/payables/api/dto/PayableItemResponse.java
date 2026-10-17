package com.iortatechnxt.brokerverse.payables.api.dto;

import com.iortatechnxt.brokerverse.payables.service.PayableItem;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItem;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Open payable that can be selected on a payment voucher.
 *
 * @param openItemId open item
 * @param documentType document type
 * @param documentNo document number
 * @param documentDate document date
 * @param dueDate due date
 * @param currency currency
 * @param amount original amount
 * @param outstanding unsettled amount
 * @param available amount not reserved by other vouchers
 * @param narration narration
 */
public record PayableItemResponse(
    Long openItemId,
    String documentType,
    String documentNo,
    LocalDate documentDate,
    LocalDate dueDate,
    String currency,
    BigDecimal amount,
    BigDecimal outstanding,
    BigDecimal available,
    String narration) {

  /**
   * Maps a payable item.
   *
   * @param p payable
   * @return response
   */
  public static PayableItemResponse from(PayableItem p) {
    OpenItem i = p.item();
    return new PayableItemResponse(
        i.getId(),
        i.getDocumentType(),
        i.getDocumentNo(),
        i.getDocumentDate(),
        i.getDueDate(),
        i.getCurrency(),
        i.getAmount(),
        i.outstanding(),
        p.available(),
        i.getNarration());
  }
}
