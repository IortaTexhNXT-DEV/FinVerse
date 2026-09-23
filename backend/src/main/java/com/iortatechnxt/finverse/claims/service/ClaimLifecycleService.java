package com.iortatechnxt.finverse.claims.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.claims.domain.Claim;
import com.iortatechnxt.finverse.claims.domain.ClaimStatus;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decisions that end or resume the handling of a claim. They are checker decisions (permission
 * CLAIM_AUTHORIZE, never by the user who registered the claim) and release any outstanding reserve
 * through system reserve changes in the same transaction:
 *
 * <ul>
 *   <li>close: open / partially settled / reopened claim → CLOSED;
 *   <li>reopen: CLOSED → REOPENED (a new reserve is then requested through maker-checker);
 *   <li>repudiate / withdraw: claim with nothing paid → REJECTED / WITHDRAWN, with reason.
 * </ul>
 */
@Service
@Transactional
public class ClaimLifecycleService {

  private final ClaimSupport support;
  private final ReserveService reserves;
  private final PendingDocuments pending;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param support claims helpers
   * @param reserves reserve releases
   * @param pending pending document guard
   * @param audit audit trail
   */
  public ClaimLifecycleService(
      ClaimSupport support,
      ReserveService reserves,
      PendingDocuments pending,
      AuditTrailService audit) {
    this.support = support;
    this.reserves = reserves;
    this.pending = pending;
    this.audit = audit;
  }

  /**
   * Closes a claim, releasing its outstanding reserve.
   *
   * @param claimId claim
   * @param reason reason
   * @param accountingDate date of the release, null for today
   * @return closed claim
   */
  public Claim close(Long claimId, String reason, LocalDate accountingDate) {
    Claim claim = decide(claimId);
    claim.requireSettleable("close");
    pending.requireNone(claim, false);
    LocalDate date = support.accountingDate(claim, accountingDate);
    reserves.releaseOutstanding(claim, date, "Reserve released on closing: " + reason);
    claim.close(date, reason);
    audit.record(ClaimSupport.CLAIM_ENTITY, claim.getClaimNo(), AuditAction.CLOSE, reason);
    return claim;
  }

  /**
   * Reopens a closed claim.
   *
   * @param claimId claim
   * @param reason reason
   * @return reopened claim
   */
  public Claim reopen(Long claimId, String reason) {
    Claim claim = decide(claimId);
    claim.reopen(reason);
    audit.record(ClaimSupport.CLAIM_ENTITY, claim.getClaimNo(), AuditAction.REOPEN, reason);
    return claim;
  }

  /**
   * Repudiates (REJECTED) or records the withdrawal (WITHDRAWN) of a claim on which nothing was
   * paid, releasing its reserve.
   *
   * @param claimId claim
   * @param target REJECTED or WITHDRAWN
   * @param reason reason
   * @param accountingDate date of the release, null for today
   * @return declined claim
   */
  public Claim decline(Long claimId, ClaimStatus target, String reason, LocalDate accountingDate) {
    Claim claim = decide(claimId);
    claim.requireDeclinable();
    pending.requireNone(claim, true);
    LocalDate date = support.accountingDate(claim, accountingDate);
    reserves.releaseOutstanding(claim, date, "Reserve released, claim " + target + ": " + reason);
    claim.decline(target, date, reason);
    audit.record(
        ClaimSupport.CLAIM_ENTITY, claim.getClaimNo(), AuditAction.REJECT, target + ": " + reason);
    return claim;
  }

  private Claim decide(Long claimId) {
    Claim claim = support.claim(claimId);
    if (Objects.equals(claim.getCreatedBy(), support.user())) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION",
          "Claim " + claim.getClaimNo() + " cannot be decided by the user who registered it");
    }
    return claim;
  }
}
