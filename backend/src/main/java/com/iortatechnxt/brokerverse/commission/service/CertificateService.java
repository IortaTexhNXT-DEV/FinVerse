package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.commission.domain.CertificateSubmission;
import com.iortatechnxt.brokerverse.commission.domain.CertificateSubmission.Certificate;
import com.iortatechnxt.brokerverse.commission.domain.CertificateSubmission.OrLink;
import com.iortatechnxt.brokerverse.commission.domain.CertificateSubmissionRepository;
import com.iortatechnxt.brokerverse.commission.domain.DpItemRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BIR withholding tax certificates of insurers submitted to Comptrollership (CMRID.010/015,
 * workflow {@code OPS_BIR_CERT}): the commission team tags a certificate to the ORs issued for the
 * insurer's commissions (at least one OR) and submits it with its scans (attachments of the
 * submission); Comptrollership acknowledges it or rejects it with a reason; a rejected submission
 * is corrected and resubmitted. Every step is dated and logged. The certificate roles are to be
 * confirmed (OQ41).
 */
@Service
@Transactional
public class CertificateService {

  /** Entity type of the submissions' work cases and attachments. */
  public static final String ENTITY = "BirCertificate";

  private final CertificateSubmissionRepository submissions;
  private final DpItemRepository items;
  private final WorkflowService workflow;
  private final NotificationService notifications;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param submissions submissions
   * @param items DP accounts (ORs collected)
   * @param workflow workflow engine
   * @param notifications notifications
   * @param numbers submission numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public CertificateService(
      CertificateSubmissionRepository submissions,
      DpItemRepository items,
      WorkflowService workflow,
      NotificationService notifications,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.submissions = submissions;
    this.items = items;
    this.workflow = workflow;
    this.notifications = notifications;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Submits a certificate (BIR_CERT_SUBMIT).
   *
   * @param companyId company
   * @param insurerCode insurer that issued it
   * @param certificate facts and ORs
   * @return the submission
   */
  public CertificateSubmission submit(Long companyId, String insurerCode, Certificate certificate) {
    validate(certificate);
    String insurer = insurerCode.strip().toUpperCase(Locale.ROOT);
    CertificateSubmission s =
        submissions.save(
            new CertificateSubmission(
                companyId,
                numbers.next("BCS-" + LocalDate.now(clock).getYear()),
                insurer,
                certificate));
    workflow.start(
        new StartCase(
            companyId,
            CertificateSubmission.WORKFLOW,
            new CaseRecord(
                ENTITY,
                String.valueOf(s.getId()),
                s.getSubmissionNo(),
                "BIR certificate " + certificate.number() + " of " + insurer,
                "/commission/certificates/" + s.getId(),
                null),
            null));
    notifyComptrollership(s, "submitted");
    audit.record(ENTITY, s.getSubmissionNo(), AuditAction.SUBMIT, describe(s));
    return s;
  }

  /**
   * Acknowledges a submission (Comptrollership, BIR_CERT_ACK).
   *
   * @param id submission
   * @param comment comment, may be null
   * @return the submission
   */
  public CertificateSubmission acknowledge(Long id, String comment) {
    CertificateSubmission s = require(id);
    workflow.transition(ENTITY, String.valueOf(id), "acknowledge", TransitionNote.comment(comment));
    s.decided(null, clock.instant(), currentUser.username());
    audit.record(ENTITY, s.getSubmissionNo(), AuditAction.AUTHORIZE, "Acknowledged");
    return s;
  }

  /**
   * Rejects a submission with a reason (Comptrollership, BIR_CERT_ACK).
   *
   * @param id submission
   * @param reason reason
   * @return the submission
   */
  public CertificateSubmission reject(Long id, String reason) {
    CertificateSubmission s = require(id);
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException("CERT_REASON_REQUIRED", "Give the reason of the rejection");
    }
    workflow.transition(ENTITY, String.valueOf(id), "reject", TransitionNote.comment(reason));
    s.decided(reason.strip(), clock.instant(), currentUser.username());
    audit.record(ENTITY, s.getSubmissionNo(), AuditAction.REJECT, "Rejected: " + reason);
    return s;
  }

