package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.TsuRoutingService.TsuDecision;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.docgen.domain.DocTemplate;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationContent;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationHeader;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationHeader.ClientFacts;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationRepository;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationStatus;
import com.iortatechnxt.brokerverse.quotation.service.QuotationRules.Offer;
import com.iortatechnxt.brokerverse.quotation.service.QuotationRules.Resolved;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Package quotations (BRNB.020/021/043/063/102, MKTID.011): creation with the ARN and the intake
 * template version, draft changes (a change after submission opens version n+1), and the business
 * actions submit, approve (four eyes, BRNB.014/021), revise and decline of the NB_QUOTATION
 * workflow. Sending, acceptance and account creation are in {@link QuotationDispatchService} and
 * {@link QuotationAcceptanceService}; generic actions (return, void) run from the workflow panel
 * and {@link QuotationStatusListener} mirrors every stage. The workflow engine notifies the
 * originator of every change made by someone else (BRNB.015).
 */
@Service("brokingQuotationService")
@Transactional
public class QuotationService {

  /** Entity type of quotations in the workflow, attachments and audit trail. */
  public static final String ENTITY = "Quotation";

  /** Workflow of quotations. */
  public static final String WORKFLOW = "NB_QUOTATION";

  /** Intake template stamped on each quotation (BRNB.004). */
  public static final String TEMPLATE = "QUOTATION_LETTER";

  private static final String QUOTATION = "Quotation ";

