package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.domain.OpenItemRole;
import com.iortatechnxt.brokerverse.booking.service.BookingQueryService.InvoiceItem;
import com.iortatechnxt.brokerverse.subledger.domain.ItemDirection;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItem;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItemStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A sub-ledger open item of a booked invoice.
 *
 * @param id open item id
 * @param role client premium, insurer DTIP or insurer commission
 * @param partyCode party
 * @param direction DEBIT or CREDIT
 * @param documentType document type
 * @param amount amount
 * @param settledAmount settled so far
 * @param outstanding outstanding
 * @param status status
 * @param dueDate due date
 * @param journalBatchNo journal batch
 */
public record OpenItemLineResponse(
    Long id,
    OpenItemRole role,
    String partyCode,
    ItemDirection direction,
    String documentType,
    BigDecimal amount,
    BigDecimal settledAmount,
    BigDecimal outstanding,
    OpenItemStatus status,
    LocalDate dueDate,
    String journalBatchNo) {

  /**
   * Maps an item.
   *
   * @param link item with its role
   * @return response
   */
  public static OpenItemLineResponse from(InvoiceItem link) {
    OpenItem i = link.item();
    return new OpenItemLineResponse(
        i.getId(),
        link.role(),
        i.getPartyCode(),
        i.getDirection(),
        i.getDocumentType(),
        i.getAmount(),
        i.getSettledAmount(),
        i.outstanding(),
        i.getStatus(),
        i.getDueDate(),
        i.getJournalBatchNo());
  }
}
