package com.iortatechnxt.brokerverse.submitted.review.service;

import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import com.iortatechnxt.brokerverse.submitted.domain.SbmApprovalMatrix;
import com.iortatechnxt.brokerverse.submitted.domain.SbmDocStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHistorySource;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLimitCheck;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLimitCheckRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTor;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTorRepository;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService;
import com.iortatechnxt.brokerverse.submitted.review.service.SignedPdfs.Rendered;
import com.iortatechnxt.brokerverse.submitted.service.SbmHistoryService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Terms of Reference (BRIDSP-17-19; FRS FR-SP-051-053): generated for a record flagged "Insurer
 * approval required" with its breached limits and the proposed terms, approved by the TSU matrix,
 * signed and attached as a PDF of type TOR at the last approval, and released when the assigned
 * Account Officer opens or downloads it (the default meaning of Released, SP SQ08).
 */
@Service("sbmTorService")
@Transactional
public class TorService {

  private static final String ENTITY = "SubmittedTor";

  private final SbmTorRepository tors;
  private final SbmLimitCheckRepository checks;
  private final MasterlistService masterlist;
  private final DocumentApprovals approvals;
  private final SignedPdfs pdfs;
  private final AttachmentService attachments;
  private final NotificationService notifications;
  private final SbmHistoryService history;
  private final UserDirectory users;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param tors TORs
   * @param checks limit checks
   * @param masterlist masterlist
   * @param approvals matrix approvals
   * @param pdfs signed PDFs
   * @param attachments attachments (download)
   * @param notifications notifications
   * @param history record history
   * @param users user directory
   * @param numbers document numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the TOR
  public TorService(
      SbmTorRepository tors,
      SbmLimitCheckRepository checks,
      MasterlistService masterlist,
      DocumentApprovals approvals,
      SignedPdfs pdfs,
      AttachmentService attachments,
      NotificationService notifications,
      SbmHistoryService history,
      UserDirectory users,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.tors = tors;
    this.checks = checks;
    this.masterlist = masterlist;
    this.approvals = approvals;
    this.pdfs = pdfs;
    this.attachments = attachments;
    this.notifications = notifications;
    this.history = history;
    this.users = users;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The breached limits of a record, one per line.
   *
   * @param policyId record
   * @return breaches, empty when none
   */
  @Transactional(readOnly = true)
  public String breaches(Long policyId) {
    List<SbmLimitCheck> all = checks.findByPolicyIdOrderByIdDesc(policyId);
    Long lastRun = all.isEmpty() ? null : all.get(0).getRunId();
    return all.stream()
        .filter(c -> c.isBreached() && Objects.equals(c.getRunId(), lastRun))
        .map(
            c ->
                c.getAttribute()
                    + ": "
                    + c.getActualValue()
                    + " above the limit "
                    + c.getLimitValue())
        .collect(Collectors.joining("\n"));
  }

  /**
   * Generates a TOR (FR-SP-051).
   *
   * @param policyId flagged record
   * @param proposedTerms proposed terms
   * @param aoUsername Account Officer
   * @return the TOR (draft)
   */
  public SbmTor generate(Long policyId, String proposedTerms, String aoUsername) {
    SbmPolicy p = masterlist.get(policyId);
    String breached = breaches(policyId);
    if (!p.isInsurerApprovalRequired() || breached.isBlank()) {
      throw new BusinessRuleException(
          "SBM_NO_BREACH", "Policy " + p.getSbmNo() + " exceeds no limit");
    }
    requireContent(proposedTerms, aoUsername);
    SbmTor t =
        tors.save(
            new SbmTor(
                p.getCompanyId(),
                numbers.next("TOR-" + BusinessClock.today(clock).getYear()),
                new SbmTor.Account(p.getId(), p.getRenewalArn()),
                new SbmTor.Content(breached, proposedTerms.strip(), aoUsername)));
    approvals.start(
        DocumentApprovals.TOR,
        t,
        p.getAssured().assuredName(),
        DocumentApprovals.linkOf(DocumentApprovals.TOR, t));
    history.note(
        p, "TOR", "TOR " + t.getTorNo() + " generated", SbmHistorySource.MANUAL, t.getTorNo());
    audit.record(ENTITY, t.getTorNo(), AuditAction.CREATE, "TOR of " + p.getSbmNo());
    return t;
  }

  /**
   * Changes the proposed terms or the Account Officer of a draft or returned TOR.
   *
   * @param id TOR
   * @param proposedTerms proposed terms
   * @param aoUsername Account Officer
   * @return the TOR
   */
  public SbmTor change(Long id, String proposedTerms, String aoUsername) {
    SbmTor t = get(id);
    t.require(SbmDocStatus.DRAFT, SbmDocStatus.RETURNED);
    requireContent(proposedTerms, aoUsername);
    t.change(new SbmTor.Content(t.getBreaches(), proposedTerms.strip(), aoUsername));
    return t;
  }

  private void requireContent(String proposedTerms, String aoUsername) {
    if (proposedTerms == null || proposedTerms.isBlank()) {
      throw new BusinessRuleException("SBM_TERMS_REQUIRED", "Enter the proposed terms");
    }
    if (aoUsername == null || !users.usersWithPermission("SBM_VIEW").contains(aoUsername)) {
      throw new BusinessRuleException(
          "SBM_AO_NOT_ACTIVE", "Select an active Account Officer of Submitted Policies");
    }
  }

  /**
   * Submits a TOR to the TSU matrix.
   *
   * @param id TOR
   * @return the TOR
   */
  public SbmTor submit(Long id) {
    SbmTor t = get(id);
    SbmPolicy p = policy(t);
    approvals.submit(DocumentApprovals.TOR, t, levels(t, p), subject(p));
    audit.record(ENTITY, t.getTorNo(), AuditAction.SUBMIT, "Submitted for approval");
    return t;
  }

  /**
   * Approves the current level; the last approval attaches the signed PDF and notifies the AO.
   *
   * @param id TOR
   * @return the TOR
   */
  public SbmTor approve(Long id) {
    SbmTor t = get(id);
    SbmPolicy p = policy(t);
    boolean last = approvals.approve(DocumentApprovals.TOR, t, levels(t, p), subject(p));
    if (last) {
      Rendered pdf = render(t, p);
      t.signedPdf(pdf.attachmentId());
      t.approvedAt(clock.instant());
      notifications.notifyUser(
          t.getAoUsername(),
          new Notice(
              "TOR " + t.getTorNo() + " approved",
              p.getAssured().assuredName() + ": open or download the Terms of Reference",
              DocumentApprovals.linkOf(DocumentApprovals.TOR, t),
              ENTITY,
              t.getId().toString()),
          "SBM_TOR_RELEASED");
      history.note(
          p, "TOR", "TOR " + t.getTorNo() + " approved", SbmHistorySource.MANUAL, t.getTorNo());
    }
    audit.record(ENTITY, t.getTorNo(), AuditAction.AUTHORIZE, last ? "Approved" : "Level approved");
    return t;
  }

  /**
   * Returns a TOR to its preparer.
   *
   * @param id TOR
   * @param reasonCode reason
   * @param comment comment
   * @return the TOR
   */
  public SbmTor returned(Long id, String reasonCode, String comment) {
    SbmTor t = get(id);
    approvals.returned(DocumentApprovals.TOR, t, reasonCode, comment);
    audit.record(ENTITY, t.getTorNo(), AuditAction.REJECT, "Returned: " + reasonCode);
    return t;
  }

  /**
   * Cancels a draft or returned TOR.
   *
   * @param id TOR
   * @param reasonCode reason
   * @return the TOR
   */
  public SbmTor cancel(Long id, String reasonCode) {
    SbmTor t = get(id);
    t.cancel();
    approvals.move(DocumentApprovals.TOR, t, "cancel", new TransitionNote(reasonCode, null));
    return t;
  }

  /**
   * The signed PDF of an approved TOR; the assigned Account Officer's first opening releases it.
   *
   * @param id TOR
   * @return the download
   */
  public FileDownload download(Long id) {
    SbmTor t = get(id);
    if (t.getAttachmentId() == null) {
      throw new BusinessRuleException("SBM_TOR_NOT_APPROVED", "The TOR is not approved yet");
    }
    if (t.getStatus() == SbmDocStatus.APPROVED
        && CurrentUser.sameUser(currentUser.username(), t.getAoUsername())) {
      t.released(clock.instant());
      approvals.move(DocumentApprovals.TOR, t, "release", TransitionNote.NONE);
      audit.record(ENTITY, t.getTorNo(), AuditAction.UPDATE, "Released to the Account Officer");
    }
    audit.record(ENTITY, t.getTorNo(), AuditAction.EXPORT, "Downloaded");
    return attachments.downloadable(t.getAttachmentId());
  }

  /**
   * A TOR.
   *
   * @param id TOR
   * @return TOR
   */
  @Transactional(readOnly = true)
  public SbmTor get(Long id) {
    SbmTor t = tors.findById(id).orElseThrow(() -> new ResourceNotFoundException("TOR", id));
    if (t.getPolicyId() != null) {
      masterlist.get(t.getPolicyId());
    }
    return t;
  }

  /**
   * The levels of a TOR.
   *
   * @param t TOR
   * @param p its policy
   * @return levels
   */
  @Transactional(readOnly = true)
  public List<SbmApprovalMatrix> levels(SbmTor t, SbmPolicy p) {
    return approvals.levels(
        DocumentApprovals.TOR, t.getCompanyId(), p.getSegment(), p.getTerms().sumInsured());
  }

  private Rendered render(SbmTor t, SbmPolicy p) {
    Map<String, Object> v = new LinkedHashMap<>();
    v.put("torNo", t.getTorNo());
    v.put("accountRef", t.getArn() != null ? t.getArn() : p.getSbmNo());
    v.put("assuredName", p.getAssured().assuredName());
    v.put("insurerName", p.getTerms().insurerCode() == null ? "" : p.getTerms().insurerCode());
    v.put("currency", p.getTerms().currency());
    v.put("sumInsured", DisplayFormat.amount(p.getTerms().sumInsured()));
    v.put("breaches", t.getBreaches());
    v.put("proposedTerms", t.getProposedTerms());
    v.put("aoName", users.displayName(t.getAoUsername()));
    return pdfs.render(
        new SignedPdfs.Request(
            t.getCompanyId(),
            p.getId(),
            "SBM_TOR",
            "SBM_TOR",
            t.getTorNo(),
            v,
            approvals.signaturesOf(DocumentApprovals.TOR, t.getId())));
  }

  private SbmPolicy policy(SbmTor t) {
    return masterlist.require(t.getPolicyId());
  }

  private static String subject(SbmPolicy p) {
    return p.getAssured().assuredName() + " - " + p.getSbmNo();
  }
}
