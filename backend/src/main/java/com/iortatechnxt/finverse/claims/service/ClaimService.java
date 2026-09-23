package com.iortatechnxt.finverse.claims.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.claims.api.dto.ClaimPartyRequest;
import com.iortatechnxt.finverse.claims.api.dto.ClaimRequest;
import com.iortatechnxt.finverse.claims.domain.Claim;
import com.iortatechnxt.finverse.claims.domain.ClaimPartyRole;
import com.iortatechnxt.finverse.claims.domain.ClaimPolicy;
import com.iortatechnxt.finverse.claims.domain.ClaimPolicyValues;
import com.iortatechnxt.finverse.claims.domain.ClaimRepository;
import com.iortatechnxt.finverse.claims.domain.ClaimSearchCriteria;
import com.iortatechnxt.finverse.claims.domain.ClaimSpecifications;
import com.iortatechnxt.finverse.claims.domain.CostType;
import com.iortatechnxt.finverse.claims.domain.EstimateLine;
import com.iortatechnxt.finverse.claims.domain.EstimateSide;
import com.iortatechnxt.finverse.claims.domain.LossDetails;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.party.service.PartyService;
import com.iortatechnxt.finverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.finverse.underwriting.service.PolicySnapshot;
import com.iortatechnxt.finverse.underwriting.service.RiskSnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Claim registration (first notification of loss) and inquiry.
 *
 * <p>Registration checks the policy through the underwriting read API ({@link PolicyQueryService},
 * snapshots only): it must be approved and in force at the date of loss, and the claim currency
 * must be the policy currency. The policy facts are copied onto the claim, the claim is numbered
 * {@code CL-<branch>-<year>-000001} (year of notification) and optional initial loss / expense
 * reserves are submitted for approval in the same transaction.
 */
@Service
@Transactional
public class ClaimService {

  private static final String INITIAL_RESERVE = "Initial reserve at notification";

  private final ClaimRepository claims;
  private final PolicyQueryService policies;
  private final PartyService parties;
  private final ReserveService reserves;
  private final ClaimAlerts alerts;
  private final ClaimSupport support;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param claims claim repository
   * @param policies underwriting read API
   * @param parties party master
   * @param reserves initial reserves
   * @param alerts exception rules
   * @param support claims helpers
   * @param audit audit trail
   */
  public ClaimService(
      ClaimRepository claims,
      PolicyQueryService policies,
      PartyService parties,
      ReserveService reserves,
      ClaimAlerts alerts,
      ClaimSupport support,
      AuditTrailService audit) {
    this.claims = claims;
    this.policies = policies;
    this.parties = parties;
    this.reserves = reserves;
    this.alerts = alerts;
    this.support = support;
    this.audit = audit;
  }

  /**
   * Searches claims.
   *
   * @param criteria filters
   * @param pageable paging
   * @return page of claims (claimant loaded)
   */
  @Transactional(readOnly = true)
  public Page<Claim> search(ClaimSearchCriteria criteria, Pageable pageable) {
    return claims.findAll(ClaimSpecifications.claims(criteria), pageable);
  }

  /**
   * Gets a claim with its parties.
   *
   * @param id id
   * @return claim
   */
  @Transactional(readOnly = true)
  public Claim get(Long id) {
    return support.claim(id);
  }

  /**
   * Looks up a policy for a claim notification and tells whether it covers a loss date.
   *
   * @param companyId company
   * @param policyNo policy number
   * @param lossDate date of loss, null to skip the cover check
   * @return policy, risks and cover
   */
  @Transactional(readOnly = true)
  public PolicyCover policyCover(Long companyId, String policyNo, LocalDate lossDate) {
    PolicySnapshot policy = requirePolicy(companyId, policyNo);
    return new PolicyCover(
        policy, policies.risks(policy.id()), lossDate == null || policy.isInForce(lossDate));
  }

