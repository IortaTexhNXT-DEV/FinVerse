package com.iortatechnxt.brokerverse.nonpackage.service;

import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.catalog.service.ProductRuleService;
import com.iortatechnxt.brokerverse.catalog.service.TsuRoutingService.TsuDecision;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest.ClientFacts;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequestRepository;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalStatus;
import com.iortatechnxt.brokerverse.nonpackage.domain.RiskDetails;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalRules.Checked;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proposal Request Forms (BRNB.005-007/014/098/102): creation with the PRF marketing reference and
 * the ARN, changes by Marketing (draft) or TSU (in the TSU queue, BRNB.007), submission with the
 * mandatory-document checklist and the TSU routing check, and Marketing approval (one stage, four
 * eyes; the BRD's TL / TH / UH chain is configurable through the approver permission, Q05). The TSU
 * accept (prepare_qs), return and void are generic actions of the workflow panel; quotation slip,
 * responses and proposal slip are in their own services; {@link ProposalStatusListener} mirrors
 * every stage.
 */
@Service
@Transactional
public class ProposalService {

  /** Entity type of PRFs in the workflow, attachments and audit trail. */
  public static final String ENTITY = "ProposalRequest";

  /** Workflow of PRFs. */
  public static final String WORKFLOW = "NB_PROPOSAL";

  private final ProposalRequestRepository proposals;
  private final ProposalRules rules;
  private final ProposalNumbers numbers;
  private final RiskDetailsCodec codec;
  private final ProductCatalogService catalog;
  private final ProductRuleService productRules;
  private final DocumentService documents;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param proposals PRFs
   * @param rules draft validation and TSU routing
   * @param numbers numbering
   * @param codec risk details JSON
   * @param catalog products
   * @param productRules mandatory documents per product
   * @param documents documents of the PRF
   * @param workflow workflow engine
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public ProposalService(
      ProposalRequestRepository proposals,
      ProposalRules rules,
      ProposalNumbers numbers,
      RiskDetailsCodec codec,
      ProductCatalogService catalog,
      ProductRuleService productRules,
      DocumentService documents,
      WorkflowService workflow,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.proposals = proposals;
    this.rules = rules;
    this.numbers = numbers;
    this.codec = codec;
    this.catalog = catalog;
    this.productRules = productRules;
    this.documents = documents;
    this.workflow = workflow;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Creates a PRF in DRAFT with its marketing reference (BRNB.006) and ARN (BRNB.102).
   *
   * @param companyId company
   * @param draft PRF data
   * @return the PRF
   */
  public ProposalRequest create(Long companyId, ProposalDraft draft) {
    Checked checked = rules.check(companyId, draft);
    RiskProduct product = checked.product();
    ProposalRequest p =
        new ProposalRequest(
            companyId, numbers.prf(), numbers.arn(), product.getCode(), product.getLineCode());
    apply(p, draft, checked);
    ProposalRequest saved = proposals.save(p);
    workflow.start(
        new StartCase(
            companyId,
            WORKFLOW,
            new CaseRecord(
                ENTITY,
                String.valueOf(saved.getId()),
                saved.getPrfNo(),
                title(saved),
                "/proposals/" + saved.getId(),
                saved.getMarketSegment()),
            null));
    audit.record(
        ENTITY,
        saved.getPrfNo(),
        AuditAction.CREATE,
        "PRF " + saved.getArn() + " for " + saved.getClientName() + ", " + product.getCode());
    return saved;
  }

  /**
   * Changes a PRF: Marketing while it is a draft, TSU while it is in the TSU queue (BRNB.007).
   *
   * @param id PRF
   * @param draft PRF data
   * @return the PRF
   */
  public ProposalRequest update(Long id, ProposalDraft draft) {
    ProposalRequest p = get(id);
    boolean marketing =
        ProposalStatus.MARKETING_EDITABLE.contains(p.getStatus())
            && currentUser.hasAuthority("PROPOSAL_REQUEST");
    boolean tsu =
        ProposalStatus.TSU_EDITABLE.contains(p.getStatus())
            && currentUser.hasAuthority("TSU_PROCESS");
    if (!marketing && !tsu) {
      throw new BusinessRuleException(
          "PRF_NOT_EDITABLE", "PRF " + p.getPrfNo() + " cannot be changed while " + p.getStatus());
    }
    if (!p.getProductCode().equals(draft.productCode())) {
      throw new BusinessRuleException("PRF_PRODUCT_FIXED", "The product of a PRF cannot change");
    }
    apply(p, draft, rules.check(p.getCompanyId(), draft));
    workflow.describe(ENTITY, String.valueOf(id), p.getPrfNo(), title(p));
    audit.record(ENTITY, p.getPrfNo(), AuditAction.UPDATE, "PRF details updated");
    return p;
  }

  private void apply(ProposalRequest p, ProposalDraft draft, Checked checked) {
    Client c = checked.client();
    p.describe(
        new ClientFacts(c.getId(), c.getCode(), c.getDisplayName(), c.getEmail()),
        blank(draft.marketSegment()),
        blank(draft.sourceChannel()),
        blank(draft.currency()));
    RiskDetails details = checked.details();
    p.describeRisk(
        draft.periodFrom(),
        draft.periodTo(),
        codec.toJson(details),
        ProposalRules.sumInsured(details));
    p.selectInsurers(draft.insurers().stream().distinct().toList());
    TsuDecision decision = rules.route(checked.product(), details);
    p.routeTsu(decision.ruleCode(), decision.required() ? decision.reason() : null);
  }

  /**
   * Submits the PRF for Marketing approval: risk details, the product's mandatory documents
   * (BRNB.005) and a TSU routing reason (a package risk below every TSU rule is quoted directly).
   *
   * @param id PRF
   * @param comment comment
   * @return the PRF
   */
  public ProposalRequest submit(Long id, String comment) {
    ProposalRequest p = get(id);
    RiskDetails details = codec.of(p);
    if (details.items().isEmpty() && details.sections().isEmpty()) {
      throw new BusinessRuleException(
          "PRF_INCOMPLETE", "Describe the risk (sections or risk items) before submitting");
    }
    List<String> missing = missingDocuments(p);
    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          "MISSING_DOCUMENTS", "Attach the mandatory documents: " + String.join(", ", missing));
    }
    TsuDecision decision = rules.route(catalog.requireUsableProduct(p.getProductCode()), details);
    if (!decision.required()) {
      throw new BusinessRuleException(
          "PRF_NOT_NEEDED",
          "No TSU rule applies to this package risk: quote it directly as a package quotation");
    }
    p.routeTsu(decision.ruleCode(), decision.reason());
    workflow.transition(ENTITY, String.valueOf(id), "submit", TransitionNote.comment(comment));
    p.markSubmitted(currentUser.username(), clock.instant());
    return p;
  }

  /**
   * Marketing approval: sends the PRF to the TSU queue (four eyes).
   *
   * @param id PRF
   * @param comment comment
   * @return the PRF
   */
  public ProposalRequest approve(Long id, String comment) {
    ProposalRequest p = get(id);
    String user = currentUser.username();
    if (CurrentUser.sameUser(user, p.getCreatedBy())
        || CurrentUser.sameUser(user, p.getSubmittedBy())) {
      throw new BusinessRuleException(
          "PRF_FOUR_EYES", "A PRF is approved by someone other than its maker");
    }
    workflow.transition(ENTITY, String.valueOf(id), "approve", TransitionNote.comment(comment));
    p.markApproved(user, clock.instant());
    return p;
  }

  /**
   * The mandatory-document checklist of a PRF (product document rules, BRNB.005).
   *
   * @param id PRF
   * @return required document types with their presence
   */
  @Transactional(readOnly = true)
  public List<ChecklistItem> checklist(Long id) {
    ProposalRequest p = get(id);
    Set<String> present = presentDocuments(p);
    return productRules.requiredDocuments(catalog.requireProduct(p.getProductCode())).stream()
        .sorted()
        .map(type -> new ChecklistItem(type, present.contains(type)))
        .toList();
  }

  private List<String> missingDocuments(ProposalRequest p) {
    Set<String> present = presentDocuments(p);
    return productRules.requiredDocuments(catalog.requireProduct(p.getProductCode())).stream()
        .filter(type -> !present.contains(type))
        .sorted()
        .toList();
  }

  private Set<String> presentDocuments(ProposalRequest p) {
    return documents.documentTypesOf(new AttachmentTarget(ENTITY, String.valueOf(p.getId())));
  }

  /**
   * One PRF.
   *
   * @param id id
   * @return PRF
   */
  @Transactional(readOnly = true)
  public ProposalRequest get(Long id) {
    return proposals.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Work case title.
   *
   * @param p PRF
   * @return title
   */
  static String title(ProposalRequest p) {
    return p.getClientName() + " - " + p.getProductCode();
  }

  private static String blank(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * A mandatory document and whether it is attached.
   *
   * @param documentType document type (list DOCUMENT_TYPE)
   * @param attached whether the PRF has it
   */
  public record ChecklistItem(String documentType, boolean attached) {}
}
