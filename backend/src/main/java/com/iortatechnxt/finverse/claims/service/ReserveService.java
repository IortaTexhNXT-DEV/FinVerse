package com.iortatechnxt.finverse.claims.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.claims.domain.Claim;
import com.iortatechnxt.finverse.claims.domain.ClaimApproval;
import com.iortatechnxt.finverse.claims.domain.ClaimPolicy;
import com.iortatechnxt.finverse.claims.domain.CostType;
import com.iortatechnxt.finverse.claims.domain.DocumentStatus;
import com.iortatechnxt.finverse.claims.domain.EstimateLine;
import com.iortatechnxt.finverse.claims.domain.EstimateSide;
import com.iortatechnxt.finverse.claims.domain.ReserveChange;
import com.iortatechnxt.finverse.claims.domain.ReserveChangeRepository;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Claim reserves (estimates) under maker-checker control.
 *
 * <p>The maker requests a new estimate for one side (payment / recovery) and cost type (loss /
 * expense). The checker, who must not be the maker, approves it: when the payment estimate
 * increases, the resulting total payment estimate of the claim (company share, base currency) must
 * be within the checker's authorization limit. Approval applies the change to the claim, posts
 * CLAIM_RESERVE with the signed company-share delta and notifies the kernel listeners.
 */
@Service
@Transactional
public class ReserveService {

  static final String ENTITY = "ReserveChange";

  private final ReserveChangeRepository changes;
  private final ClaimSupport support;
  private final ClaimPostingService posting;
  private final ClaimAlerts alerts;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param changes reserve change repository
   * @param support claims helpers
   * @param posting accounting
   * @param alerts exception rules
   * @param audit audit trail
   */
  public ReserveService(
      ReserveChangeRepository changes,
      ClaimSupport support,
      ClaimPostingService posting,
      ClaimAlerts alerts,
      AuditTrailService audit) {
    this.changes = changes;
    this.support = support;
    this.posting = posting;
    this.alerts = alerts;
    this.audit = audit;
  }

  /**
   * Reserve changes of a claim.
   *
   * @param claimId claim
   * @return changes in sequence
   */
  @Transactional(readOnly = true)
  public List<ReserveChange> forClaim(Long claimId) {
    return changes.findByClaimIdOrderByChangeNo(claimId);
  }

