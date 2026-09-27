package com.iortatechnxt.brokerverse.submitted.review.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals.RecordFacts;
import com.iortatechnxt.brokerverse.approval.service.MasterRecordApprovals.Scope;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.submitted.domain.SbmApprovable;
import com.iortatechnxt.brokerverse.submitted.domain.SbmApprovalMatrix;
import com.iortatechnxt.brokerverse.submitted.domain.SbmDocStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaaf;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaafRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmInsurerRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLetterRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLimitRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRuleSet;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRuleSetRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRuleSetStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTor;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTorRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * My Approvals items of Submitted Policies (BRIDSP-07, 08, 18): the IAAF and TOR levels waiting for
 * the viewer (the permission or named approver of the current level, never the preparer), the rule
 * set versions submitted for approval ({@code SBM_RULE_APPROVE}, never the maker) and the limit,
 * insurer and letter rules and approval matrix levels pending authorization.
 */
@Component
@Transactional(readOnly = true)
public class SubmittedApprovalSource implements PendingApprovalSource {

  private static final String MODULE = "SUBMITTED";
  private static final String APPROVE = "SBM_RULE_APPROVE";
  private static final String SETUP_LINK = "/submitted/setup";

  private final SbmIaafRepository iaafs;
  private final SbmTorRepository tors;
  private final SbmPolicyRepository policies;
  private final SbmRuleSetRepository ruleSets;
  private final DocumentApprovals approvals;
  private final MasterRecordApprovals masters;

  /**
   * Creates the source.
   *
   * @param iaafs IAAFs
   * @param tors TORs
   * @param policies masterlist
   * @param ruleSets rule sets
   * @param approvals matrix approvals
   * @param masters maker-checker master records
   */
  public SubmittedApprovalSource(
      SbmIaafRepository iaafs,
      SbmTorRepository tors,
      SbmPolicyRepository policies,
      SbmRuleSetRepository ruleSets,
      DocumentApprovals approvals,
      MasterRecordApprovals masters) {
    this.iaafs = iaafs;
    this.tors = tors;
    this.policies = policies;
    this.ruleSets = ruleSets;
    this.approvals = approvals;
    this.masters = masters;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    List<PendingApproval> out = new ArrayList<>();
    for (SbmIaaf i : iaafs.findByStatus(SbmDocStatus.FOR_APPROVAL)) {
      document(viewer, DocumentApprovals.IAAF, i, i.getPolicyId(), "IAAF").ifPresent(out::add);
    }
    for (SbmTor t : tors.findByStatus(SbmDocStatus.FOR_APPROVAL)) {
      document(viewer, DocumentApprovals.TOR, t, t.getPolicyId(), "Terms of Reference")
          .ifPresent(out::add);
    }
    ruleSets(viewer, out);
    masters(viewer, out);
    return out;
  }

  private void ruleSets(ApprovalViewer viewer, List<PendingApproval> out) {
    if (viewer.can(APPROVE)) {
      for (SbmRuleSet s :
          ruleSets.findByStatusInOrderByIdAsc(List.of(SbmRuleSetStatus.SUBMITTED))) {
        if (viewer.mayApproveItemOf(s.getSubmittedBy())) {
          out.add(
              new PendingApproval(
                  MODULE,
                  "Rule set",
                  s.getCode() + " v" + s.getVersionNo(),
                  s.getDescription(),
                  null,
                  null,
                  s.getSubmittedBy(),
                  s.getSubmittedAt(),
                  s.getCompanyId(),
                  SETUP_LINK + "?ruleSet=" + s.getId()));
        }
      }
    }
  }

  private void masters(ApprovalViewer viewer, List<PendingApproval> out) {
    Scope scope = new Scope(MODULE, APPROVE);
    out.addAll(
        masters.pending(
            viewer,
            scope,
            SbmLimitRule.class,
            r -> facts("Limit rule", r.getInsurerCode(), r.getDescription(), r.getCompanyId())));
    out.addAll(
        masters.pending(
            viewer,
            scope,
            SbmInsurerRule.class,
            r ->
                facts(
                    "Insurer rule",
                    r.getSegment() + " " + r.getInsurerCode(),
                    r.getDescription(),
                    r.getCompanyId())));
    out.addAll(
        masters.pending(
            viewer,
            scope,
            SbmLetterRule.class,
            r -> facts("Letter rule", r.getLetterType(), r.getDescription(), r.getCompanyId())));
    out.addAll(
        masters.pending(
            viewer,
            scope,
            SbmApprovalMatrix.class,
            r ->
                facts(
                    "Approval level",
                    r.getDocument() + " level " + r.getLevel(),
                    r.getSignatoryTitle(),
                    r.getCompanyId())));
  }

  private Optional<PendingApproval> document(
      ApprovalViewer viewer, String doc, SbmApprovable d, Long policyId, String type) {
    SbmPolicy p = policyId == null ? null : policies.findById(policyId).orElse(null);
    if (p == null || !viewer.mayApproveItemOf(d.getCreatedBy())) {
      return Optional.empty();
    }
    List<SbmApprovalMatrix> levels;
    try {
      levels = approvals.levels(doc, d.getCompanyId(), p.getSegment(), p.getTerms().sumInsured());
    } catch (RuntimeException e) {
      return Optional.empty();
    }
    boolean may =
        viewer.systemView()
            || DocumentApprovals.mayApprove(d, levels, viewer.username(), viewer.authorities());
    if (!may) {
      return Optional.empty();
    }
    return Optional.of(
        new PendingApproval(
            MODULE,
            type,
            d.number(),
            p.getAssured().assuredName()
                + " - level "
                + d.getCurrentLevel()
                + " of "
                + d.getTotalLevels(),
            p.getTerms().sumInsured(),
            p.getTerms().currency(),
            d.getCreatedBy(),
            d.getSubmittedAt(),
            d.getCompanyId(),
            DocumentApprovals.linkOf(doc, d)));
  }

  private static RecordFacts facts(
      String type, String reference, String description, Long companyId) {
    return new RecordFacts(type, reference, description, companyId, SETUP_LINK);
  }
}
