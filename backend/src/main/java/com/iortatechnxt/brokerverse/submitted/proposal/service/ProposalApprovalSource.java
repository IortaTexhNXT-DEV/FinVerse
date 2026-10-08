package com.iortatechnxt.brokerverse.submitted.proposal.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmNominatedRateRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmProposal;
import com.iortatechnxt.brokerverse.submitted.domain.SbmProposalRepository;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * My Approvals items of the renewal proposals (FR-SP-066): the proposals For Review and the
 * nominated rates pending authorization, for the holders of {@code SBM_PROPOSAL} other than their
 * maker (the Team Lead releases them).
 */
@Component
@Transactional(readOnly = true)
public class ProposalApprovalSource implements PendingApprovalSource {

  private static final String MODULE = "SUBMITTED_POLICIES";
  private static final String LINK = "/submitted/proposals";

  private final SbmProposalRepository proposals;
  private final SbmNominatedRateRepository rates;

  /**
   * Creates the source.
   *
   * @param proposals proposals
   * @param rates nominated rates
   */
  public ProposalApprovalSource(SbmProposalRepository proposals, SbmNominatedRateRepository rates) {
    this.proposals = proposals;
    this.rates = rates;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can("SBM_PROPOSAL")) {
      return List.of();
    }
    Stream<PendingApproval> forReview =
        proposals.findByStatus(SbmProposal.Status.FOR_REVIEW).stream()
            .filter(p -> viewer.mayApproveItemOf(p.getCreatedBy()))
            .map(
                p ->
                    new PendingApproval(
                        MODULE,
                        "Renewal proposal",
                        "Proposal v" + p.getVersionNo(),
                        "Insurer " + p.getInsurerCode() + " at " + p.getAppliedRate() + "%",
                        null,
                        null,
                        p.getCreatedBy(),
                        p.getCreatedAt(),
                        p.getCompanyId(),
                        LINK));
    Stream<PendingApproval> pendingRates =
        rates.findByRecordStatus(RecordStatus.PENDING_AUTHORIZATION).stream()
            .filter(r -> viewer.mayApproveItemOf(r.getMaker()))
            .map(
                r ->
                    new PendingApproval(
                        MODULE,
                        "Nominated rate",
                        r.getSegment() + " " + r.getInsurerCode(),
                        "Rate " + r.getRate() + "%",
                        null,
                        null,
                        r.getMaker(),
                        r.getUpdatedAt() == null ? r.getCreatedAt() : r.getUpdatedAt(),
                        r.getCompanyId(),
                        LINK + "?tab=rates"));
    return Stream.concat(forReview, pendingRates).toList();
  }
}
