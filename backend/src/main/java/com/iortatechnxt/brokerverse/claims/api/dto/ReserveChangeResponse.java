package com.iortatechnxt.brokerverse.claims.api.dto;

import com.iortatechnxt.brokerverse.claims.domain.CostType;
import com.iortatechnxt.brokerverse.claims.domain.EstimateSide;
import com.iortatechnxt.brokerverse.claims.domain.ReserveChange;
import java.math.BigDecimal;

/**
 * A reserve change.
 *
 * @param id id
 * @param claimId claim
 * @param changeNo sequence within the claim
 * @param side payment or recovery
 * @param costType loss or expense
 * @param newEstimate requested estimate (100 %)
 * @param previousEstimate estimate before approval (100 %)
 * @param changeAmount change applied (100 %)
 * @param ourChange company share of the change
 * @param reason reason
 * @param systemGenerated release made by a close / final settlement / repudiation
 * @param journalBatchNo journal posted
 * @param approval maker-checker state
 */
public record ReserveChangeResponse(
    Long id,
    Long claimId,
    int changeNo,
    EstimateSide side,
    CostType costType,
    BigDecimal newEstimate,
    BigDecimal previousEstimate,
    BigDecimal changeAmount,
    BigDecimal ourChange,
    String reason,
    boolean systemGenerated,
    String journalBatchNo,
    ApprovalResponse approval) {

  /**
   * Maps a reserve change.
   *
   * @param r change
   * @return response
   */
  public static ReserveChangeResponse from(ReserveChange r) {
    return new ReserveChangeResponse(
        r.getId(),
        r.getClaim().getId(),
        r.getChangeNo(),
        r.getSide(),
        r.getCostType(),
        r.getNewEstimate(),
        r.getPreviousEstimate(),
        r.getChangeAmount(),
        r.getOurChange(),
        r.getReason(),
        r.isSystemGenerated(),
        r.getJournalBatchNo(),
        ApprovalResponse.from(r.getApproval()));
  }
}
