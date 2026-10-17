package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.service.BookingPreviewService.PreviewLine;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import java.math.BigDecimal;

/**
 * One journal line of a preview.
 *
 * @param insurerCode insurer share
 * @param accountCode GL account
 * @param accountName account name
 * @param side debit or credit
 * @param amount amount
 * @param partyCode sub-ledger party
 * @param narration narration
 */
public record PreviewLineResponse(
    String insurerCode,
    String accountCode,
    String accountName,
    BalanceSide side,
    BigDecimal amount,
    String partyCode,
    String narration) {

  /**
   * Maps a preview line.
   *
   * @param l line
   * @return response
   */
  public static PreviewLineResponse from(PreviewLine l) {
    return new PreviewLineResponse(
        l.insurerCode(),
        l.accountCode(),
        l.accountName(),
        l.side(),
        l.amount(),
        l.partyCode(),
        l.narration());
  }
}
