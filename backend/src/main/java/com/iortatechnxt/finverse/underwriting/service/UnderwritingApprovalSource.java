package com.iortatechnxt.finverse.underwriting.service;

import com.iortatechnxt.finverse.approval.service.ApprovalViewer;
import com.iortatechnxt.finverse.approval.service.MasterRecordApprovals;
import com.iortatechnxt.finverse.approval.service.MasterRecordApprovals.RecordFacts;
import com.iortatechnxt.finverse.approval.service.MasterRecordApprovals.Scope;
import com.iortatechnxt.finverse.approval.service.PendingApproval;
import com.iortatechnxt.finverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.finverse.underwriting.domain.ApprovalWorkflow;
import com.iortatechnxt.finverse.underwriting.domain.Endorsement;
import com.iortatechnxt.finverse.underwriting.domain.EndorsementRepository;
import com.iortatechnxt.finverse.underwriting.domain.OpenCover;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import com.iortatechnxt.finverse.underwriting.domain.PolicyRepository;
import com.iortatechnxt.finverse.underwriting.domain.PolicyStatus;
import com.iortatechnxt.finverse.underwriting.domain.Product;
import com.iortatechnxt.finverse.underwriting.domain.QuotationRepository;
import com.iortatechnxt.finverse.underwriting.domain.QuotationStatus;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Approval inbox source of underwriting: policies, endorsements and quotations pending approval,
 * and products and open covers pending authorization. All are decided under {@code
 * POLICY_AUTHORIZE}; the creator and the submitter of a document may not decide it, so neither sees
 * it. Underwriting applies no authorization limit.
 */
@Component
public class UnderwritingApprovalSource implements PendingApprovalSource {

  /** Module code of underwriting items. */
  public static final String MODULE = "UNDERWRITING";

  private static final String PERMISSION = "POLICY_AUTHORIZE";
  private static final String POLICY_ROUTE = "/underwriting/policies/";
  private static final String WORKFLOW = "workflow";
  private static final String STATUS = "status";

  private final PolicyRepository policies;
  private final EndorsementRepository endorsements;
  private final QuotationRepository quotations;
  private final MasterRecordApprovals records;

  /**
   * Creates the source.
   *
   * @param policies policy repository
   * @param endorsements endorsement repository
   * @param quotations quotation repository
   * @param records master record helper (products, open covers)
   */
  public UnderwritingApprovalSource(
      PolicyRepository policies,
      EndorsementRepository endorsements,
      QuotationRepository quotations,
      MasterRecordApprovals records) {
    this.policies = policies;
    this.endorsements = endorsements;
    this.quotations = quotations;
    this.records = records;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can(PERMISSION)) {
      return List.of();
    }
    List<PendingApproval> items = new ArrayList<>();
    items.addAll(policies(viewer));
    items.addAll(endorsements(viewer));
    items.addAll(quotations(viewer));
    Scope scope = new Scope(MODULE, PERMISSION);
    items.addAll(records.pending(viewer, scope, Product.class, UnderwritingApprovalSource::facts));
    items.addAll(
        records.pending(viewer, scope, OpenCover.class, UnderwritingApprovalSource::facts));
    return items;
  }

  private List<PendingApproval> policies(ApprovalViewer viewer) {
    Specification<Policy> pending =
        (root, query, cb) ->
            cb.equal(root.get(WORKFLOW).get(STATUS), PolicyStatus.PENDING_APPROVAL);
    return policies.findAll(pending).stream()
        .filter(p -> decidable(viewer, p.getCreatedBy(), p.getWorkflow()))
        .map(
            p ->
                new PendingApproval(
                    MODULE,
                    "Policy",
                    p.getPolicyNo(),
                    p.getInsuredName(),
                    p.getPremium().getGrossPremium(),
                    p.getCurrency(),
                    p.getWorkflow().getSubmittedBy(),
                    p.getWorkflow().getSubmittedAt(),
                    p.getCompanyId(),
                    POLICY_ROUTE + p.getId()))
        .toList();
  }

  private List<PendingApproval> endorsements(ApprovalViewer viewer) {
    Specification<Endorsement> pending =
        (root, query, cb) ->
            cb.equal(root.get(WORKFLOW).get(STATUS), PolicyStatus.PENDING_APPROVAL);
    return endorsements.findAll(pending).stream()
        .filter(e -> decidable(viewer, e.getCreatedBy(), e.getWorkflow()))
        .map(
            e ->
                new PendingApproval(
                    MODULE,
                    "Endorsement (" + e.getEndorsementType() + ")",
                    e.documentNo(),
                    e.getPolicy().getInsuredName(),
                    e.getPremium().getGrossPremium(),
                    e.getPolicy().getCurrency(),
                    e.getWorkflow().getSubmittedBy(),
                    e.getWorkflow().getSubmittedAt(),
                    e.getPolicy().getCompanyId(),
                    POLICY_ROUTE + e.getPolicy().getId()))
        .toList();
  }

  private List<PendingApproval> quotations(ApprovalViewer viewer) {
    return quotations.findByStatusOrderById(QuotationStatus.PENDING_APPROVAL).stream()
        .filter(
            q ->
                viewer.mayApproveItemOf(q.getCreatedBy())
                    && viewer.mayApproveItemOf(q.getSubmittedBy()))
        .map(
            q ->
                new PendingApproval(
                    MODULE,
                    "Quotation",
                    q.getQuotationNo(),
                    q.getInsuredName(),
                    q.getIterations().isEmpty() ? null : q.current().getGrossPremium(),
                    q.getCurrency(),
                    q.getSubmittedBy(),
                    q.getUpdatedAt(),
                    q.getCompanyId(),
                    "/underwriting/quotations/" + q.getId()))
        .toList();
  }

  private static boolean decidable(ApprovalViewer viewer, String creator, ApprovalWorkflow flow) {
    return viewer.mayApproveItemOf(creator) && viewer.mayApproveItemOf(flow.getSubmittedBy());
  }

  private static RecordFacts facts(Product p) {
    return new RecordFacts(
        "Product", p.getCode(), p.getName(), p.getCompanyId(), "/underwriting/products");
  }

  private static RecordFacts facts(OpenCover c) {
    return new RecordFacts(
        "Open cover",
        c.getOpenCoverNo(),
        c.getInsuredName(),
        c.getCompanyId(),
        "/underwriting/open-covers/" + c.getId());
  }
}
