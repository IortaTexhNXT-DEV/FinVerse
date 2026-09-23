package com.iortatechnxt.finverse.claims.api.dto;

import com.iortatechnxt.finverse.claims.domain.CostType;
import com.iortatechnxt.finverse.claims.domain.Settlement;
import com.iortatechnxt.finverse.claims.domain.SettlementType;
import java.math.BigDecimal;

/**
 * A settlement.
 *
 * @param id id
 * @param claimId claim
 * @param settlementNo settlement number
 * @param payeeCode payee code
 * @param payeeName payee name
 * @param costType loss or expense
 * @param settlementType partial or final
 * @param assessedAmount assessed (100 %)
 * @param deductible deductible (100 %)
 * @param excessAmount excess (100 %)
 * @param netAmount net (100 %)
 * @param ourAmount company share (on approval)
 * @param payableAmount paid to the payee (on approval)
 * @param coinsurerAmount coinsurers' share recoverable (on approval)
 * @param exchangeRate rate to base currency (on approval)
 * @param narration narration
 * @param journalBatchNo settlement journal
 * @param coinsuranceBatchNo coinsurance journal
 * @param approval maker-checker state
 */
public record SettlementResponse(
    Long id,
    Long claimId,
    String settlementNo,
    String payeeCode,
    String payeeName,
    CostType costType,
    SettlementType settlementType,
    BigDecimal assessedAmount,
    BigDecimal deductible,
    BigDecimal excessAmount,
    BigDecimal netAmount,
    BigDecimal ourAmount,
    BigDecimal payableAmount,
    BigDecimal coinsurerAmount,
    BigDecimal exchangeRate,
    String narration,
    String journalBatchNo,
    String coinsuranceBatchNo,
    ApprovalResponse approval) {

  /**
   * Maps a settlement (payee loaded).
   *
   * @param s settlement
   * @return response
   */
  public static SettlementResponse from(Settlement s) {
    return new SettlementResponse(
        s.getId(),
        s.getClaim().getId(),
        s.getSettlementNo(),
        s.getPayee().getCode(),
        s.getPayee().getName(),
        s.getCostType(),
        s.getSettlementType(),
        s.getAssessedAmount(),
        s.getDeductible(),
        s.getExcessAmount(),
        s.getNetAmount(),
        s.getOurAmount(),
        s.getPayableAmount(),
        s.getCoinsurerAmount(),
        s.getExchangeRate(),
        s.getNarration(),
        s.getJournalBatchNo(),
        s.getCoinsuranceBatchNo(),
        ApprovalResponse.from(s.getApproval()));
  }
}
