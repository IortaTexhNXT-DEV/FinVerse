package com.iortatechnxt.finverse.underwriting.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.underwriting.api.dto.EndorsementRequest;
import com.iortatechnxt.finverse.underwriting.domain.Endorsement;
import com.iortatechnxt.finverse.underwriting.domain.EndorsementRepository;
import com.iortatechnxt.finverse.underwriting.domain.EndorsementType;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import com.iortatechnxt.finverse.underwriting.domain.PolicyStatus;
import com.iortatechnxt.finverse.underwriting.domain.PremiumBreakdown;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maker side of endorsements. Premium of each type:
 *
 * <ul>
 *   <li>ADDITIONAL / REFUND: the entered gross premium (positive / negative) on the policy terms;
 *   <li>RENEWAL: the renewal premium (default: original gross) plus the policy fee, new period;
 *   <li>CANCELLATION: pro-rata return of the current period's gross premium, 1/365 basis: gross ×
 *       unexpired days / period days (unexpired days counted from the effective date inclusive);
 *   <li>NIL: no premium.
 * </ul>
 *
 * Only one endorsement per policy may be open (draft or pending) at a time, so pro-rata figures are
 * always computed on the approved position.
 */
@Service
@Transactional
public class EndorsementService {

  static final String ENTITY = "Endorsement";

  private static final int RATIO_SCALE = 10;

  private final EndorsementRepository endorsements;
  private final PolicyService policies;
  private final PolicyPremiumCalculator calculator;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param endorsements repository
   * @param policies policies
   * @param calculator premium calculator
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public EndorsementService(
      EndorsementRepository endorsements,
      PolicyService policies,
      PolicyPremiumCalculator calculator,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.endorsements = endorsements;
    this.policies = policies;
    this.calculator = calculator;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Gets an endorsement with its policy.
   *
   * @param id id
   * @return endorsement
   */
  @Transactional(readOnly = true)
  public Endorsement get(Long id) {
    return endorsements
        .findWithPolicyById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Endorsement history of a policy.
   *
   * @param policyId policy
   * @return endorsements in number order
   */
  @Transactional(readOnly = true)
  public List<Endorsement> forPolicy(Long policyId) {
    return endorsements.findByPolicyIdOrderByEndorsementNo(policyId);
  }

  /**
   * Creates a draft endorsement on an approved policy.
   *
   * @param policyId policy
   * @param r request
   * @return draft
   */
  public Endorsement create(Long policyId, EndorsementRequest r) {
    Policy policy = policies.get(policyId);
    long open =
        endorsements.countByPolicyIdAndWorkflowStatusIn(
            policyId, EnumSet.of(PolicyStatus.DRAFT, PolicyStatus.PENDING_APPROVAL));
    if (open > 0) {
      throw new BusinessRuleException(
          "ENDORSEMENT_IN_PROGRESS", policy.label() + " already has an open endorsement");
    }
    Endorsement e =
        new Endorsement(
            policy,
            endorsements.maxEndorsementNo(policyId) + 1,
            r.type(),
            r.issueDate(),
            r.effectiveDate(),
            r.description());
    if (r.type() == EndorsementType.RENEWAL) {
      e.renewalPeriod(r.newPeriodFrom(), r.newPeriodTo());
    }
    e.applyPremium(premium(policy, r));
    Endorsement saved = endorsements.save(e);
    audit.record(ENTITY, saved.documentNo(), AuditAction.CREATE, r.type() + " endorsement created");
    return saved;
  }

  /**
   * Submits a draft endorsement.
   *
   * @param id id
   * @return endorsement
   */
  public Endorsement submit(Long id) {
    Endorsement e = get(id);
    e.submit(currentUser.username(), clock.instant());
    audit.record(ENTITY, e.documentNo(), AuditAction.SUBMIT, "Submitted for approval");
    return e;
  }

  /**
   * Discards a draft endorsement.
   *
   * @param id id
   * @return endorsement
   */
  public Endorsement discard(Long id) {
    Endorsement e = get(id);
    e.discard();
    audit.record(ENTITY, e.documentNo(), AuditAction.DEACTIVATE, "Discarded draft");
    return e;
  }

  private PremiumBreakdown premium(Policy policy, EndorsementRequest r) {
    BigDecimal gross = Money.nz(r.grossPremium());
    BigDecimal si = Money.nz(r.sumInsuredChange());
    return switch (r.type()) {
      case ADDITIONAL -> calculator.forEndorsement(policy, gross, si, BigDecimal.ZERO);
      case REFUND ->
          calculator.forEndorsement(policy, gross.negate(), si.negate(), BigDecimal.ZERO);
      case RENEWAL ->
          calculator.forEndorsement(
              policy,
              r.grossPremium() != null ? gross : policy.getPremium().getGrossPremium(),
              r.sumInsuredChange() != null ? si : policy.getPremium().getSumInsured(),
              policy.getProduct().getPolicyFee());
      case CANCELLATION -> cancellation(policy, r);
      case NIL -> new PremiumBreakdown();
    };
  }

  private PremiumBreakdown cancellation(Policy policy, EndorsementRequest r) {
    BigDecimal[] current = currentPeriodPosition(policy);
    long total = ChronoUnit.DAYS.between(policy.getPeriodFrom(), policy.getPeriodTo()) + 1;
    long unexpired = ChronoUnit.DAYS.between(r.effectiveDate(), policy.getPeriodTo()) + 1;
    BigDecimal ratio =
        BigDecimal.valueOf(unexpired)
            .divide(BigDecimal.valueOf(total), RATIO_SCALE, RoundingMode.HALF_EVEN);
    BigDecimal returnGross = Money.round(current[0].multiply(ratio)).negate();
    return calculator.forEndorsement(policy, returnGross, current[1].negate(), BigDecimal.ZERO);
  }

  /**
   * Gross premium and sum insured of the current period: the original issue (or the latest approved
   * renewal) plus later approved endorsements.
   */
  private BigDecimal[] currentPeriodPosition(Policy policy) {
    BigDecimal gross = policy.getPremium().getGrossPremium();
    BigDecimal si = policy.getPremium().getSumInsured();
    for (Endorsement e : forPolicy(policy.getId())) {
      if (e.getStatus() != PolicyStatus.APPROVED) {
        continue;
      }
      PremiumBreakdown p = e.getPremium();
      boolean renewal = e.getEndorsementType() == EndorsementType.RENEWAL;
      gross = renewal ? p.getGrossPremium() : gross.add(p.getGrossPremium());
      si = renewal ? p.getSumInsured() : si.add(p.getSumInsured());
    }
    return new BigDecimal[] {gross, si};
  }
}
