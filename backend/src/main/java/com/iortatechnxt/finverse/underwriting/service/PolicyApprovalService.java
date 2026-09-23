package com.iortatechnxt.finverse.underwriting.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.underwriting.domain.Endorsement;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import com.iortatechnxt.finverse.underwriting.service.UnderwritingAuthority.Premium;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checker side of policies and endorsements. Approval and its accounting (premium, commission and
 * coinsurance events, debit / credit notes and open items) happen in ONE transaction: either the
 * document is approved and fully accounted for, or nothing changes. The checker must differ from
 * the maker and the submitter, and the gross premium must be within the checker's authorization
 * limit ({@link UnderwritingAuthority}).
 */
@Service
@Transactional
public class PolicyApprovalService {

  private final PolicyService policies;
  private final EndorsementService endorsements;
  private final PremiumPostingService posting;
  private final UnderwritingAuthority authority;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param policies policy service
   * @param endorsements endorsement service
   * @param posting premium accounting
   * @param authority authorization limit
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public PolicyApprovalService(
      PolicyService policies,
      EndorsementService endorsements,
      PremiumPostingService posting,
      UnderwritingAuthority authority,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.policies = policies;
    this.endorsements = endorsements;
    this.posting = posting;
    this.authority = authority;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Approves a policy and accounts for its premium.
   *
   * @param id policy
   * @param accountingDate accounting date, null for today
   * @return approved policy
   */
  public Policy approvePolicy(Long id, LocalDate accountingDate) {
    Policy policy = policies.get(id);
    String checker = currentUser.username();
    LocalDate date = dateOrToday(accountingDate);
    policy.approve(checker, clock.instant(), date);
    authority.requireWithinLimit(
        checker,
        policy.label(),
        new Premium(
            policy.getCompanyId(),
            policy.getCurrency(),
            policy.getPremium().getGrossPremium(),
            date));
    policy.recordPosting(posting.post(PremiumPosting.of(policy)));
    audit.record(
        PolicyService.ENTITY,
        policy.getPolicyNo(),
        AuditAction.AUTHORIZE,
        "Approved; debit note " + policy.getRefs().getDebitNoteNo());
    return policy;
  }

  /**
   * Returns a policy to its maker.
   *
   * @param id policy
   * @param reason reason
   * @return policy
   */
  public Policy rejectPolicy(Long id, String reason) {
    Policy policy = policies.get(id);
    policy.reject(reason);
    audit.record(PolicyService.ENTITY, policy.getPolicyNo(), AuditAction.REJECT, reason);
    return policy;
  }

  /**
   * Approves an endorsement, applies it to the policy and accounts for it (non-financial
   * endorsements raise no event).
   *
   * @param id endorsement
   * @param accountingDate accounting date, null for today
   * @return approved endorsement
   */
  public Endorsement approveEndorsement(Long id, LocalDate accountingDate) {
    Endorsement e = endorsements.get(id);
    String checker = currentUser.username();
    LocalDate date = dateOrToday(accountingDate);
    e.approve(checker, clock.instant(), date);
    authority.requireWithinLimit(
        checker,
        e.label(),
        new Premium(
            e.getPolicy().getCompanyId(),
            e.getPolicy().getCurrency(),
            e.getPremium().getGrossPremium(),
            date));
    e.getEndorsementType()
        .eventType()
        .filter(type -> e.getPremium().isFinancial())
        .ifPresent(type -> e.recordPosting(posting.post(PremiumPosting.of(e, type))));
    audit.record(
        EndorsementService.ENTITY, e.documentNo(), AuditAction.AUTHORIZE, "Approved endorsement");
    return e;
  }

  /**
   * Returns an endorsement to its maker.
   *
   * @param id endorsement
   * @param reason reason
   * @return endorsement
   */
  public Endorsement rejectEndorsement(Long id, String reason) {
    Endorsement e = endorsements.get(id);
    e.reject(reason);
    audit.record(EndorsementService.ENTITY, e.documentNo(), AuditAction.REJECT, reason);
    return e;
  }

  private LocalDate dateOrToday(LocalDate date) {
    return date != null ? date : LocalDate.now(clock);
  }
}
