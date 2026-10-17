package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.service.BookingQueryService.PostedLine;
import java.math.BigDecimal;

/**
 * A journal line posted for a booked invoice.
 *
 * @param batchId journal batch
 * @param batchNo journal batch number
 * @param accountCode GL account
 * @param accountName GL account name
 * @param side debit or credit
 * @param amount amount
 * @param partyCode sub-ledger party
 * @param narration narration
 */
public record PostedLineResponse(
    Long batchId,
    String batchNo,
    String accountCode,
    String accountName,
    String side,
    BigDecimal amount,
    String partyCode,
    String narration) {

  /**
   * Maps a posted line.
   *
   * @param l line
   * @return response
   */
  public static PostedLineResponse from(PostedLine l) {
    return new PostedLineResponse(
        l.batchId(),
        l.batchNo(),
        l.accountCode(),
        l.accountName(),
        l.side().name(),
        l.amount(),
        l.partyCode(),
        l.narration());
  }
}
