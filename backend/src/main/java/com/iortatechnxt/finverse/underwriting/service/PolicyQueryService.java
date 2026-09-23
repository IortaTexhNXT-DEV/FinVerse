package com.iortatechnxt.finverse.underwriting.service;

import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.underwriting.domain.EndorsementRepository;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import com.iortatechnxt.finverse.underwriting.domain.PolicyRepository;
import com.iortatechnxt.finverse.underwriting.domain.PolicyRisk;
import com.iortatechnxt.finverse.underwriting.domain.PolicyStatus;
import com.iortatechnxt.finverse.underwriting.domain.UnderwritingSpecifications;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PUBLIC READ API of underwriting for other modules (claims, reinsurance, receivables, finance
 * reports). Returns immutable snapshots, never entities, so callers are insulated from the
 * underwriting persistence model.
 *
 * <ul>
 *   <li>{@link #findByNumber} / {@link #get}: policy header (current period, status, parties);
 *   <li>{@link #risks}: insured risks with sums insured (RI allocation, claim registration);
 *   <li>{@link #isInForce}: cover check for a loss date;
 *   <li>{@link #approvedTransactions}: approved policy issues and endorsements in a period with
 *       their premium figures (RI cessions, production statistics);
 *   <li>{@link #policyTransactions}: premium history of one policy;
 *   <li>{@link #transactions}: general selection used by the underwriting registers;
 *   <li>{@link #policiesExpiring} / {@link #risksInForce}: renewal and accumulation views.
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class PolicyQueryService {

  private static final Comparator<PremiumTransaction> ORDER =
      Comparator.comparing((PremiumTransaction t) -> t.policy().policyNo())
          .thenComparingInt(PremiumTransaction::endorsementNo);

  private final PolicyRepository policies;
  private final EndorsementRepository endorsements;

  /**
   * Creates the service.
   *
   * @param policies policy repository
   * @param endorsements endorsement repository
   */
  public PolicyQueryService(PolicyRepository policies, EndorsementRepository endorsements) {
    this.policies = policies;
    this.endorsements = endorsements;
  }

  /**
   * Finds a policy (or marine certificate) by number.
   *
   * @param companyId company
   * @param policyNo policy number
   * @return snapshot, empty when unknown
   */
  public Optional<PolicySnapshot> findByNumber(Long companyId, String policyNo) {
    return policies.findByCompanyIdAndPolicyNo(companyId, policyNo).map(PolicySnapshot::of);
  }

  /**
   * Gets a policy by id.
   *
   * @param policyId policy id
   * @return snapshot
   * @throws ResourceNotFoundException when unknown
   */
  public PolicySnapshot get(Long policyId) {
    return PolicySnapshot.of(load(policyId));
  }

  /**
   * Insured risks of a policy.
   *
   * @param policyId policy id
   * @return risks in line order
   */
  public List<RiskSnapshot> risks(Long policyId) {
    return load(policyId).getRisks().stream().map(RiskSnapshot::of).toList();
  }

  /**
   * Whether a policy covers a date (approved, within its current period, not cancelled before the
   * date). Claims registration uses it to validate the loss date.
   *
   * @param policyId policy id
   * @param date loss date
   * @return true when in force
   */
  public boolean isInForce(Long policyId, LocalDate date) {
    return load(policyId).isInForce(date);
  }

  /**
   * Approved premium transactions (original issues and endorsements, including those of policies
   * cancelled later) whose approval (accounting) date falls in a period.
   *
   * @param companyId company
   * @param from approval date from (inclusive)
   * @param to approval date to (inclusive)
   * @return transactions ordered by policy number then endorsement number
   */
  public List<PremiumTransaction> approvedTransactions(
      Long companyId, LocalDate from, LocalDate to) {
    return transactions(TransactionQuery.approved(companyId, from, to));
  }

  /**
   * Approved premium history of one policy (original issue and approved endorsements).
   *
   * @param policyId policy id
   * @return transactions in endorsement order
   */
  public List<PremiumTransaction> policyTransactions(Long policyId) {
    Policy policy = load(policyId);
    List<PremiumTransaction> out = new ArrayList<>();
    if (policy.getWorkflow().getApprovalDate() != null) {
      out.add(PremiumTransaction.of(policy));
    }
    endorsements.findByPolicyIdOrderByEndorsementNo(policyId).stream()
        .filter(e -> e.getStatus() == PolicyStatus.APPROVED)
        .map(PremiumTransaction::of)
        .forEach(out::add);
    return out;
  }

  /**
   * Premium transactions matching a selection. Discarded drafts are never returned.
   *
   * @param q selection
   * @return transactions ordered by policy number then endorsement number
   */
  public List<PremiumTransaction> transactions(TransactionQuery q) {
    Stream<PremiumTransaction> issues =
        policies
            .findAll(
                UnderwritingSpecifications.policiesBy(
                    q.companyId(), q.basis(), q.from(), q.to(), q.statuses()))
            .stream()
            .map(PremiumTransaction::of);
    Stream<PremiumTransaction> changes =
        endorsements
            .findAll(
                UnderwritingSpecifications.endorsementsBy(
                    q.companyId(), q.basis(), q.from(), q.to(), q.statuses()))
            .stream()
            .map(PremiumTransaction::of);
    return Stream.concat(issues, changes)
        .filter(t -> t.status() != PolicyStatus.CANCELLED || t.approvalDate() != null)
        .sorted(ORDER)
        .toList();
  }

  /**
   * Approved (not cancelled) policies expiring in a period, e.g. for renewal lists.
   *
   * @param companyId company
   * @param expiryFrom current period end from, null for open
   * @param expiryTo current period end to, null for open
   * @return snapshots ordered by policy number
   */
  public List<PolicySnapshot> policiesExpiring(
      Long companyId, LocalDate expiryFrom, LocalDate expiryTo) {
    return policies
        .findAll(
            UnderwritingSpecifications.expiring(
                companyId, expiryFrom, expiryTo, EnumSet.of(PolicyStatus.APPROVED)))
        .stream()
        .map(PolicySnapshot::of)
        .sorted(Comparator.comparing(PolicySnapshot::policyNo))
        .toList();
  }

  /**
   * Risks in force on a date (approved policies, or cancelled after the date), with the number of
   * approved endorsements of their policy; used for risk accumulation by zone.
   *
   * @param companyId company
   * @param asOf date
   * @return exposures ordered by accumulation zone and policy
   */
  public List<RiskExposure> risksInForce(Long companyId, LocalDate asOf) {
    List<PolicyRisk> risks =
        policies.findRisksCovering(
            companyId, asOf, EnumSet.of(PolicyStatus.APPROVED, PolicyStatus.CANCELLED));
    List<Long> ids = risks.stream().map(r -> r.getPolicy().getId()).distinct().toList();
    Map<Long, Long> endorsed =
        ids.isEmpty()
            ? Map.of()
            : endorsements.findByPolicyIdIn(ids).stream()
                .filter(e -> e.getStatus() == PolicyStatus.APPROVED)
                .collect(Collectors.groupingBy(e -> e.getPolicy().getId(), Collectors.counting()));
    return risks.stream()
        .filter(r -> r.getPolicy().isInForce(asOf))
        .map(
            r ->
                new RiskExposure(
                    RiskSnapshot.of(r),
                    PolicySnapshot.of(r.getPolicy()),
                    endorsed.getOrDefault(r.getPolicy().getId(), 0L).intValue()))
        .toList();
  }

  private Policy load(Long policyId) {
    return policies
        .findWithDetailsById(policyId)
        .orElseThrow(() -> new ResourceNotFoundException(PolicyService.ENTITY, policyId));
  }
}