  /**
   * Gets a reserve change with its claim.
   *
   * @param id id
   * @return change
   */
  @Transactional(readOnly = true)
  public ReserveChange get(Long id) {
    return changes
        .findWithClaimById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Maker requests a new estimate (pending approval).
   *
   * @param claimId claim
   * @param line side, cost type and new estimate at 100 %
   * @param reason reason for the change
   * @return pending change
   */
  public ReserveChange request(Long claimId, EstimateLine line, String reason) {
    Claim claim = support.claim(claimId);
    claim.requireActive("change the reserve of");
    requireChange(claim, line);
    if (changes.existsByClaimIdAndSideAndCostTypeAndApprovalStatus(
        claimId, line.side(), line.costType(), DocumentStatus.PENDING_APPROVAL)) {
      throw new BusinessRuleException(
          "RESERVE_CHANGE_PENDING",
          "A " + line.side() + "/" + line.costType() + " reserve change is already pending");
    }
    ReserveChange rc =
        changes.save(
            new ReserveChange(
                claim,
                changes.lastChangeNo(claimId) + 1,
                line,
                reason,
                new ClaimApproval(
                    support.user(),
                    support.now(),
                    authority(claim, line, support.dateOrToday(null)))));
    audit.record(ENTITY, rc.label(), AuditAction.CREATE, "Requested estimate " + line.amount());
    return rc;
  }

  /**
   * Checker approves a change: applies, posts and notifies.
   *
   * @param id change
   * @param accountingDate accounting date, null for today
   * @return approved change
   */
  public ReserveChange approve(Long id, LocalDate accountingDate) {
    ReserveChange rc = get(id);
    Claim claim = rc.getClaim();
    claim.requireActive("change the reserve of");
    LocalDate date = support.accountingDate(claim, accountingDate);
    rc.approve(support.user(), support.now(), date);
    BigDecimal authority =
        authority(
            claim, new EstimateLine(rc.getSide(), rc.getCostType(), rc.getNewEstimate()), date);
    rc.getApproval().setAuthorityAmount(authority);
    support.requireWithinLimit(authority, rc.label());
    apply(rc, date);
    audit.record(
        ENTITY,
        rc.label(),
        AuditAction.AUTHORIZE,
        "Approved change " + rc.getChangeAmount() + " (company share " + rc.getOurChange() + ")");
    return rc;
  }

  /**
   * Checker rejects a change.
   *
   * @param id change
   * @param reason reason
   * @return rejected change
   */
  public ReserveChange reject(Long id, String reason) {
    ReserveChange rc = get(id);
    rc.reject(support.user(), support.now(), reason);
    audit.record(ENTITY, rc.label(), AuditAction.REJECT, reason);
    return rc;
  }

  /**
   * Releases the whole outstanding payment reserve of a claim through system changes approved as
   * part of the caller's decision (final settlement, close, repudiation, withdrawal).
   *
   * @param claim claim
   * @param date accounting date
   * @param reason reason recorded on the changes
   */
  public void releaseOutstanding(Claim claim, LocalDate date, String reason) {
    for (CostType cost : CostType.values()) {
      if (claim.getTotals().outstanding(EstimateSide.PAYMENT, cost).signum() > 0) {
        EstimateLine released =
            new EstimateLine(
                EstimateSide.PAYMENT, cost, claim.getTotals().paid(EstimateSide.PAYMENT, cost));
        ReserveChange rc =
            changes.save(
                new ReserveChange(
                    claim,
                    changes.lastChangeNo(claim.getId()) + 1,
                    released,
                    reason,
                    ClaimApproval.approvedBy(support.user(), support.now(), date)));
        apply(rc, date);
        audit.record(ENTITY, rc.label(), AuditAction.AUTHORIZE, reason);
      }
    }
  }

  private void apply(ReserveChange rc, LocalDate date) {
    Claim claim = rc.getClaim();
    ClaimPolicy policy = claim.getPolicy();
    BigDecimal previous = claim.getTotals().estimate(rc.getSide(), rc.getCostType());
    claim.applyEstimate(rc.getSide(), rc.getCostType(), rc.getNewEstimate());
    rc.recordApplied(
        previous, policy.ourShare(rc.getNewEstimate()).subtract(policy.ourShare(previous)));
    posting.postEstimate(rc, date);
    if (rc.getSide() == EstimateSide.PAYMENT) {
      alerts.checkReserve(claim, support.toBase(claim, claim.ourEstimate(), date));
    }
  }

  private static void requireChange(Claim claim, EstimateLine line) {
    if (line.side() == EstimateSide.RECOVERY && line.costType() != CostType.LOSS) {
      throw new BusinessRuleException(
          "RECOVERY_IS_LOSS_ONLY", "Recovery estimates relate to the loss only");
    }
    BigDecimal current = claim.getTotals().estimate(line.side(), line.costType());
    if (line.amount().compareTo(current) == 0) {
      throw new BusinessRuleException(
          "NO_RESERVE_CHANGE", "The new estimate equals the current estimate " + current);
    }
    if (line.amount().compareTo(claim.getTotals().paid(line.side(), line.costType())) < 0) {
      throw new BusinessRuleException(
          "ESTIMATE_BELOW_PAID", "The new estimate is below the amount already settled");
    }
  }

  /**
   * Amount subject to the checker's limit: for payment estimate increases, the resulting total
   * payment estimate of the claim (company share, base currency); zero for decreases and recovery
   * estimates, which reduce the exposure.
   */
  private BigDecimal authority(Claim claim, EstimateLine line, LocalDate date) {
    BigDecimal current = claim.getTotals().estimate(line.side(), line.costType());
    if (line.side() != EstimateSide.PAYMENT || line.amount().compareTo(current) <= 0) {
      return BigDecimal.ZERO;
    }
    BigDecimal total = claim.getTotals().paymentEstimate().subtract(current).add(line.amount());
    return support.toBase(claim, claim.getPolicy().ourShare(total), date);
  }
}
