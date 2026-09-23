package com.iortatechnxt.finverse.claims.service;

import com.iortatechnxt.finverse.approval.service.ApprovalViewer;
import com.iortatechnxt.finverse.approval.service.PendingApproval;
import com.iortatechnxt.finverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.finverse.claims.domain.Claim;
import com.iortatechnxt.finverse.claims.domain.ClaimApproval;
import com.iortatechnxt.finverse.claims.domain.DocumentStatus;
import com.iortatechnxt.finverse.claims.domain.RecoveryRepository;
import com.iortatechnxt.finverse.claims.domain.ReserveChangeRepository;
import com.iortatechnxt.finverse.claims.domain.SettlementRepository;
import com.iortatechnxt.finverse.security.service.UserDirectory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Approval inbox source: claim reserve changes, settlements and recoveries waiting for a checker
 * with CLAIM_AUTHORIZE. The maker never sees their own documents; reserve changes and settlements
 * above the viewer's authorization limit are left out (the viewer could not approve them).
 */
@Component
public class ClaimApprovalSource implements PendingApprovalSource {

  private static final String PERMISSION = "CLAIM_AUTHORIZE";
  private static final String MODULE = "CLAIMS";
  private static final DocumentStatus PENDING = DocumentStatus.PENDING_APPROVAL;

  private final ReserveChangeRepository reserves;
  private final SettlementRepository settlements;
  private final RecoveryRepository recoveries;
  private final UserDirectory users;

  /**
   * Creates the source.
   *
   * @param reserves reserve changes
   * @param settlements settlements
   * @param recoveries recoveries
   * @param users authorization limits
   */
  public ClaimApprovalSource(
      ReserveChangeRepository reserves,
      SettlementRepository settlements,
      RecoveryRepository recoveries,
      UserDirectory users) {
    this.reserves = reserves;
    this.settlements = settlements;
    this.recoveries = recoveries;
    this.users = users;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can(PERMISSION)) {
      return List.of();
    }
    BigDecimal limit =
        viewer.systemView() ? null : users.authorizationLimit(viewer.username()).orElse(null);
    List<PendingApproval> items = new ArrayList<>();
    reserves.findByApprovalStatusOrderById(PENDING).stream()
        .filter(r -> visible(viewer, limit, r.getApproval()))
        .map(
            r ->
                item(
                    r.getClaim(),
                    r.getApproval(),
                    "Claim reserve change",
                    r.getClaim().getClaimNo() + "/" + r.getChangeNo(),
                    r.getSide() + " " + r.getCostType() + " estimate: " + r.getReason(),
                    r.getNewEstimate()))
        .forEach(items::add);
    settlements.findByApprovalStatusOrderById(PENDING).stream()
        .filter(s -> visible(viewer, limit, s.getApproval()))
        .map(
            s ->
                item(
                    s.getClaim(),
                    s.getApproval(),
                    "Claim settlement",
                    s.getSettlementNo(),
                    s.getSettlementType() + " to " + s.getPayee().getName(),
                    s.getNetAmount()))
        .forEach(items::add);
    recoveries.findByApprovalStatusOrderById(PENDING).stream()
        .filter(r -> viewer.mayApproveItemOf(r.getApproval().getSubmittedBy()))
        .map(
            r ->
                item(
                    r.getClaim(),
                    r.getApproval(),
                    "Claim recovery",
                    r.getRecoveryNo(),
                    r.getRecoveryType() + " recovery",
                    r.getAmount()))
        .forEach(items::add);
    return items;
  }

  /** Visible to the viewer: not their own, and within their limit (null = unlimited). */
  private static boolean visible(ApprovalViewer viewer, BigDecimal limit, ClaimApproval approval) {
    return viewer.mayApproveItemOf(approval.getSubmittedBy())
        && (limit == null || approval.getAuthorityAmount().compareTo(limit) <= 0);
  }

  private static PendingApproval item(
      Claim claim,
      ClaimApproval approval,
      String type,
      String reference,
      String description,
      BigDecimal amount) {
    return new PendingApproval(
        MODULE,
        type,
        reference,
        claim.getClaimNo() + " · " + claim.getPolicy().getInsuredName() + " · " + description,
        amount,
        claim.getCurrency(),
        approval.getSubmittedBy(),
        approval.getSubmittedAt(),
        claim.getCompanyId(),
        "/claims/" + claim.getId());
  }
}
