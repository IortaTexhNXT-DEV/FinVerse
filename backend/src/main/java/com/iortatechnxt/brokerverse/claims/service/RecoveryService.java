package com.iortatechnxt.brokerverse.claims.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.claims.domain.Claim;
import com.iortatechnxt.brokerverse.claims.domain.ClaimApproval;
import com.iortatechnxt.brokerverse.claims.domain.CostType;
import com.iortatechnxt.brokerverse.claims.domain.EstimateSide;
import com.iortatechnxt.brokerverse.claims.domain.Recovery;
import com.iortatechnxt.brokerverse.claims.domain.RecoveryRepository;
import com.iortatechnxt.brokerverse.claims.domain.RecoveryTerms;
import com.iortatechnxt.brokerverse.claims.domain.ShareSplit;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Salvage and subrogation recoveries under maker-checker control. A recovery must be covered by the
 * claim's recovery estimate; approval posts CLAIM_RECOVERY (Dr bank / Cr claims paid, company
 * share) and notifies the kernel listeners. Money received is not subject to authorization limits.
 */
@Service
@Transactional
public class RecoveryService {

  static final String ENTITY = "Recovery";

  private static final String RECOVER = "record a recovery on";

  private final RecoveryRepository recoveries;
  private final PartyService parties;
  private final ClaimSupport support;
  private final ClaimPostingService posting;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param recoveries recovery repository
   * @param parties payer lookup
   * @param support claims helpers
   * @param posting accounting
   * @param audit audit trail
   */
  public RecoveryService(
      RecoveryRepository recoveries,
      PartyService parties,
      ClaimSupport support,
      ClaimPostingService posting,
      AuditTrailService audit) {
    this.recoveries = recoveries;
    this.parties = parties;
    this.support = support;
    this.posting = posting;
    this.audit = audit;
  }

  /**
   * Recoveries of a claim.
   *
   * @param claimId claim
   * @return recoveries with payer
   */
  @Transactional(readOnly = true)
  public List<Recovery> forClaim(Long claimId) {
    return recoveries.findByClaimIdOrderById(claimId);
  }

  /**
   * Gets a recovery with claim and payer.
   *
   * @param id id
   * @return recovery
   */
  @Transactional(readOnly = true)
  public Recovery get(Long id) {
    return recoveries
        .findWithDetailsById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Maker records money recovered (pending approval).
   *
   * @param claimId claim
   * @param command type, payer, bank account, amount and narration
   * @return pending recovery
   */
  public Recovery create(Long claimId, RecoveryCommand command) {
    Claim claim = support.claim(claimId);
    claim.requireRecoverable(RECOVER);
    claim
        .getTotals()
        .requireWithinOutstanding(EstimateSide.RECOVERY, CostType.LOSS, command.amount());
    Party payer =
        command.fromPartyCode() == null || command.fromPartyCode().isBlank()
            ? null
            : parties.getByCode(claim.getCompanyId(), command.fromPartyCode());
    RecoveryTerms terms =
        new RecoveryTerms(
            command.recoveryType(),
            payer,
            command.bankAccountCode(),
            command.amount(),
            command.narration());
    Recovery r =
        recoveries.save(
            new Recovery(
                claim,
                support.nextNumber("CR", claim.getBranchId(), support.dateOrToday(null)),
                terms,
                new ClaimApproval(support.user(), support.now(), BigDecimal.ZERO)));
    audit.record(ENTITY, r.getRecoveryNo(), AuditAction.CREATE, "Recorded " + r.getAmount());
    return r;
  }

  /**
   * Checker approves a recovery: consumes the recovery estimate, posts and notifies.
   *
   * @param id recovery
   * @param accountingDate accounting date, null for today
   * @return approved recovery
   */
  public Recovery approve(Long id, LocalDate accountingDate) {
    Recovery r = get(id);
    Claim claim = r.getClaim();
    claim.requireRecoverable(RECOVER);
    LocalDate date = support.accountingDate(claim, accountingDate);
    r.approve(support.user(), support.now(), date);
    ShareSplit split =
        claim
            .getPolicy()
            .split(claim.getTotals().paid(EstimateSide.RECOVERY, CostType.LOSS), r.getAmount());
    r.getApproval().setAuthorityAmount(support.toBase(claim, split.ours(), date));
    claim.applyRecovery(r.getAmount());
    posting.postRecovery(r, split, date);
    audit.record(
        ENTITY,
        r.getRecoveryNo(),
        AuditAction.AUTHORIZE,
        "Approved, company share " + split.ours());
    return r;
  }

  /**
   * Checker rejects a recovery.
   *
   * @param id recovery
   * @param reason reason
   * @return rejected recovery
   */
  public Recovery reject(Long id, String reason) {
    Recovery r = get(id);
    r.reject(support.user(), support.now(), reason);
    audit.record(ENTITY, r.getRecoveryNo(), AuditAction.REJECT, reason);
    return r;
  }
}
