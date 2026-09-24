package com.iortatechnxt.brokerverse.claims.service;

import com.iortatechnxt.brokerverse.claims.domain.Claim;
import com.iortatechnxt.brokerverse.claims.domain.DocumentStatus;
import com.iortatechnxt.brokerverse.claims.domain.RecoveryRepository;
import com.iortatechnxt.brokerverse.claims.domain.ReserveChangeRepository;
import com.iortatechnxt.brokerverse.claims.domain.SettlementRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

/**
 * Guard for decisions that finish a claim (final settlement, close, repudiation, withdrawal): no
 * other document of the claim may still wait for approval, otherwise it could never be approved.
 */
@Component
public class PendingDocuments {

  private static final DocumentStatus PENDING = DocumentStatus.PENDING_APPROVAL;

  private final ReserveChangeRepository reserves;
  private final SettlementRepository settlements;
  private final RecoveryRepository recoveries;

  /**
   * Creates the guard.
   *
   * @param reserves reserve changes
   * @param settlements settlements
   * @param recoveries recoveries
   */
  public PendingDocuments(
      ReserveChangeRepository reserves,
      SettlementRepository settlements,
      RecoveryRepository recoveries) {
    this.reserves = reserves;
    this.settlements = settlements;
    this.recoveries = recoveries;
  }

  /**
   * Fails when a reserve change or settlement of the claim is pending; recoveries too when {@code
   * includeRecoveries} (they remain possible on closed claims, but not on declined ones).
   *
   * @param claim claim
   * @param includeRecoveries whether pending recoveries also block
   */
  public void requireNone(Claim claim, boolean includeRecoveries) {
    Long id = claim.getId();
    boolean pending =
        reserves.existsByClaimIdAndApprovalStatus(id, PENDING)
            || settlements.existsByClaimIdAndApprovalStatus(id, PENDING)
            || includeRecoveries && recoveries.existsByClaimIdAndApprovalStatus(id, PENDING);
    if (pending) {
      throw new BusinessRuleException(
          "CLAIM_HAS_PENDING_DOCUMENTS",
          "Claim " + claim.getClaimNo() + " has documents waiting for approval");
    }
  }
}