  /**
   * Registers a claim.
   *
   * @param r notification
   * @return registered claim
   */
  public Claim register(ClaimRequest r) {
    PolicySnapshot policy = requirePolicy(r.companyId(), r.policyNo());
    requireNotification(r, policy);
    List<RiskSnapshot> risks = policies.risks(policy.id());
    RiskSnapshot risk = risk(risks, r.riskLineNo());
    String claimantCode = r.claimantCode() == null ? policy.customerCode() : r.claimantCode();
    Party claimant =
        parties.requireActive(r.companyId(), claimantCode, ClaimPartyRole.CLAIMANT.partyTypes());
    Claim claim =
        new Claim(
            support.nextNumber("CL", policy.branchId(), r.reportedDate()),
            policy.branchId(),
            new ClaimPolicy(policyValues(policy, risk, risks)),
            new LossDetails(
                r.lossDate(),
                r.reportedDate(),
                r.natureOfLoss(),
                r.causeOfLoss(),
                r.lossLocation(),
                r.description()),
            r.currency(),
            claimant,
            r.companyId());
    claim.addParty(claimant, ClaimPartyRole.CLAIMANT);
    for (ClaimPartyRequest p : r.partiesOrEmpty()) {
      claim.addParty(
          parties.requireActive(r.companyId(), p.partyCode(), p.role().partyTypes()), p.role());
    }
    Claim saved = claims.save(claim);
    audit.record(
        ClaimSupport.CLAIM_ENTITY,
        saved.getClaimNo(),
        AuditAction.CREATE,
        "Registered under policy " + policy.policyNo() + ", loss " + r.lossDate());
    alerts.checkNotification(saved);
    requestInitial(saved, CostType.LOSS, r.initialLossReserve());
    requestInitial(saved, CostType.EXPENSE, r.initialExpenseReserve());
    return saved;
  }

  /**
   * Adds an involved party to an active claim.
   *
   * @param claimId claim
   * @param request role and party
   * @return claim
   */
  public Claim addParty(Long claimId, ClaimPartyRequest request) {
    Claim claim = support.claim(claimId);
    claim.requireActive("add a party to");
    claim.addParty(
        parties.requireActive(
            claim.getCompanyId(), request.partyCode(), request.role().partyTypes()),
        request.role());
    audit.record(
        ClaimSupport.CLAIM_ENTITY,
        claim.getClaimNo(),
        AuditAction.UPDATE,
        "Added " + request.role() + " " + request.partyCode());
    return claim;
  }

  private void requestInitial(Claim claim, CostType cost, BigDecimal amount) {
    if (Money.isPositive(amount)) {
      reserves.request(
          claim.getId(), new EstimateLine(EstimateSide.PAYMENT, cost, amount), INITIAL_RESERVE);
    }
  }

  private PolicySnapshot requirePolicy(Long companyId, String policyNo) {
    return policies
        .findByNumber(companyId, policyNo)
        .orElseThrow(
            () -> new BusinessRuleException("POLICY_NOT_FOUND", "Unknown policy " + policyNo));
  }

  private void requireNotification(ClaimRequest r, PolicySnapshot policy) {
    if (!policy.isInForce(r.lossDate())) {
      throw new BusinessRuleException(
          "POLICY_NOT_IN_FORCE",
          "Policy " + policy.policyNo() + " is not approved and in force on " + r.lossDate());
    }
    if (r.reportedDate().isAfter(support.dateOrToday(null))) {
      throw new BusinessRuleException(
          "REPORTED_DATE_IN_FUTURE", "The reported date cannot be in the future");
    }
    if (!policy.currency().equals(r.currency())) {
      throw new BusinessRuleException(
          "CLAIM_CURRENCY_MISMATCH",
          "The claim currency must be the policy currency " + policy.currency());
    }
  }

  private static RiskSnapshot risk(List<RiskSnapshot> risks, Integer lineNo) {
    if (lineNo == null) {
      return null;
    }
    return risks.stream()
        .filter(r -> r.lineNo() == lineNo)
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessRuleException("UNKNOWN_RISK", "The policy has no risk line " + lineNo));
  }

  private static ClaimPolicyValues policyValues(
      PolicySnapshot p, RiskSnapshot risk, List<RiskSnapshot> risks) {
    BigDecimal sumInsured =
        risk != null
            ? risk.sumInsured()
            : risks.stream().map(RiskSnapshot::sumInsured).reduce(Money.zero(), BigDecimal::add);
    return new ClaimPolicyValues(
        p.id(),
        p.policyNo(),
        p.productCode(),
        p.productName(),
        p.businessLine(),
        p.customerCode(),
        p.customerName(),
        p.insuredName(),
        p.intermediaryCode(),
        p.uwYear(),
        p.sharePct(),
        p.coinsuranceLeader(),
        p.coinsurerCode(),
        risk == null ? null : risk.id(),
        risk == null ? null : risk.description(),
        sumInsured);
  }
}
