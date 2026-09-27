package com.iortatechnxt.brokerverse.submitted.review.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.submitted.domain.SbmApprovalMatrix;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHistorySource;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaaf;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaafLink;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaafLinkRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaafRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaafReview;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaafReviewRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService;
import com.iortatechnxt.brokerverse.submitted.review.service.SignedPdfs.Rendered;
import com.iortatechnxt.brokerverse.submitted.service.SbmHistoryService;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Policy reviews and the IAAF (BRIDSP-05-07; FRS FR-SP-040, 041): the reviewer records each review
 * of a record in review; findings are e-mailed to the bank counterpart; once the last review is
 * adequate the reviewer generates the one IAAF of the policy, links related policies and submits it
 * to the IAAF matrix. The last approval renders the signed PDF; Send to Bank Counterpart e-mails it
 * and issues the IAAF.
 */
@Service
@Transactional
public class IaafService {

  private static final String ENTITY = "SubmittedIaaf";
  private static final Set<String> RELATIONS = Set.of("PREVIOUS_TERM", "SAME_BORROWER", "OTHER");

  private final SbmIaafRepository iaafs;
  private final SbmIaafReviewRepository reviews;
  private final SbmIaafLinkRepository links;
  private final SbmPolicyRepository policies;
  private final MasterlistService masterlist;
  private final DocumentApprovals approvals;
  private final SignedPdfs pdfs;
  private final MessageService messages;
  private final SbmHistoryService history;
  private final LovService lovs;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param iaafs IAAFs
   * @param reviews reviews
   * @param links IAAF links
   * @param policies masterlist
   * @param masterlist masterlist (scope)
   * @param approvals matrix approvals
   * @param pdfs signed PDFs
   * @param messages outbound e-mail
   * @param history record history
   * @param lovs lists of values
   * @param numbers document numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the IAAF
  public IaafService(
      SbmIaafRepository iaafs,
      SbmIaafReviewRepository reviews,
      SbmIaafLinkRepository links,
      SbmPolicyRepository policies,
      MasterlistService masterlist,
      DocumentApprovals approvals,
      SignedPdfs pdfs,
      MessageService messages,
      SbmHistoryService history,
      LovService lovs,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.iaafs = iaafs;
    this.reviews = reviews;
    this.links = links;
    this.policies = policies;
    this.masterlist = masterlist;
    this.approvals = approvals;
    this.pdfs = pdfs;
    this.messages = messages;
    this.history = history;
    this.lovs = lovs;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Records a review (FR-SP-040); findings are e-mailed to the bank counterpart of the record.
   *
   * @param policyId record in review
   * @param content date, adequacy, findings and remarks
   * @return the review
   */
  public SbmIaafReview review(Long policyId, SbmIaafReview.Content content) {
    SbmPolicy p = masterlist.get(policyId);
    validate(content);
    List<SbmIaafReview> earlier = reviews.findByPolicyIdOrderByReviewNoAsc(policyId);
    SbmIaafReview r =
        reviews.save(
            new SbmIaafReview(policyId, earlier.size() + 1, currentUser.username(), content));
    p.adequacy(content.adequacy());
    iaafs.findByPolicyId(policyId).ifPresent(i -> r.iaaf(i.getId()));
    if (SbmIaafReview.WITH_FINDINGS.equals(content.adequacy())) {
      sendFindings(p, r);
    }
    history.note(
        p,
        "Policy review",
        "Review " + r.getReviewNo() + ": " + DisplayFormat.words(content.adequacy()),
        SbmHistorySource.MANUAL,
        null);
    audit.record(
        SubmittedCodes.ENTITY, p.getSbmNo(), AuditAction.CREATE, "Review " + r.getReviewNo());
    return r;
  }

  private void validate(SbmIaafReview.Content c) {
    if (c.adequacy() == null
        || !SbmIaafReview.ADEQUATE.equals(c.adequacy())
            && !SbmIaafReview.WITH_FINDINGS.equals(c.adequacy())) {
      throw new BusinessRuleException("SBM_ADEQUACY_REQUIRED", "Select the adequacy");
    }
    if (c.reviewDate() == null) {
      throw new BusinessRuleException("SBM_REVIEW_DATE_REQUIRED", "Review date is required");
    }
    if (c.reviewDate().isAfter(BusinessClock.today(clock))) {
      throw new BusinessRuleException(
          "SBM_REVIEW_DATE_FUTURE", "The review date cannot be in the future");
    }
    if (SbmIaafReview.WITH_FINDINGS.equals(c.adequacy()) && c.findings().isEmpty()) {
      throw new BusinessRuleException("SBM_FINDINGS_REQUIRED", "Select the findings");
    }
    LocalDate today = BusinessClock.today(clock);
    c.findings().forEach(f -> lovs.requireValid(SubmittedCodes.LOV_FINDING, f, today));
  }

  private void sendFindings(SbmPolicy p, SbmIaafReview r) {
    String to = p.getAssured().bankCounterpartEmail();
    if (to == null || to.isBlank()) {
      return;
    }
    String findings =
        r.findingList().stream()
            .map(f -> lovs.label(SubmittedCodes.LOV_FINDING, f))
            .collect(Collectors.joining("\n- ", "- ", ""));
    messages.queueEmail(
        new OutboundEmail(
            p.getCompanyId(),
            "SBM_REVIEW_FINDINGS",
            List.of(to),
            List.of(),
            "Policy review findings - " + p.getAssured().assuredName() + " - " + p.getSbmNo(),
            "The review of the policy "
                + nz(p.getTerms().policyNo())
                + " of "
                + p.getAssured().assuredName()
                + " found:\n"
                + findings
                + (r.getRemarks() == null ? "" : "\n\nRemarks: " + r.getRemarks())
                + "\n\nPlease send the corrected policy or endorsement.",
            List.of(),
            null,
            new RecordLink(SubmittedCodes.ENTITY, p.getId().toString(), p.getSbmNo())));
    r.sent(to, clock.instant());
  }

  /**
   * Generates the IAAF of a policy (one per policy) once its last review is adequate, with links to
   * related policies.
   *
   * @param policyId record
   * @param related related policies and their relation
   * @return the IAAF (draft)
   */
  public SbmIaaf generate(Long policyId, Map<Long, String> related) {
    SbmPolicy p = masterlist.get(policyId);
    iaafs
        .findByPolicyId(policyId)
        .ifPresent(
            i -> {
              throw new BusinessRuleException(
                  "SBM_IAAF_EXISTS",
                  "Policy " + p.getSbmNo() + " already has IAAF " + i.getIaafNo());
            });
    List<SbmIaafReview> done = reviews.findByPolicyIdOrderByReviewNoAsc(policyId);
    if (done.isEmpty() || !SbmIaafReview.ADEQUATE.equals(done.get(done.size() - 1).getAdequacy())) {
      throw new BusinessRuleException(
          "SBM_IAAF_FINDINGS_OPEN",
          "The last review has findings; record an adequate review first");
    }
    SbmIaaf i =
        iaafs.save(
            new SbmIaaf(
                p.getCompanyId(),
                numbers.next("IAAF-" + BusinessClock.today(clock).getYear()),
                policyId));
    done.forEach(r -> r.iaaf(i.getId()));
    link(i, related);
    approvals.start(
        DocumentApprovals.IAAF,
        i,
        p.getAssured().assuredName(),
        DocumentApprovals.linkOf(DocumentApprovals.IAAF, i));
    history.note(
        p, "IAAF", "IAAF " + i.getIaafNo() + " generated", SbmHistorySource.MANUAL, i.getIaafNo());
    audit.record(ENTITY, i.getIaafNo(), AuditAction.CREATE, "IAAF of " + p.getSbmNo());
    return i;
  }

  private void link(SbmIaaf i, Map<Long, String> related) {
    if (related == null) {
      return;
    }
    related.forEach(
        (id, relation) -> {
          if (!RELATIONS.contains(relation)) {
            throw new BusinessRuleException(
                "SBM_LINK_RELATION", "Select previous term, same borrower or other");
          }
          SbmPolicy other = masterlist.get(id);
          if (!other.getId().equals(i.getPolicyId())) {
            links.save(new SbmIaafLink(i.getId(), other.getId(), relation));
          }
        });
  }

  /**
   * Submits an IAAF to its first approval level.
   *
   * @param id IAAF
   * @return the IAAF
   */
  public SbmIaaf submit(Long id) {
    SbmIaaf i = get(id);
    SbmPolicy p = policy(i);
    approvals.submit(DocumentApprovals.IAAF, i, levels(i, p), subject(p));
    audit.record(ENTITY, i.getIaafNo(), AuditAction.SUBMIT, "Submitted for approval");
    return i;
  }

  /**
   * Approves the current level; the last approval renders the signed PDF.
   *
   * @param id IAAF
   * @return the IAAF
   */
  public SbmIaaf approve(Long id) {
    SbmIaaf i = get(id);
    SbmPolicy p = policy(i);
    boolean last = approvals.approve(DocumentApprovals.IAAF, i, levels(i, p), subject(p));
    if (last) {
      Rendered pdf = render(i, p);
      i.signedPdf(pdf.attachmentId());
      i.rendered(pdf.versionNo());
      history.note(
          p, "IAAF", "IAAF " + i.getIaafNo() + " approved", SbmHistorySource.MANUAL, i.getIaafNo());
    }
    audit.record(
        ENTITY, i.getIaafNo(), AuditAction.AUTHORIZE, last ? "Approved" : "Level approved");
    return i;
  }

  /**
   * Returns an IAAF to its preparer.
   *
   * @param id IAAF
   * @param reasonCode reason
   * @param comment comment
   * @return the IAAF
   */
  public SbmIaaf returned(Long id, String reasonCode, String comment) {
    SbmIaaf i = get(id);
    approvals.returned(DocumentApprovals.IAAF, i, reasonCode, comment);
    audit.record(ENTITY, i.getIaafNo(), AuditAction.REJECT, "Returned: " + reasonCode);
    return i;
  }

  /**
   * Sends the approved IAAF to the bank counterpart and issues it (FR-SP-041).
   *
   * @param id IAAF
   * @return the IAAF
   */
  public SbmIaaf issue(Long id) {
    SbmIaaf i = get(id);
    SbmPolicy p = policy(i);
    String to = p.getAssured().bankCounterpartEmail();
    if (to == null || to.isBlank()) {
      throw new BusinessRuleException(
          "SBM_NO_COUNTERPART", "Enter the e-mail of the bank counterpart on the policy first");
    }
    byte[] pdf = i.getAttachmentId() == null ? render(i, p).pdf() : pdfs.read(i.getAttachmentId());
    messages.queueEmail(
        new OutboundEmail(
            p.getCompanyId(),
            "SBM_IAAF",
            List.of(to),
            List.of(),
            "Insurance Adequacy Assessment Form "
                + i.getIaafNo()
                + " - "
                + p.getAssured().assuredName(),
            "Please find attached the Insurance Adequacy Assessment Form of the policy "
                + nz(p.getTerms().policyNo())
                + " of "
                + p.getAssured().assuredName()
                + ".",
            List.of(new MessageFile(i.getIaafNo() + ".pdf", "application/pdf", pdf)),
            null,
            new RecordLink(SubmittedCodes.ENTITY, p.getId().toString(), p.getSbmNo())));
    i.issued(to, clock.instant());
    approvals.move(DocumentApprovals.IAAF, i, "issue", TransitionNote.NONE);
    history.note(
        p,
        "IAAF",
        "IAAF " + i.getIaafNo() + " sent to the bank counterpart",
        SbmHistorySource.MANUAL,
        i.getIaafNo());
    audit.record(ENTITY, i.getIaafNo(), AuditAction.POST, "Sent to " + to);
    return i;
  }

  /**
   * Cancels a draft or returned IAAF.
   *
   * @param id IAAF
   * @param reasonCode reason
   * @return the IAAF
   */
  public SbmIaaf cancel(Long id, String reasonCode) {
    SbmIaaf i = get(id);
    i.cancel();
    approvals.move(DocumentApprovals.IAAF, i, "cancel", new TransitionNote(reasonCode, null));
    audit.record(ENTITY, i.getIaafNo(), AuditAction.DEACTIVATE, "Cancelled");
    return i;
  }

  /**
   * An IAAF.
   *
   * @param id IAAF
   * @return IAAF
   */
  @Transactional(readOnly = true)
  public SbmIaaf get(Long id) {
    SbmIaaf i = iaafs.findById(id).orElseThrow(() -> new ResourceNotFoundException("IAAF", id));
    masterlist.get(i.getPolicyId());
    return i;
  }

  /**
   * The levels of an IAAF.
   *
   * @param i IAAF
   * @param p its policy
   * @return levels
   */
  @Transactional(readOnly = true)
  public List<SbmApprovalMatrix> levels(SbmIaaf i, SbmPolicy p) {
    return approvals.levels(
        DocumentApprovals.IAAF, i.getCompanyId(), p.getSegment(), p.getTerms().sumInsured());
  }

  private Rendered render(SbmIaaf i, SbmPolicy p) {
    List<SbmIaafReview> done = reviews.findByPolicyIdOrderByReviewNoAsc(p.getId());
    Map<String, Object> v = new LinkedHashMap<>();
    v.put("iaafNo", i.getIaafNo());
    v.put("sbmNo", p.getSbmNo());
    v.put("borrowerName", nz(p.getLoan().borrowerName()));
    v.put("assuredName", p.getAssured().assuredName());
    v.put("policyNo", nz(p.getTerms().policyNo()));
    v.put("insurerName", nz(p.getTerms().insurerCode()));
    v.put("inceptionDate", DisplayFormat.date(p.getTerms().inceptionDate()));
    v.put("expiryDate", DisplayFormat.date(p.getTerms().expiryDate()));
    v.put("currency", p.getTerms().currency());
    v.put("sumInsured", DisplayFormat.amount(p.getTerms().sumInsured()));
    v.put("reviewCount", done.size());
    v.put("reviews", reviewLines(done));
    v.put(
        "links",
        links.findByIaafIdOrderByIdAsc(i.getId()).stream()
            .map(
                l ->
                    policies.findById(l.getRelatedPolicyId()).map(SbmPolicy::getSbmNo).orElse("")
                        + " ("
                        + DisplayFormat.words(l.getRelation())
                        + ")")
            .collect(Collectors.joining(", ")));
    v.put("adequacy", "Adequate");
    return pdfs.render(
        new SignedPdfs.Request(
            i.getCompanyId(),
            p.getId(),
            "SBM_IAAF",
            "SBM_IAAF",
            i.getIaafNo(),
            v,
            approvals.signaturesOf(DocumentApprovals.IAAF, i.getId())));
  }

  private String reviewLines(List<SbmIaafReview> done) {
    return done.stream()
        .map(
            r ->
                "Review "
                    + r.getReviewNo()
                    + " of "
                    + DisplayFormat.date(r.getReviewDate())
                    + ": "
                    + DisplayFormat.words(r.getAdequacy())
                    + (r.findingList().isEmpty()
                        ? ""
                        : " - "
                            + r.findingList().stream()
                                .map(f -> lovs.label(SubmittedCodes.LOV_FINDING, f))
                                .collect(Collectors.joining(", "))))
        .collect(Collectors.joining("\n"));
  }

  private SbmPolicy policy(SbmIaaf i) {
    return masterlist.require(i.getPolicyId());
  }

  private static String subject(SbmPolicy p) {
    return p.getAssured().assuredName() + " - " + p.getSbmNo();
  }

  private static String nz(String value) {
    return value == null ? "" : value;
  }
}
