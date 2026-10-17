package com.iortatechnxt.brokerverse.submitted.proposal.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmProposal;
import com.iortatechnxt.brokerverse.submitted.domain.SbmProposalBatch;
import com.iortatechnxt.brokerverse.submitted.domain.SbmProposalBatchRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmProposalRepository;
import com.iortatechnxt.brokerverse.submitted.service.SbmScopeService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renewal proposals with nominated package rates (FR-SP-066) and the preferred insurer (FR-SP-067):
 * the proposed insurer and rate of each selected record, the edited rate applied with a reason, the
 * batch SBP-yyyy-nnnnnn of one proposal per record and version For Review, the release or return by
 * the Team Lead (not the maker), and the assignment of another insurer that re-applies its
 * nominated rate.
 */
@Service
@Transactional
public class SbmProposalService {

  /** Audit entity. */
  public static final String ENTITY = "SbmProposal";

  private static final Set<SbmPolicyStatus> PROPOSABLE =
      EnumSet.of(SbmPolicyStatus.FOR_RENEWAL, SbmPolicyStatus.RENEWAL_IN_PROGRESS);

  private final SbmPolicyRepository policies;
  private final SbmProposalRepository proposals;
  private final SbmProposalBatchRepository batches;
  private final ProposalPricing pricing;
  private final InsurerService insurers;
  private final SbmScopeService scope;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param policies masterlist
   * @param proposals proposals
   * @param batches proposal batches
   * @param pricing proposed insurer, rate and premium
   * @param insurers insurer master
   * @param scope data scope
   * @param numbers document numbers
   * @param audit audit trail
   * @param currentUser signed-in user
   * @param clock clock
   */
  public SbmProposalService(
      SbmPolicyRepository policies,
      SbmProposalRepository proposals,
      SbmProposalBatchRepository batches,
      ProposalPricing pricing,
      InsurerService insurers,
      SbmScopeService scope,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.policies = policies;
    this.proposals = proposals;
    this.batches = batches;
    this.pricing = pricing;
    this.insurers = insurers;
    this.scope = scope;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The proposed insurer, nominated rate and premium of each selected record.
   *
   * @param companyId company
   * @param policyIds records
   * @return one line per record
   */
  @Transactional(readOnly = true)
  public List<Proposed> preview(Long companyId, List<Long> policyIds) {
    return selected(companyId, policyIds).stream().map(p -> pricing.proposed(p, null)).toList();
  }

  /**
   * Generates the proposals of the selected records in a new batch, For Review.
   *
   * @param companyId company
   * @param lines records with the insurer and the rate to apply
   * @return the batch
   */
  public SbmProposalBatch generate(Long companyId, List<Line> lines) {
    List<SbmPolicy> records = selected(companyId, lines.stream().map(Line::policyId).toList());
    LocalDate today = BusinessClock.today(clock);
    SbmProposalBatch batch =
        batches.save(new SbmProposalBatch(companyId, numbers.next("SBP-" + today.getYear())));
    for (int i = 0; i < records.size(); i++) {
      SbmPolicy p = records.get(i);
      Line line = lines.get(i);
      SbmProposal.Terms terms = pricing.terms(p, line);
      List<SbmProposal> earlier = proposals.findByPolicyIdOrderByVersionNoDesc(p.getId());
      earlier.stream()
          .filter(e -> e.getStatus() != SbmProposal.Status.SUPERSEDED)
          .forEach(SbmProposal::supersede);
      int version = earlier.isEmpty() ? 1 : earlier.get(0).getVersionNo() + 1;
      SbmProposal saved = proposals.save(new SbmProposal(batch, p, version, terms));
      audit.record(ENTITY, number(batch, p, saved), AuditAction.CREATE, describe(terms));
    }
    return batch;
  }

  /**
   * Assigns a preferred insurer to proposals For Review and re-applies its nominated rate.
   *
   * @param ids proposals
   * @param insurerCode insurer chosen
   * @return the proposals
   */
  public List<SbmProposal> assignInsurer(List<Long> ids, String insurerCode) {
    if (insurerCode == null || insurerCode.isBlank()) {
      throw new BusinessRuleException("SBM_PROPOSAL_INSURER", "Select the insurer");
    }
    return ids.stream().map(id -> assignOne(proposal(id), insurerCode.strip())).toList();
  }

  private SbmProposal assignOne(SbmProposal proposal, String insurerCode) {
    SbmPolicy p = policy(proposal.getCompanyId(), proposal.getPolicyId());
    if (proposal.getStatus() != SbmProposal.Status.FOR_REVIEW) {
      throw new BusinessRuleException(
          "SBM_PROPOSAL_RELEASED",
          "Proposal of "
              + p.getSbmNo()
              + " is "
              + (proposal.getStatus() == SbmProposal.Status.RELEASED ? "released" : "closed")
              + ". Ask the Team Lead to return it first");
    }
    insurers.requireUsableInsurer(p.getCompanyId(), insurerCode);
    BigDecimal nominated = pricing.nominated(p, insurerCode);
    if (nominated == null) {
      throw new BusinessRuleException(
          "SBM_PROPOSAL_NO_RATE",
          "Policy " + p.getSbmNo() + " has no nominated rate with " + insurerCode);
    }
    proposal.assign(
        new SbmProposal.Terms(
            proposal.getDefaultInsurer(),
            insurerCode,
            nominated,
            nominated,
            null,
            ProposalPricing.premium(p, nominated)));
    audit.record(
        ENTITY,
        p.getSbmNo() + " v" + proposal.getVersionNo(),
        AuditAction.UPDATE,
        "Insurer "
            + insurerCode
            + " chosen in place of "
            + Objects.requireNonNullElse(proposal.getDefaultInsurer(), "none"));
    return proposal;
  }

  /**
   * Releases a proposal For Review (someone other than its maker).
   *
   * @param id proposal
   * @return proposal
   */
  public SbmProposal release(Long id) {
    SbmProposal proposal = proposal(id);
    proposal.release(currentUser.username(), clock.instant());
    audit.record(ENTITY, id, AuditAction.AUTHORIZE, "Released");
    return proposal;
  }

  /**
   * Returns a proposal to its maker.
   *
   * @param id proposal
   * @param reason reason
   * @return proposal
   */
  public SbmProposal returnProposal(Long id, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException("SBM_PROPOSAL_REASON", "Enter the reason of the return");
    }
    SbmProposal proposal = proposal(id);
    proposal.returned(reason.strip());
    audit.record(ENTITY, id, AuditAction.REJECT, "Returned: " + reason.strip());
    return proposal;
  }

