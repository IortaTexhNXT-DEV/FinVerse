package com.iortatechnxt.brokerverse.journal.api.dto;

import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.journal.domain.JournalLine;
import java.math.BigDecimal;

/**
 * Journal line view.
 *
 * @param lineNo line number
 * @param accountId account id
 * @param accountCode account code
 * @param accountName account name
 * @param branchId branch
 * @param side side
 * @param currency currency
 * @param amount amount
 * @param exchangeRate rate
 * @param baseAmount base amount
 * @param costCenter cost centre
 * @param businessLine business line
 * @param partyCode party
 * @param reference reference
 * @param narration narration
 */
public record JournalLineResponse(
    int lineNo,
    Long accountId,
    String accountCode,
    String accountName,
    Long branchId,
    BalanceSide side,
    String currency,
    BigDecimal amount,
    BigDecimal exchangeRate,
    BigDecimal baseAmount,
    String costCenter,
    String businessLine,
    String partyCode,
    String reference,
    String narration) {

  /**
   * Maps an entity.
   *
   * @param l line
   * @return response
   */
  public static JournalLineResponse from(JournalLine l) {
    return new JournalLineResponse(
        l.getLineNo(),
        l.getAccount().getId(),
        l.getAccount().getCode(),
        l.getAccount().getName(),
        l.getBranchId(),
        l.getSide(),
        l.getCurrency(),
        l.getAmount(),
        l.getExchangeRate(),
        l.getBaseAmount(),
        l.getCostCenter(),
        l.getBusinessLine(),
        l.getPartyCode(),
        l.getReference(),
        l.getNarration());
  }
}
