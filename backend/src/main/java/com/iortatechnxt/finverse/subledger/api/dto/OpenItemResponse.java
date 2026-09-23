package com.iortatechnxt.finverse.subledger.api.dto;

import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
import com.iortatechnxt.finverse.subledger.domain.OpenItem;
import com.iortatechnxt.finverse.subledger.domain.OpenItemStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Open item view.
 *
 * @param id id
 * @param partyCode party
 * @param direction direction
 * @param documentType document type
 * @param documentNo document number
 * @param documentDate document date
 * @param dueDate due date
 * @param currency currency
 * @param amount amount
 * @param settledAmount settled amount
 * @param outstanding outstanding amount
 * @param status status
 * @param sourceModule source module
 * @param journalBatchNo GL batch
 * @param narration narration
 */
public record OpenItemResponse(
    Long id,
    String partyCode,
    ItemDirection direction,
    String documentType,
    String documentNo,
    LocalDate documentDate,
    LocalDate dueDate,
    String currency,
    BigDecimal amount,
    BigDecimal settledAmount,
    BigDecimal outstanding,
    OpenItemStatus status,
    String sourceModule,
    String journalBatchNo,
    String narration) {

  /**
   * Maps an entity.
   *
   * @param i item
   * @return response
   */
  public static OpenItemResponse from(OpenItem i) {
    return new OpenItemResponse(
        i.getId(),
        i.getPartyCode(),
        i.getDirection(),
        i.getDocumentType(),
        i.getDocumentNo(),
        i.getDocumentDate(),
        i.getDueDate(),
        i.getCurrency(),
        i.getAmount(),
        i.getSettledAmount(),
        i.outstanding(),
        i.getStatus(),
        i.getSourceModule(),
        i.getJournalBatchNo(),
        i.getNarration());
  }
}