  private final QuotationRepository quotations;
  private final QuotationVersions versions;
  private final QuotationRules rules;
  private final QuotationPricing pricing;
  private final QuotationNumbers numbers;
  private final QuotationRequestService requests;
  private final DocTemplateService templates;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param quotations quotations
   * @param versions version store
   * @param rules draft validation
   * @param pricing premium and TSU routing
   * @param numbers numbering
   * @param requests quotation requests
   * @param templates document templates
   * @param workflow workflow engine
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public QuotationService(
      QuotationRepository quotations,
      QuotationVersions versions,
      QuotationRules rules,
      QuotationPricing pricing,
      QuotationNumbers numbers,
      QuotationRequestService requests,
      DocTemplateService templates,
      WorkflowService workflow,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.quotations = quotations;
    this.versions = versions;
    this.rules = rules;
    this.pricing = pricing;
    this.numbers = numbers;
    this.requests = requests;
    this.templates = templates;
    this.workflow = workflow;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Creates a quotation in DRAFT with its quotation number, ARN (BRNB.102) and version 1, priced
   * with the rates in force, and opens its work case. A prospect is enough (BRNB.063).
   *
   * @param companyId company
   * @param draft quotation data
   * @return the quotation
   */
  public Quotation create(Long companyId, QuotationDraft draft) {
    Resolved resolved = rules.resolve(companyId, draft);
    if (resolved.content().validUntil().isBefore(LocalDate.now(clock))) {
      throw new BusinessRuleException(
          "QUOTATION_VALIDITY_PAST", "The validity date cannot be in the past");
    }
    RiskProduct product = resolved.product();
    DocTemplate template = templates.current(TEMPLATE, LocalDate.now(clock));
    Quotation quotation =
        Quotation.create(
            new QuotationHeader(
                companyId,
                numbers.quotation(),
                numbers.arn(),
                draft.requestId(),
                facts(resolved.client()),
                product.getCode(),
                product.getLineCode(),
                QuotationRules.blankToNull(draft.marketSegment()),
                QuotationRules.blankToNull(draft.sourceChannel()),
                QuotationRules.blankToNull(draft.currency()),
                template.getCode() + " v" + template.getVersionNo()));
    QuotationContent priced = show(quotation, product, resolved.content());
    Quotation saved = quotations.save(quotation);
    versions.write(saved, priced);
    workflow.start(
        new StartCase(
            companyId,
            WORKFLOW,
            new CaseRecord(
                ENTITY,
                String.valueOf(saved.getId()),
                saved.getQuotationNo(),
                title(saved),
                "/quotations/" + saved.getId(),
                saved.getMarketSegment()),
            null));
    if (draft.requestId() != null) {
      requests.markQuoted(draft.requestId(), saved);
    }
    audit.record(
        ENTITY,
        saved.getQuotationNo(),
        AuditAction.CREATE,
        QUOTATION + saved.getArn() + " for " + saved.getClientName() + ", " + product.getCode());
    return saved;
  }

  /**
   * Prices a draft without saving it (live premium of the wizard); the client is not needed.
   *
   * @param companyId company
   * @param draft quotation data
   * @return priced content and TSU routing
   */
  @Transactional(readOnly = true)
  public Preview preview(Long companyId, QuotationDraft draft) {
    Offer offer = rules.offer(companyId, draft);
    QuotationContent priced = pricing.price(companyId, offer.product(), offer.content());
    TsuDecision decision = pricing.tsu(offer.product(), priced);
    return new Preview(priced, decision.required(), decision.required() ? decision.reason() : null);
  }

  /**
   * Changes a draft quotation. The client may change (e.g. a prospect replaced by the confirmed
   * client), the product may not. After a submission the change opens version n+1 (BRNB.020).
   *
   * @param id quotation
   * @param draft quotation data
   * @return the quotation
   */
  public Quotation update(Long id, QuotationDraft draft) {
    Quotation quotation = get(id);
    if (quotation.getStatus() != QuotationStatus.DRAFT) {
      throw new BusinessRuleException(
          "QUOTATION_NOT_EDITABLE",
          QUOTATION + quotation.getQuotationNo() + " is " + quotation.getStatus());
    }
    if (!quotation.getProductCode().equals(draft.productCode())) {
      throw new BusinessRuleException(
          "QUOTATION_PRODUCT_FIXED", "The product of a quotation cannot change");
    }
    Resolved resolved = rules.resolve(quotation.getCompanyId(), draft);
    boolean newVersion = !quotation.isVersionOpen();
    if (newVersion) {
      quotation.openNextVersion();
    }
    quotation.describe(
        facts(resolved.client()),
        QuotationRules.blankToNull(draft.marketSegment()),
        QuotationRules.blankToNull(draft.sourceChannel()));
    QuotationContent priced = show(quotation, resolved.product(), resolved.content());
    versions.write(quotation, priced);
    workflow.describe(ENTITY, String.valueOf(id), quotation.getQuotationNo(), title(quotation));
    audit.record(
        ENTITY,
        quotation.getQuotationNo(),
        AuditAction.UPDATE,
        (newVersion ? "Version " : "Updated version ") + quotation.getCurrentVersion());
    return quotation;
  }

  private QuotationContent show(Quotation quotation, RiskProduct product, QuotationContent raw) {
    QuotationContent priced = pricing.price(quotation.getCompanyId(), product, raw);
    quotation.showContent(priced, QuotationPricing.totalSumInsured(priced));
    TsuDecision decision = pricing.tsu(product, priced);
    quotation.routeTsu(decision.required(), decision.reason());
    return priced;
  }

  /**
   * Submits the current version for approval: it must have items, a rated premium and a validity
   * not yet past. The version is frozen (read-only) from now on.
   *
   * @param id quotation
   * @param comment comment
   * @return the quotation
   */
  public Quotation submit(Long id, String comment) {
    Quotation quotation = get(id);
    QuotationContent content = versions.current(quotation);
    if (content.items().isEmpty()) {
      throw new BusinessRuleException("QUOTATION_NO_ITEMS", "Add at least one risk item");
    }
    if (!content.isRated()) {
      throw new BusinessRuleException(
          "QUOTATION_NOT_RATED",
          "The premium cannot be computed: give each item a sum insured (and a rate when the"
              + " product has no default rate)");
    }
    if (content.validUntil().isBefore(LocalDate.now(clock))) {
      throw new BusinessRuleException(
          "QUOTATION_EXPIRED", "The validity date has passed: change it before submitting");
    }
    workflow.transition(ENTITY, String.valueOf(id), "submit", TransitionNote.comment(comment));
    versions.freeze(quotation, currentUser.username(), clock.instant());
    quotation.markSubmitted(currentUser.username(), clock.instant());
    return quotation;
  }

  /**
   * Approves a submitted quotation (BRNB.014/021): four eyes, the creator and the submitter may not
   * approve it.
   *
   * @param id quotation
   * @param comment comment
   * @return the quotation
   */
  public Quotation approve(Long id, String comment) {
    Quotation quotation = get(id);
    String user = currentUser.username();
    if (CurrentUser.sameUser(user, quotation.getCreatedBy())
        || CurrentUser.sameUser(user, quotation.getSubmittedBy())) {
      throw new BusinessRuleException(
          "QUOTATION_FOUR_EYES", "A quotation is approved by someone other than its maker");
    }
    workflow.transition(ENTITY, String.valueOf(id), "approve", TransitionNote.comment(comment));
    quotation.markApproved(user, clock.instant());
    return quotation;
  }

  /**
   * Reopens an approved or sent quotation as a draft; the next change is version n+1.
   *
   * @param id quotation
   * @param comment comment
   * @return the quotation
   */
  public Quotation revise(Long id, String comment) {
    Quotation quotation = get(id);
    workflow.transition(ENTITY, String.valueOf(id), "revise", TransitionNote.comment(comment));
    return quotation;
  }

  /**
   * Records that the client declined the quotation.
   *
   * @param id quotation
   * @param comment comment
   * @return the quotation
   */
  public Quotation decline(Long id, String comment) {
    Quotation quotation = get(id);
    workflow.transition(ENTITY, String.valueOf(id), "decline", TransitionNote.comment(comment));
    return quotation;
  }

  /**
   * One quotation.
   *
   * @param id id
   * @return quotation
   */
  @Transactional(readOnly = true)
  public Quotation get(Long id) {
    return quotations.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Client facts of a quotation.
   *
   * @param client client
   * @return facts
   */
  static ClientFacts facts(Client client) {
    return new ClientFacts(
        client.getId(), client.getCode(), client.getDisplayName(), client.getEmail());
  }

  /**
   * Work case title.
   *
   * @param q quotation
   * @return title
   */
  static String title(Quotation q) {
    return q.getClientName() + " - " + q.getProductCode();
  }

  /**
   * A priced draft.
   *
   * @param content content with premiums
   * @param tsuRequired whether a TSU routing rule applies (BRNB.098)
   * @param tsuReason rule description
   */
  public record Preview(QuotationContent content, boolean tsuRequired, String tsuReason) {}
}
