package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.Dates;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.InsurerLine;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.Scheme;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestType;
import com.iortatechnxt.brokerverse.productmaint.domain.Signoff;
import com.iortatechnxt.brokerverse.productmaint.domain.SignoffRepository;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The requirements pack and the ManCom sign-off (BRPM.015): TSU completes the proposed rate scheme,
 * dates and computation basis and attaches the signed package slip; the submission is refused while
 * anything is missing ({@code REQUIREMENTS_INCOMPLETE}). ManCom signs off in the system (the
 * physically signed sheet is optional, PQ07): the rounds are locked and the sign-off record becomes
 * the MANCOM_SIGNOFF document of the request. A ManCom return is recorded as well.
 */
@Service
@Transactional
public class RequirementsService {

  /** Document type of the signed package slip. */
  public static final String SIGNED_SLIP = "PKG_SLIP_SIGNED";

  /** Document type of the ManCom sign-off. */
  public static final String MANCOM_SIGNOFF = "MANCOM_SIGNOFF";

  private final PackageRequests requests;
  private final NegotiationService negotiation;
  private final SignoffRepository signoffs;
  private final TermsCodec codec;
  private final PackageDocuments documents;
  private final DocumentService attachments;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests request reads
   * @param negotiation rounds (lock at sign-off)
   * @param signoffs ManCom decisions
   * @param codec terms JSON
   * @param documents package slip and sign-off record
   * @param attachments documents of the request
   * @param workflow workflow engine
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public RequirementsService(
      PackageRequests requests,
      NegotiationService negotiation,
      SignoffRepository signoffs,
      TermsCodec codec,
      PackageDocuments documents,
      DocumentService attachments,
      WorkflowService workflow,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.requests = requests;
    this.negotiation = negotiation;
    this.signoffs = signoffs;
    this.codec = codec;
    this.documents = documents;
    this.attachments = attachments;
    this.workflow = workflow;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Completes the proposed rate scheme and dates of the requirements pack.
   *
   * @param id request
   * @param scheme rate scheme and computation basis
   * @param dates effectivity and package term
   * @return the request
   */
  public PackageRequest updateRequirements(Long id, Scheme scheme, Dates dates) {
    PackageRequest p =
        requests.inStage(
            id,
            RequestStage.REQUIREMENTS_PREP,
            "PKG_NOT_IN_REQUIREMENTS",
            "The requirements are prepared after the terms are accepted");
    PackageTerms proposed = proposed(p).withScheme(scheme, dates);
    if (dates != null
        && dates.effectiveFrom() != null
        && dates.packageEndDate() != null
        && !dates.packageEndDate().isAfter(dates.effectiveFrom())) {
      throw new BusinessRuleException(
          "PKG_DATES_INVALID", "The package end date must be after the effective date");
    }
    p.propose(codec.toJson(proposed), proposed.insurerCodes());
    p.summarise(proposed.dates().packageEndDate(), proposed.scheme().defaultRate());
    audit.record(
        PackageRequests.ENTITY, p.getRequestNo(), AuditAction.UPDATE, "Requirements updated");
    return p;
  }

  /**
   * What the requirements pack still lacks (BRPM.015).
   *
   * @param id request
   * @return missing items, empty when complete
   */
  @Transactional(readOnly = true)
  public List<String> missing(Long id) {
    PackageRequest p = requests.get(id);
    PackageTerms t = proposed(p);
    List<String> missing = new ArrayList<>();
    Dates d = t.dates();
    if (d.effectiveFrom() == null || d.packageEndDate() == null) {
      missing.add("the effective date and the package end date");
    }
    Scheme s = t.scheme();
    if (s.defaultRate() == null && t.insurers().stream().anyMatch(i -> i.rate() == null)) {
      missing.add("the scheme rate or a rate for every insurer");
    }
    if (s.ratingBasisNote() == null || s.ratingBasisNote().isBlank()) {
      missing.add("the computation basis");
    }
    if (t.insurers().isEmpty()) {
      missing.add("the chosen insurer(s)");
    }
    if (!documentTypes(p).contains(SIGNED_SLIP)) {
      missing.add("the signed package slip (document " + SIGNED_SLIP + ")");
    }
    return missing;
  }

  /**
   * Submits the complete requirements pack for the ManCom sign-off (BRPM.015).
   *
   * @param id request
   * @param comment comment
   * @return the request
   */
  public PackageRequest submitRequirements(Long id, String comment) {
    PackageRequest p = requests.get(id);
    List<String> missing = missing(id);
    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          "REQUIREMENTS_INCOMPLETE", "Complete the requirements: " + String.join("; ", missing));
    }
    workflow.transition(
        PackageRequests.ENTITY,
        String.valueOf(id),
        "submit_requirements",
        TransitionNote.comment(comment));
    p.getMilestones().requirementsSubmitted(currentUser.username(), clock.instant());
    return p;
  }

  /**
   * ManCom sign-off (BRPM.015): locks the quotation slips and records the decision with the signed
   * sheet (uploaded as MANCOM_SIGNOFF) or the generated sign-off record.
   *
   * @param id request
   * @param comment comment
   * @return the sign-off
   */
  public Signoff signoff(Long id, String comment) {
    PackageRequest p = requests.get(id);
    String user = currentUser.username();
    if (CurrentUser.sameUser(user, p.getMilestones().getRequirementsBy())) {
      throw new BusinessRuleException(
          "PKG_FOUR_EYES", "ManCom signs off requirements submitted by someone else");
    }
    workflow.transition(
        PackageRequests.ENTITY, String.valueOf(id), "signoff", TransitionNote.comment(comment));
    negotiation.lockAll(id);
    Signoff s =
        signoffs.save(
            new Signoff(id, Signoff.SIGNED, reference(p), user, clock.instant(), blank(comment)));
    Long sheet = latestDocument(p, MANCOM_SIGNOFF).orElseGet(() -> storeRecord(p, s));
    s.attachSheet(sheet);
    audit.record(
        PackageRequests.ENTITY,
        p.getRequestNo(),
        AuditAction.UPDATE,
        "ManCom sign-off " + s.getReference() + " by " + user);
    return s;
  }

  /**
   * Records a ManCom return to TSU (generic action of the workflow panel).
   *
   * @param p request
   * @param comment reason and comment
   */
  public void recordReturn(PackageRequest p, String comment) {
    signoffs.save(
        new Signoff(
            p.getId(),
            Signoff.RETURNED,
            reference(p),
            currentUser.username(),
            clock.instant(),
            comment));
  }

  /**
   * ManCom decisions on a request, newest first.
   *
   * @param id request
   * @return decisions
   */
  @Transactional(readOnly = true)
  public List<Signoff> signoffs(Long id) {
    return signoffs.findByRequestIdOrderByIdDesc(requests.get(id).getId());
  }

  /**
   * The package slip to print for signature.
   *
   * @param id request
   * @return PDF
   */
  @Transactional(readOnly = true)
  public MessageFile packageSlip(Long id) {
    return documents.packageSlip(requests.get(id));
  }

  /**
   * The ManCom reference of a request.
   *
   * @param p request
   * @return reference
   */
  static String reference(PackageRequest p) {
    return "MC-" + p.getRequestNo();
  }

  private PackageTerms proposed(PackageRequest p) {
    String json = p.getProposedTerms() == null ? p.getRequestedTerms() : p.getProposedTerms();
    PackageTerms t = codec.terms(json);
    if (p.getRequestType() == RequestType.RETIRE || !t.insurers().isEmpty()) {
      return t;
    }
    return t.withInsurers(
        codec.terms(p.getRequestedTerms()).insurerCodes().stream()
            .map(InsurerLine::target)
            .toList());
  }

  private List<String> documentTypes(PackageRequest p) {
    return attachments.list(target(p)).stream().map(Attachment::getDocumentType).toList();
  }

  private Optional<Long> latestDocument(PackageRequest p, String type) {
    return attachments.list(target(p)).stream()
        .filter(a -> type.equals(a.getDocumentType()))
        .max(Comparator.comparing(Attachment::getId))
        .map(Attachment::getId);
  }

  private Long storeRecord(PackageRequest p, Signoff s) {
    MessageFile f = documents.signoffRecord(p, s);
    return attachments
        .upload(
            target(p),
            List.of(new UploadedFile(f.fileName(), f.content())),
            new UploadOptions(MANCOM_SIGNOFF, false, p.getRequestNo(), "ManCom sign-off record"))
        .get(0)
        .getId();
  }

  private static AttachmentTarget target(PackageRequest p) {
    return new AttachmentTarget(PackageRequests.ENTITY, String.valueOf(p.getId()));
  }

  private static String blank(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
