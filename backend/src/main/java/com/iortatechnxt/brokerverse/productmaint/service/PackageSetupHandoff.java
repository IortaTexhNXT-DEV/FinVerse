package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionStatus;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageSetupService;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageSpec;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionQueryService;
import com.iortatechnxt.brokerverse.catalog.service.version.VersionRef;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.productmaint.domain.Advisory;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestType;
import com.iortatechnxt.brokerverse.productmaint.domain.Signoff;
import com.iortatechnxt.brokerverse.productmaint.domain.SignoffRepository;
import com.iortatechnxt.brokerverse.productmaint.service.VersionTerms.SetupFacts;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The MBS step of a package request (BRPM.015): MBS receives only signed-off requirements, returns
 * incomplete ones to TSU with a reason, and sets the package up through the catalog {@link
 * PackageSetupService} (a DRAFT version created from the proposed terms, or the returned draft
 * replaced); the request then waits for the catalog validation checkpoint (PMADD06). A RETIRE
 * request retires the product instead (BRPM.011 "deletion", BRPM.006) and drafts the retirement
 * advisory.
 */
@Service
@Transactional
public class PackageSetupHandoff {

  private final PackageRequests requests;
  private final PackageSetupService setup;
  private final ProductVersionQueryService versions;
  private final SignoffRepository signoffs;
  private final AdvisoryService advisories;
  private final TermsCodec codec;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests request reads
   * @param setup catalog package set-up
   * @param versions catalog package versions
   * @param signoffs ManCom decisions
   * @param advisories advisories
   * @param codec terms JSON
   * @param workflow workflow engine
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public PackageSetupHandoff(
      PackageRequests requests,
      PackageSetupService setup,
      ProductVersionQueryService versions,
      SignoffRepository signoffs,
      AdvisoryService advisories,
      TermsCodec codec,
      WorkflowService workflow,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.requests = requests;
    this.setup = setup;
    this.versions = versions;
    this.signoffs = signoffs;
    this.advisories = advisories;
    this.codec = codec;
    this.workflow = workflow;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Sets the package up as a DRAFT catalog version and hands it to the validation checkpoint.
   *
   * @param id request
   * @param input product code and name of a new package, change summary, comment
   * @return the request, now FOR_VALIDATION
   */
  public PackageRequest setUp(Long id, SetupInput input) {
    PackageRequest p = withMbs(id);
    if (p.getRequestType() == RequestType.RETIRE) {
      throw new BusinessRuleException(
          "PKG_REQUEST_TYPE_MISMATCH", "A RETIRE request retires the package instead");
    }
    String code =
        p.getTargetProductCode() == null ? blank(input.productCode()) : p.getTargetProductCode();
    if (code == null) {
      throw new BusinessRuleException(
          "PRODUCT_CODE_REQUIRED", "Enter the risk code of the new package");
    }
    PackageSpec.NewProduct identity = newProduct(p, input);
    PackageTerms terms =
        codec.terms(p.getProposedTerms() == null ? p.getRequestedTerms() : p.getProposedTerms());
    PackageSpec spec =
        VersionTerms.spec(
            new SetupFacts(
                p.getCompanyId(), code, identity, p.getBaseVersionNo(), origin(p, input)),
            terms);
    VersionRef ref = draftOf(p, code, spec);
    p.markSetUp(ref.productCode(), ref.versionNo());
    workflow.transition(
        PackageRequests.ENTITY,
        String.valueOf(id),
        "setup",
        TransitionNote.comment(
            "Version " + ref.versionNo() + " of " + ref.productCode() + comment(input.comment())));
    p.getMilestones().setUp(currentUser.username(), clock.instant());
    audit.record(
        PackageRequests.ENTITY,
        p.getRequestNo(),
        AuditAction.UPDATE,
        "Package set up as version " + ref.versionNo() + " of " + ref.productCode());
    return p;
  }

  private static PackageSpec.NewProduct newProduct(PackageRequest p, SetupInput input) {
    if (p.getRequestType() != RequestType.NEW) {
      return null;
    }
    String name = blank(input.productName());
    return new PackageSpec.NewProduct(
        name == null ? p.getTitle() : name,
        p.getLineCode(),
        p.getCoverTypeCode(),
        p.getMarketSegmentList(),
        p.getClientCode());
  }

  private VersionRef draftOf(PackageRequest p, String code, PackageSpec spec) {
    Integer previous = p.getResultingVersionNo();
    boolean returnedDraft =
        previous != null
            && versions
                .version(code, previous)
                .filter(v -> v.status() == ProductVersionStatus.DRAFT)
                .isPresent();
    return returnedDraft
        ? setup.updateDraftVersion(code, previous, spec)
        : setup.createDraftVersion(spec);
  }

  /**
   * Returns incomplete requirements to TSU (BRPM.015).
   *
   * @param id request
   * @param reasonCode reason (list RETURN_REASON)
   * @param comment comment
   * @return the request
   */
  public PackageRequest returnIncomplete(Long id, String reasonCode, String comment) {
    PackageRequest p = withMbs(id);
    workflow.transition(
        PackageRequests.ENTITY,
        String.valueOf(id),
        "return_incomplete",
        new TransitionNote(reasonCode, comment));
    return p;
  }

  /**
   * Retires the package of a RETIRE request (BRPM.011/006) and drafts the retirement advisory.
   *
   * @param id request
   * @param comment comment
   * @return the request, now RETIRED
   */
  public PackageRequest retire(Long id, String comment) {
    PackageRequest p = withMbs(id);
    if (p.getRequestType() != RequestType.RETIRE) {
      throw new BusinessRuleException(
          "PKG_REQUEST_TYPE_MISMATCH", "Only a RETIRE request retires the package");
    }
    setup.retireProduct(
        p.getTargetProductCode(),
        new PackageSpec.Origin(p.getRequestNo(), mancomReference(p), "Retired: " + p.getReason()));
    workflow.transition(
        PackageRequests.ENTITY, String.valueOf(id), "retire", TransitionNote.comment(comment));
    p.getMilestones().setUp(currentUser.username(), clock.instant());
    advisories.draftFor(p, Advisory.Type.RETIREMENT, null);
    return p;
  }

  private PackageRequest withMbs(Long id) {
    return requests.inStage(
        id,
        RequestStage.WITH_MBS,
        "PKG_NOT_WITH_MBS",
        "MBS receives signed-off requirements only (BRPM.015)");
  }

  private PackageSpec.Origin origin(PackageRequest p, SetupInput input) {
    String summary =
        input.changeSummary() == null || input.changeSummary().isBlank()
            ? p.getRequestType() + " request " + p.getRequestNo() + " (" + p.getReason() + ")"
            : input.changeSummary().strip();
    return new PackageSpec.Origin(p.getRequestNo(), mancomReference(p), summary);
  }

  private String mancomReference(PackageRequest p) {
    return signoffs.findByRequestIdOrderByIdDesc(p.getId()).stream()
        .filter(s -> Signoff.SIGNED.equals(s.getDecision()))
        .map(Signoff::getReference)
        .findFirst()
        .orElse(null);
  }

  private static String comment(String comment) {
    return comment == null || comment.isBlank() ? "" : " - " + comment.strip();
  }

  private static String blank(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * What MBS enters at the set-up.
   *
   * @param productCode risk code of a new package (NEW requests), ignored otherwise
   * @param productName name of a new package, null for the request title
   * @param changeSummary what changes against the base version, null for a default
   * @param comment comment
   */
  public record SetupInput(
      String productCode, String productName, String changeSummary, String comment) {}
}
