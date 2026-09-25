package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionQueryService;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionView;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import com.iortatechnxt.brokerverse.productmaint.service.PackageRequestRules.Checked;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Package requests up to the start of the negotiation (BRPM.008/009/011/021): creation with the PKR
 * number and the PM_PACKAGE_REQUEST work case, changes by Marketing (draft) or TSU (review),
 * submission with the completeness check, Marketing approval (four eyes), the TSU Team Lead
 * recommendation and the TSU Head approval (never the recommender), with or without negotiation.
 * Returns and voids are generic actions of the workflow panel; {@link PackageStatusListener}
 * mirrors every stage.
 */
@Service
@Transactional
public class PackageRequestService {

  private static final String FOUR_EYES = "PKG_FOUR_EYES";

  private final PackageRequests requests;
  private final PackageRequestRules rules;
  private final PackageNumbers numbers;
  private final TermsCodec codec;
  private final ProductVersionQueryService versions;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests request reads
   * @param rules form validation
   * @param numbers numbering
   * @param codec terms JSON
   * @param versions catalog package versions (pre-fill)
   * @param workflow workflow engine
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public PackageRequestService(
      PackageRequests requests,
      PackageRequestRules rules,
      PackageNumbers numbers,
      TermsCodec codec,
      ProductVersionQueryService versions,
      WorkflowService workflow,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.requests = requests;
    this.rules = rules;
    this.numbers = numbers;
    this.codec = codec;
    this.versions = versions;
    this.workflow = workflow;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Creates a request in DRAFT with its PKR number and work case (BRPM.008).
   *
   * @param companyId company
   * @param draft form data
   * @return the request
   */
  public PackageRequest create(Long companyId, RequestDraft draft) {
    if (draft.type() == null || draft.scope() == null) {
      throw new BusinessRuleException(
          PackageRequestRules.INCOMPLETE, "Select the request type and the package scope");
    }
    Checked checked = rules.check(companyId, draft);
    PackageRequest p = new PackageRequest(companyId, numbers.request(), draft.type());
    apply(p, checked);
    PackageRequest saved = requests.save(p);
    workflow.start(
        new StartCase(
            companyId,
            PackageRequests.WORKFLOW,
            new CaseRecord(
                PackageRequests.ENTITY,
                String.valueOf(saved.getId()),
                saved.getRequestNo(),
                PackageRequests.title(saved),
                PackageRequests.link(saved),
                saved.getLineCode()),
            null));
    audit.record(
        PackageRequests.ENTITY,
        saved.getRequestNo(),
        AuditAction.CREATE,
        saved.getRequestType() + " package request '" + saved.getTitle() + "'");
    return saved;
  }

  /**
   * Changes a request: Marketing or TSU while it is a draft, the TSU Team Lead during the review
   * (BRPM.011: TSU completes the form instead of re-keying it).
   *
   * @param id request
   * @param draft form data (the type cannot change)
   * @return the request
   */
  public PackageRequest update(Long id, RequestDraft draft) {
    PackageRequest p = requests.get(id);
    boolean maker = p.getStatus() == RequestStage.DRAFT && currentUser.hasAuthority("PKG_REQUEST");
    boolean reviewer =
        p.getStatus() == RequestStage.FOR_TSU_REVIEW
            && currentUser.hasAuthority("PKG_TSU_RECOMMEND");
    if (!maker && !reviewer) {
      throw new BusinessRuleException(
          "PKG_REQUEST_NOT_EDITABLE",
          "Request " + p.getRequestNo() + " cannot be changed while " + p.getStatus());
    }
    if (draft.type() != null && draft.type() != p.getRequestType()) {
      throw new BusinessRuleException("PKG_TYPE_FIXED", "The type of a request cannot change");
    }
    Checked checked =
        rules.check(
            p.getCompanyId(),
            new RequestDraft(
                p.getRequestType(),
                draft.scope() == null ? p.getScope() : draft.scope(),
                draft.title(),
                draft.clientId(),
                draft.lineCode(),
                draft.coverTypeCode(),
                draft.productCode(),
                draft.baseVersionNo(),
                draft.marketSegments(),
                draft.reason(),
                draft.reasonNote(),
                draft.negotiationRequired(),
                draft.terms()));
    apply(p, checked);
    workflow.describe(
        PackageRequests.ENTITY, String.valueOf(id), p.getRequestNo(), PackageRequests.title(p));
    audit.record(
        PackageRequests.ENTITY, p.getRequestNo(), AuditAction.UPDATE, "Request details updated");
    return p;
  }

  private void apply(PackageRequest p, Checked checked) {
    PackageTerms terms = checked.terms();
    p.describe(checked.form(), codec.toJson(terms));
    p.summarise(terms.dates().packageEndDate(), terms.scheme().defaultRate());
  }

  /**
   * Submits the request for Marketing approval after the completeness check (BRPM.008/021).
   *
   * @param id request
   * @param comment comment
   * @return the request
   */
  public PackageRequest submit(Long id, String comment) {
    PackageRequest p = requests.get(id);
    List<String> missing =
        TermsChecks.missing(
            p.getRequestType(),
            p.getCoverTypeCode(),
            p.isNegotiationRequired(),
            codec.terms(p.getRequestedTerms()));
    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          PackageRequestRules.INCOMPLETE, "Complete the request: " + String.join("; ", missing));
    }
    workflow.transition(
        PackageRequests.ENTITY, String.valueOf(id), "submit", TransitionNote.comment(comment));
    p.getMilestones().submitted(currentUser.username(), clock.instant());
    return p;
  }

  /**
   * Marketing TL / TH / UH approval: sends the request to the TSU Team Lead (four eyes, BRPM.008).
   *
   * @param id request
   * @param comment comment
   * @return the request
   */
  public PackageRequest approve(Long id, String comment) {
    PackageRequest p = requests.get(id);
    String user = currentUser.username();
    if (CurrentUser.sameUser(user, p.getCreatedBy())
        || CurrentUser.sameUser(user, p.getMilestones().getSubmittedBy())) {
      throw new BusinessRuleException(
          FOUR_EYES, "A package request is approved by someone other than its maker");
    }
    workflow.transition(
        PackageRequests.ENTITY, String.valueOf(id), "approve", TransitionNote.comment(comment));
    p.getMilestones().approved(user, clock.instant());
    return p;
  }

  /**
   * TSU Team Lead review: records the recommendation shown to the TSU Head (BRPM.009).
   *
   * @param id request
   * @param recommendation recommendation (mandatory)
   * @return the request
   */
  public PackageRequest recommend(Long id, String recommendation) {
    if (recommendation == null || recommendation.isBlank()) {
      throw new BusinessRuleException(
          "PKG_RECOMMENDATION_REQUIRED", "Write the recommendation for the TSU Head");
    }
    PackageRequest p = requests.get(id);
    workflow.transition(
        PackageRequests.ENTITY,
        String.valueOf(id),
        "recommend",
        TransitionNote.comment(recommendation.strip()));
    p.recommend(recommendation.strip());
    p.getMilestones().recommended(currentUser.username(), clock.instant());
    return p;
  }

  /**
   * TSU Head approval (BRPM.009): opens the negotiation, or — for a request that needs no
   * negotiation (RETIRE, a renewal on unchanged terms) — sends the requested terms to ManCom.
   *
   * @param id request
   * @param comment comment
   * @return the request
   */
  public PackageRequest approveTsu(Long id, String comment) {
    PackageRequest p = requests.get(id);
    String user = currentUser.username();
    if (CurrentUser.sameUser(user, p.getMilestones().getRecommendedBy())) {
      throw new BusinessRuleException(
          FOUR_EYES, "The TSU Head approval is given by someone other than the recommender");
    }
    String action = p.isNegotiationRequired() ? "approve" : "approve_no_negotiation";
    if (!p.isNegotiationRequired()) {
      PackageTerms terms = codec.terms(p.getRequestedTerms());
      p.propose(p.getRequestedTerms(), terms.insurerCodes());
    }
    workflow.transition(
        PackageRequests.ENTITY, String.valueOf(id), action, TransitionNote.comment(comment));
    p.getMilestones().tsuApproved(user, clock.instant());
    return p;
  }

  /**
   * The terms of a package's current (or latest) version, to pre-fill an AMEND, UPDATE, RENEW or
   * REACTIVATE request (BRPM.011/017: no re-keying).
   *
   * @param productCode product
   * @return terms and the version they come from
   */
  @Transactional(readOnly = true)
  public Prefill prefill(String productCode) {
    ProductVersionView v =
        versions
            .current(productCode)
            .or(() -> versions.versions(productCode).stream().findFirst())
            .orElseThrow(() -> new ResourceNotFoundException("Package version", productCode));
    return new Prefill(productCode, v.versionNo(), v.status().name(), VersionTerms.of(v));
  }

  /**
   * Pre-filled terms of a package.
   *
   * @param productCode product
   * @param versionNo version the terms come from
   * @param versionStatus its status
   * @param terms terms
   */
  public record Prefill(
      String productCode, int versionNo, String versionStatus, PackageTerms terms) {}
}
