package com.iortatechnxt.finverse.claims.api.dto;

import com.iortatechnxt.finverse.claims.domain.Recovery;
import com.iortatechnxt.finverse.claims.domain.RecoveryType;
import java.math.BigDecimal;

/**
 * A recovery.
 *
 * @param id id
 * @param claimId claim
 * @param recoveryNo recovery number
 * @param recoveryType salvage or subrogation
 * @param fromPartyCode payer code
 * @param fromPartyName payer name
 * @param bankAccountCode bank account
 * @param amount amount (100 %)
 * @param ourAmount company share (on approval)
 * @param coinsurerAmount coinsurers' share payable (on approval)
 * @param narration narration
 * @param journalBatchNo recovery journal
 * @param coinsuranceBatchNo coinsurance journal
 * @param approval maker-checker state
 */
public record RecoveryResponse(
    Long id,
    Long claimId,
    String recoveryNo,
    RecoveryType recoveryType,
    String fromPartyCode,
    String fromPartyName,
    String bankAccountCode,
    BigDecimal amount,
    BigDecimal ourAmount,
    BigDecimal coinsurerAmount,
    String narration,
    String journalBatchNo,
    String coinsuranceBatchNo,
    ApprovalResponse approval) {

  /**
   * Maps a recovery (payer loaded).
   *
   * @param r recovery
   * @return response
   */
  public static RecoveryResponse from(Recovery r) {
    return new RecoveryResponse(
        r.getId(),
        r.getClaim().getId(),
        r.getRecoveryNo(),
        r.getRecoveryType(),
        r.getFromParty() == null ? null : r.getFromParty().getCode(),
        r.getFromParty() == null ? null : r.getFromParty().getName(),
        r.getBankAccountCode(),
        r.getAmount(),
        r.getOurAmount(),
        r.getCoinsurerAmount(),
        r.getNarration(),
        r.getJournalBatchNo(),
        r.getCoinsuranceBatchNo(),
        ApprovalResponse.from(r.getApproval()));
  }
}
