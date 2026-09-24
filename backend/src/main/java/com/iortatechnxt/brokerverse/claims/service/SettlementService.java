package com.iortatechnxt.brokerverse.claims.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.claims.domain.Claim;
import com.iortatechnxt.brokerverse.claims.domain.ClaimApproval;
import com.iortatechnxt.brokerverse.claims.domain.EstimateSide;
import com.iortatechnxt.brokerverse.claims.domain.Settlement;
import com.iortatechnxt.brokerverse.claims.domain.SettlementRepository;
import com.iortatechnxt.brokerverse.claims.domain.SettlementTerms;
import com.iortatechnxt.brokerverse.claims.domain.SettlementType;
import com.iortatechnxt.brokerverse.claims.domain.ShareSplit;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Claim settlements (partial or final) under maker-checker control.
 *
 * <p>Approval, in one transaction: the checker (not the maker, within their authorization limit for
 * the amount payable in base currency) approves; the net amount consumes the outstanding reserve of
 * its cost type; CLAIM_SETTLEMENT is posted and the payee's payable recorded (see {@link
 * ClaimPostingService}); a final settlement then releases the remaining reserve and closes the
 * claim.
 */
@Service
@Transactional
public class SettlementService {

  static final String ENTITY = "Settlement";

  /** Party types that may be paid under a claim (same as the payables CLAIM category). */
  static final Set<PartyType> PAYEE_TYPES =
      EnumSet.of(
          PartyType.INDIVIDUAL_CLIENT,
          PartyType.CORPORATE_CLIENT,
          PartyType.GARAGE,
          PartyType.SURVEYOR);

  private static final String SETTLE = "settle";

  private final SettlementRepository settlements;
  private final PartyService parties;
  private final ClaimSupport support;
  private final ClaimPostingService posting;
  private final ReserveService reserves;
  private final PendingDocuments pending;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param settlements settlement repository
   * @param parties payee lookup
   * @param support claims helpers
   * @param posting accounting
   * @param reserves reserve releases on final settlement
   * @param pending pending document guard
   * @param audit audit trail
   */
  public SettlementService(
      SettlementRepository settlements,
      PartyService parties,
      ClaimSupport support,
      ClaimPostingService posting,
      ReserveService reserves,
      PendingDocuments pending,
      AuditTrailService audit) {
    this.settlements = settlements;
    this.parties = parties;
    this.support = support;
    this.posting = posting;
    this.reserves = reserves;
    this.pending = pending;
    this.audit = audit;
  }

  /**
   * Settlements of a claim.
   *
   * @param claimId claim
   * @return settlements with payee
   */
  @Transactional(readOnly = true)
  public List<Settlement> forClaim(Long claimId) {
    return settlements.findByClaimIdOrderById(claimId);
  }

  /**
   * Gets a settlement with claim and payee.
   *
   * @param id id
   * @return settlement
   */
  @Transactional(readOnly = true)
  public Settlement get(Long id) {
    return settlements
        .findWithDetailsById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Maker enters a settlement (pending approval). The net amount must be covered by the outstanding
   * reserve of its cost type.
   *
   * @param claimId claim
   * @param command payee, cost type, type and amounts
   * @return pending settlement
   */
  public Settlement create(Long claimId, SettlementCommand command) {
    Claim claim = support.claim(claimId);
    claim.requireSettleable(SETTLE);
    Party payee = parties.requireActive(claim.getCompanyId(), command.payeeCode(), PAYEE_TYPES);
    SettlementTerms terms = command.toTerms(payee);
    LocalDate today = support.dateOrToday(null);
    Settlement s =
        new Settlement(
            claim,
            support.nextNumber("CS", claim.getBranchId(), today),
            terms,
            new ClaimApproval(support.user(), support.now(), BigDecimal.ZERO));
    claim
        .getTotals()
        .requireWithinOutstanding(EstimateSide.PAYMENT, s.getCostType(), s.getNetAmount());
    ShareSplit split = split(claim, s);
    s.getApproval().setAuthorityAmount(support.toBase(claim, split.payable(), today));
    Settlement saved = settlements.save(s);
    audit.record(
        ENTITY, saved.getSettlementNo(), AuditAction.CREATE, "Entered net " + s.getNetAmount());
    return saved;
  }

  /**
   * Checker approves a settlement: consumes the reserve, posts, records the payable and, for a
   * final settlement, releases the remaining reserve and closes the claim.
   *
   * @param id settlement
   * @param accountingDate accounting date, null for today
   * @return approved settlement
   */
  public Settlement approve(Long id, LocalDate accountingDate) {
    Settlement s = get(id);
    Claim claim = s.getClaim();
    claim.requireSettleable(SETTLE);
    LocalDate date = support.accountingDate(claim, accountingDate);
    s.approve(support.user(), support.now(), date);
    boolean isFinal = s.getSettlementType() == SettlementType.FINAL;
    if (isFinal) {
      pending.requireNone(claim, false);
    }
    ShareSplit split = split(claim, s);
    BigDecimal authority = support.toBase(claim, split.payable(), date);
    s.getApproval().setAuthorityAmount(authority);
    support.requireWithinLimit(authority, s.label());
    claim.applySettlement(s.getCostType(), s.getNetAmount());
    posting.postSettlement(s, split, date);
    if (isFinal) {
      String reason = "Final settlement " + s.getSettlementNo();
      reserves.releaseOutstanding(claim, date, "Reserve released on " + reason);
      claim.close(date, reason);
    }
    audit.record(
        ENTITY,
        s.getSettlementNo(),
        AuditAction.AUTHORIZE,
        "Approved " + s.getSettlementType() + " settlement, company share " + split.ours());
    return s;
  }

  /**
   * Checker rejects a settlement.
   *
   * @param id settlement
   * @param reason reason
   * @return rejected settlement
   */
  public Settlement reject(Long id, String reason) {
    Settlement s = get(id);
    s.reject(support.user(), support.now(), reason);
    audit.record(ENTITY, s.getSettlementNo(), AuditAction.REJECT, reason);
    return s;
  }

  private static ShareSplit split(Claim claim, Settlement s) {
    return claim
        .getPolicy()
        .split(claim.getTotals().paid(EstimateSide.PAYMENT, s.getCostType()), s.getNetAmount());
  }
}