  /**
   * Corrects and resubmits a rejected submission (BIR_CERT_SUBMIT).
   *
   * @param id submission
   * @param certificate corrected facts and ORs
   * @return the submission
   */
  public CertificateSubmission resubmit(Long id, Certificate certificate) {
    CertificateSubmission s = require(id);
    if (!CertificateSubmission.REJECTED.equals(s.getStage())) {
      throw new BusinessRuleException(
          "CERT_NOT_REJECTED", "Only a rejected submission can be resubmitted");
    }
    validate(certificate);
    s.replace(certificate);
    s.resubmitted();
    workflow.transition(
        ENTITY,
        String.valueOf(id),
        "resubmit",
        TransitionNote.comment("Corrected and resubmitted"));
    notifyComptrollership(s, "resubmitted");
    audit.record(ENTITY, s.getSubmissionNo(), AuditAction.SUBMIT, "Resubmitted: " + describe(s));
    return s;
  }

  private static void validate(Certificate c) {
    if (c.receipts().isEmpty()) {
      throw new BusinessRuleException(
          "CERT_NO_OR", "Tag the certificate to at least one official receipt (CMRID.015)");
    }
    if (c.periodTo().isBefore(c.periodFrom())) {
      throw new BusinessRuleException(
          "CERT_PERIOD", "The certificate period ends before it starts");
    }
    if (c.taxWithheld().signum() <= 0) {
      throw new BusinessRuleException("CERT_AMOUNT", "The tax withheld must be above zero");
    }
  }

  private void notifyComptrollership(CertificateSubmission s, String what) {
    notifications.notifyPermission(
        "BIR_CERT_ACK",
        new Notice(
            "BIR certificate " + what + ": " + s.getSubmissionNo(),
            describe(s),
            "/commission/certificates/" + s.getId(),
            ENTITY,
            String.valueOf(s.getId())));
  }

  private static String describe(CertificateSubmission s) {
    return s.getCertificateForm()
        + " "
        + s.getCertificateNo()
        + " of "
        + s.getInsurerCode()
        + " for "
        + s.getPeriodFrom()
        + " - "
        + s.getPeriodTo()
        + ", tax "
        + s.getTaxWithheld()
        + ", "
        + s.getReceipts().size()
        + " OR(s)";
  }

  /**
   * A submission with its ORs.
   *
   * @param id submission
   * @return submission
   */
  @Transactional(readOnly = true)
  public CertificateSubmission require(Long id) {
    return submissions
        .findWithReceiptsById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Submissions of a company.
   *
   * @param companyId company
   * @param stage stage, null for all
   * @param pageable page
   * @return submissions, newest first
   */
  @Transactional(readOnly = true)
  public Page<CertificateSubmission> search(Long companyId, String stage, Pageable pageable) {
    Page<CertificateSubmission> page = submissions.search(companyId, stage, pageable);
    page.forEach(CertificateSubmission::loadReceipts);
    return page;
  }

  /**
   * ORs of the commission collected from an insurer, to tag certificates to.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @return OR numbers with their amounts
   */
  @Transactional(readOnly = true)
  public List<OrLink> receiptsOf(Long companyId, String insurerCode) {
    return items.receiptsOf(companyId, insurerCode).stream()
        .map(r -> new OrLink((String) r[0], (BigDecimal) r[1]))
        .toList();
  }

  /**
   * Mirrors the work case stage on the submission.
   *
   * @param event transition
   */
  @EventListener
  public void onTransition(WorkCaseTransitioned event) {
    if (ENTITY.equals(event.entityType())) {
      submissions
          .findById(Long.valueOf(event.entityId()))
          .ifPresent(s -> s.mirrorStage(event.toStage()));
    }
  }
}
