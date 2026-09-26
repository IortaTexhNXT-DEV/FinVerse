package com.iortatechnxt.brokerverse.consolidation.api.dto;

import com.iortatechnxt.brokerverse.consolidation.service.IntercompanyReconciliationService.ReconciliationLine;
import java.math.BigDecimal;

/**
 * Inter-company reconciliation line.
 *
 * @param relationshipId relationship
 * @param creditorCompanyId company holding the due-from
 * @param debtorCompanyId company holding the due-to
 * @param dueFromAccount due-from account
 * @param dueToAccount due-to account
 * @param currency transaction currency
 * @param dueFromFc receivable (transaction currency)
 * @param dueToFc payable (transaction currency, credit positive)
 * @param differenceFc difference
 * @param dueFromBase receivable in creditor base currency
 * @param dueToBase payable in debtor base currency
 * @param matched true when both sides agree
 */
public record ReconciliationLineResponse(
    Long relationshipId,
    Long creditorCompanyId,
    Long debtorCompanyId,
    String dueFromAccount,
    String dueToAccount,
    String currency,
    BigDecimal dueFromFc,
    BigDecimal dueToFc,
    BigDecimal differenceFc,
    BigDecimal dueFromBase,
    BigDecimal dueToBase,
    boolean matched) {

  /**
   * Maps a line.
   *
   * @param l line
   * @return view
   */
  public static ReconciliationLineResponse from(ReconciliationLine l) {
    return new ReconciliationLineResponse(
        l.relationshipId(),
        l.creditorCompanyId(),
        l.debtorCompanyId(),
        l.dueFromAccount(),
        l.dueToAccount(),
        l.currency(),
        l.dueFromFc(),
        l.dueToFc(),
        l.difference(),
        l.dueFromBase(),
        l.dueToBase(),
        l.matched());
  }
}
