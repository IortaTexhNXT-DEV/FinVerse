package com.iortatechnxt.brokerverse.submitted.review.service;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.submitted.domain.SbmApprovable;
import com.iortatechnxt.brokerverse.submitted.domain.SbmApprovalMatrix;
import com.iortatechnxt.brokerverse.submitted.domain.SbmApprovalMatrixRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmDocStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmSignature;
import com.iortatechnxt.brokerverse.submitted.domain.SbmSignatureRepository;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import com.iortatechnxt.brokerverse.submitted.service.port.SignatureProvider;
import com.iortatechnxt.brokerverse.submitted.service.port.SignatureProvider.SignRequest;
import com.iortatechnxt.brokerverse.submitted.service.port.SignatureProvider.Signature;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The approval of an IAAF or a TOR by its matrix (BRIDSP-07, 18; FRS FR-SP-041, 052): the levels of
 * the band of the document's segment and sum insured, the approvers of the current level notified,
 * each level approved by a holder of its permission (or its named approver) who is not the
 * preparer and signed by the {@link SignatureProvider}, the document returned with a reason. The
 * {@code SBM_IAAF} and {@code SBM_TOR} work cases keep the history.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class DocumentApprovals {

  /** IAAF document. */
  public static final String IAAF = "IAAF";

  /** TOR document. */
  public static final String TOR = "TOR";

  private final SbmApprovalMatrixRepository matrix;
  private final SbmSignatureRepository signatures;
  private final SignatureProvider signer;
  private final WorkflowService workflow;
  private final NotificationService notifications;
  private final UserDirectory users;
  private final LovService lovs;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the helper.
   *
   * @param matrix approval matrices
   * @param signatures signatures
   * @param signer signature provider
   * @param workflow workflow engine
   * @param notifications notifications
   * @param users user directory
   * @param lovs lists of values (segment labels)
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the approval
  public DocumentApprovals(
      SbmApprovalMatrixRepository matrix,
      SbmSignatureRepository signatures,
      SignatureProvider signer,
      WorkflowService workflow,
      NotificationService notifications,
      UserDirectory users,
      LovService lovs,
      CurrentUser currentUser,
      Clock clock) {
    this.matrix = matrix;
    this.signatures = signatures;
    this.signer = signer;
    this.workflow = workflow;
    this.notifications = notifications;
    this.users = users;
    this.lovs = lovs;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Opens the work case of a new document.
   *
   * @param doc IAAF or TOR
   * @param d document (saved)
   * @param title title of the work case
   * @param link frontend route
   */
  public void start(String doc, SbmApprovable d, String title, String link) {
    workflow.start(
        new StartCase(
            d.getCompanyId(),
            workflowOf(doc),
            new CaseRecord(entityOf(doc), d.getId().toString(), d.number(), title, link, null),
            null));
  }

  /**
   * The levels of a document, one row per level.
   *
   * @param doc IAAF or TOR
   * @param companyId company
   * @param segment segment
   * @param sumInsured sum insured, may be null
   * @return levels in order
   */
  public List<SbmApprovalMatrix> levels(
      String doc, Long companyId, String segment, BigDecimal sumInsured) {
    BigDecimal tsi = sumInsured == null ? BigDecimal.ZERO : sumInsured;
    Map<Integer, SbmApprovalMatrix> byLevel = new TreeMap<>();
    for (SbmApprovalMatrix m :
        matrix.findByCompanyIdAndDocumentAndRecordStatusOrderByLevelAsc(
            companyId, doc, RecordStatus.ACTIVE)) {
      if (m.applies(doc, segment, tsi)) {
        byLevel.merge(m.getLevel(), m, (a, b) -> a.getSegment() != null ? a : b);
      }
    }
    if (byLevel.isEmpty()) {
      throw new BusinessRuleException(
          "SBM_NO_APPROVAL_LEVEL",
          "No approval level is defined for segment "
              + lovs.label(SubmittedCodes.LOV_SEGMENT, segment)
              + " and sum insured "
              + DisplayFormat.amount(tsi));
    }
    return List.copyOf(byLevel.values());
  }

  /**
   * Submits a document to its first level and notifies its approvers.
   *
   * @param doc IAAF or TOR
   * @param d document
   * @param levels levels of the document
   * @param subject what is approved (for the notice)
   */
  public void submit(String doc, SbmApprovable d, List<SbmApprovalMatrix> levels, String subject) {
    d.submit(levels.size(), clock.instant());
    move(doc, d, "submit", TransitionNote.NONE);
    notifyLevel(doc, d, levels.get(0), subject);
  }

  /**
   * Approves the current level of a document by the current user and signs it.
   *
   * @param doc IAAF or TOR
   * @param d document
   * @param levels levels of the document
   * @param subject what is approved (for the notice)
   * @return true when the last level was approved
   */
  public boolean approve(String doc, SbmApprovable d, List<SbmApprovalMatrix> levels, String subject) {
    d.require(SbmDocStatus.FOR_APPROVAL);
    String me = currentUser.username();
    if (CurrentUser.sameUser(me, d.getCreatedBy())) {
      throw new BusinessRuleException(
          "SBM_APPROVER_IS_PREPARER",
          "An " + label(doc) + " is approved by someone other than its preparer");
    }
    SbmApprovalMatrix level = levelOf(levels, d.getCurrentLevel());
    if (!mayApprove(level, me)) {
      throw new BusinessRuleException(
          "SBM_NOT_APPROVER",
          "You are not an approver of level " + d.getCurrentLevel() + " of " + d.number());
    }
    String name = users.displayName(me);
    Signature s =
        signer.sign(
            new SignRequest(
                doc,
                d.number(),
                d.getCurrentLevel(),
                me,
                name,
                level.getSignatoryTitle(),
                Sha256.hex(d.number() + "|" + d.getSubmittedAt())));
    signatures.save(
        new SbmSignature(
            new SbmSignature.Document(doc, d.getId()),
            d.getCurrentLevel(),
            new SbmSignature.Signer(me, name, level.getSignatoryTitle()),
            new SbmSignature.Stamp(s.signedAt(), s.method(), s.hash())));
    boolean last = d.approveLevel();
    move(doc, d, last ? "approve" : "approve_level", TransitionNote.NONE);
    if (!last) {
      notifyLevel(doc, d, levelOf(levels, d.getCurrentLevel()), subject);
    }
    return last;
  }

  /**
   * Returns a document to its preparer.
   *
   * @param doc IAAF or TOR
   * @param d document
   * @param reasonCode reason (LOV SBM_RETURN_REASON)
   * @param comment comment
   */
  public void returned(String doc, SbmApprovable d, String reasonCode, String comment) {
    if (reasonCode == null || reasonCode.isBlank()) {
      throw new BusinessRuleException("WORKFLOW_REASON_REQUIRED", "Select a reason for 'Return'");
    }
    move(doc, d, "return", new TransitionNote(reasonCode, comment));
    d.returned(lovs.label("SBM_RETURN_REASON", reasonCode) + (comment == null ? "" : ": " + comment));
    notifications.notifyUser(
        d.getCreatedBy(),
        new Notice(
            d.number() + " returned",
            d.getReturnReason(),
            linkOf(doc, d),
            entityOf(doc),
            d.getId().toString()));
  }

  /**
   * Moves the work case (system action: the services check the permissions).
   *
   * @param doc IAAF or TOR
   * @param d document
   * @param action action
   * @param note reason and comment
   */
  public void move(String doc, SbmApprovable d, String action, TransitionNote note) {
    workflow.systemTransition(entityOf(doc), d.getId().toString(), action, note);
  }

  /**
   * Whether a user may approve the current level of a document (approval inbox).
   *
   * @param d document
   * @param levels levels of the document
   * @param username user
   * @param authorities user's permissions
   * @return true when the user holds the level's permission (or is its named approver)
   */
  public static boolean mayApprove(
      SbmApprovable d, List<SbmApprovalMatrix> levels, String username, Set<String> authorities) {
    Optional<SbmApprovalMatrix> level =
        levels.stream().filter(l -> l.getLevel() == d.getCurrentLevel()).findFirst();
    return level.isPresent()
        && authorities.contains(level.get().getPermission())
        && (level.get().getApproverUsername() == null
            || CurrentUser.sameUser(level.get().getApproverUsername(), username));
  }

  /**
   * The signatures of a document.
   *
   * @param doc IAAF or TOR
   * @param id document
   * @return signatures by level
   */
  public List<SbmSignature> signaturesOf(String doc, Long id) {
    return signatures.findByDocumentTypeAndDocumentIdOrderByLevelAsc(doc, id);
  }

  private boolean mayApprove(SbmApprovalMatrix level, String me) {
    return currentUser.hasAuthority(level.getPermission())
        && (level.getApproverUsername() == null
            || CurrentUser.sameUser(level.getApproverUsername(), me));
  }

  private void notifyLevel(String doc, SbmApprovable d, SbmApprovalMatrix level, String subject) {
    Notice notice =
        new Notice(
            label(doc) + " " + d.number() + " to approve (level " + level.getLevel() + ")",
            subject,
            linkOf(doc, d),
            entityOf(doc),
            d.getId().toString());
    String event = IAAF.equals(doc) ? "SBM_IAAF_PENDING" : "SBM_TOR_PENDING";
    if (level.getApproverUsername() != null) {
      notifications.notifyUser(level.getApproverUsername(), notice, event);
    } else {
      notifications.notifyPermission(level.getPermission(), notice, event);
    }
  }

  private static SbmApprovalMatrix levelOf(List<SbmApprovalMatrix> levels, int level) {
    return levels.stream()
        .filter(l -> l.getLevel() == level)
        .findFirst()
        .orElse(levels.get(Math.min(level, levels.size()) - 1));
  }

  /**
   * The frontend route of a document.
   *
   * @param doc IAAF or TOR
   * @param d document
   * @return route
   */
  public static String linkOf(String doc, SbmApprovable d) {
    return (IAAF.equals(doc) ? "/submitted/iaaf/" : "/submitted/tor/") + d.getId();
  }

  /**
   * The entity type of a document.
   *
   * @param doc IAAF or TOR
   * @return entity type
   */
  public static String entityOf(String doc) {
    return IAAF.equals(doc) ? "SubmittedIaaf" : "SubmittedTor";
  }

  private static String workflowOf(String doc) {
    return IAAF.equals(doc) ? "SBM_IAAF" : "SBM_TOR";
  }

  private static String label(String doc) {
    return IAAF.equals(doc) ? "IAAF" : "TOR";
  }
}