  /**
   * The batches of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return batches
   */
  @Transactional(readOnly = true)
  public Page<SbmProposalBatch> batches(Long companyId, Pageable pageable) {
    return batches.findByCompanyIdOrderByIdDesc(companyId, pageable);
  }

  /**
   * The proposals of a batch.
   *
   * @param batchId batch
   * @return proposals
   */
  @Transactional(readOnly = true)
  public List<SbmProposal> ofBatch(Long batchId) {
    return proposals.findByBatchIdOrderByIdAsc(batchId);
  }

  /**
   * A proposal.
   *
   * @param id proposal
   * @return proposal
   */
  @Transactional(readOnly = true)
  public SbmProposal proposal(Long id) {
    return proposals.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * A record of a proposal, within the user's scope.
   *
   * @param companyId company
   * @param policyId record
   * @return record
   */
  @Transactional(readOnly = true)
  public SbmPolicy policy(Long companyId, Long policyId) {
    SbmPolicy p =
        policies
            .findById(policyId)
            .filter(x -> x.getCompanyId().equals(companyId))
            .orElseThrow(() -> new ResourceNotFoundException("Submitted policy", policyId));
    return scope.requireVisible(p);
  }

  private List<SbmPolicy> selected(Long companyId, List<Long> policyIds) {
    if (policyIds == null || policyIds.isEmpty()) {
      throw new BusinessRuleException("SBM_PROPOSAL_SELECTION", "Select at least one policy");
    }
    return policyIds.stream().map(id -> requireProposable(policy(companyId, id))).toList();
  }

  private static SbmPolicy requireProposable(SbmPolicy p) {
    if (!PROPOSABLE.contains(p.getStatus())) {
      throw new BusinessRuleException(
          "SBM_PROPOSAL_STATUS", "Policy " + p.getSbmNo() + " is not for renewal");
    }
    return p;
  }

  private static String number(SbmProposalBatch batch, SbmPolicy p, SbmProposal proposal) {
    return batch.getBatchNo() + " " + p.getSbmNo() + " v" + proposal.getVersionNo();
  }

  private static String describe(SbmProposal.Terms t) {
    return "Proposal with "
        + t.insurerCode()
        + " at "
        + t.appliedRate().stripTrailingZeros().toPlainString()
        + "%"
        + (t.reason() == null
            ? ""
            : " (nominated "
                + (t.nominatedRate() == null
                    ? "none"
                    : t.nominatedRate().stripTrailingZeros().toPlainString() + "%")
                + "; reason: "
                + t.reason()
                + ")");
  }

  /**
   * What is proposed for a record.
   *
   * @param policyId record
   * @param sbmNo masterlist number
   * @param assuredName assured
   * @param segment segment
   * @param vehicleType vehicle classification
   * @param sumInsured sum insured
   * @param defaultInsurer insurer the rules give (else the expiring insurer)
   * @param insurerCode insurer proposed
   * @param nominatedRate nominated rate, null when none
   * @param premium premium at the nominated rate
   * @param problem why the record cannot be generated as it is, null when it can
   */
  public record Proposed(
      Long policyId,
      String sbmNo,
      String assuredName,
      String segment,
      String vehicleType,
      BigDecimal sumInsured,
      String defaultInsurer,
      String insurerCode,
      BigDecimal nominatedRate,
      BigDecimal premium,
      String problem) {}

  /**
   * A record to generate.
   *
   * @param policyId record
   * @param insurerCode insurer chosen, blank for the proposed one
   * @param rate rate to apply, null for the nominated rate
   * @param reason reason of a rate other than the nominated one
   */
  public record Line(Long policyId, String insurerCode, BigDecimal rate, String reason) {}
}
