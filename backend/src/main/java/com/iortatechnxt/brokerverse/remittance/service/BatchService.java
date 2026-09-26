package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.InvoiceTag;
import com.iortatechnxt.brokerverse.remittance.domain.InvoiceTagRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch.Exclusion;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatchRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTag;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkAssignmentService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowViewService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Process Remittance of a batch (RMTID.002/009-011/019/029, OPS_REMITTANCE): exclude and restore
 * lines with a reason (addendum: amounts are never edited), preview, submit for approval, approve
 * with four eyes (posting and push to Disbursement by {@link BatchPosting}), return with a reason
 * (lines re-tagged RETURNED and extractable again) and re-assign the processor. Every action is
 * audited, notified (RMTID.034) and recorded in the batch's workflow history (RMTID.036).
 */
@Service
@Transactional
public class BatchService {

  /** Audit entity of batches. */
  public static final String ENTITY = "RemittanceBatch";

  private static final String EXCLUSION_LOV = "REMIT_EXCLUSION_REASON";
  private static final String BATCH = "Batch ";
  private static final String DECIDED = "REMIT_BATCH_DECIDED";

  private final RemittanceBatchRepository batches;
  private final InvoiceTagRepository tags;
  private final BatchLedger batchLedger;
  private final BatchPosting posting;
  private final BatchDocuments documents;
  private final SpecialRemittanceService specials;
  private final WorkflowService workflow;
  private final WorkflowViewService workflowViews;
  private final WorkAssignmentService assignments;
  private final UserDirectory users;
  private final LovService lovs;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param batches batches
   * @param tags extraction tags
   * @param batchLedger ledger side of lines
   * @param posting approval posting
   * @param documents schedule and payment request
   * @param specials special remittance requests
   * @param workflow Process Remittance workflow
   * @param workflowViews workflow cases
   * @param assignments work assignment
   * @param users users by permission
   * @param lovs reasons
   * @param notifications notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public BatchService(
      RemittanceBatchRepository batches,
      InvoiceTagRepository tags,
      BatchLedger batchLedger,
      BatchPosting posting,
      BatchDocuments documents,
      SpecialRemittanceService specials,
      WorkflowService workflow,
      WorkflowViewService workflowViews,
      WorkAssignmentService assignments,
      UserDirectory users,
      LovService lovs,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.batches = batches;
    this.tags = tags;
    this.batchLedger = batchLedger;
    this.posting = posting;
    this.documents = documents;
    this.specials = specials;
    this.workflow = workflow;
    this.workflowViews = workflowViews;
    this.assignments = assignments;
    this.users = users;
    this.lovs = lovs;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * A batch with its lines.
   *
   * @param id batch
   * @return batch
   */
  @Transactional(readOnly = true)
  public RemittanceBatch get(Long id) {
    return batches
        .findWithLinesById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Excludes invoices from a batch with a reason (RMTID.002 addendum, REMIT_EXCLUDE): the amounts
   * stay as extracted, the invoices return to their previous remittance status and are unlocked.
   *
   * @param id batch
   * @param invoiceNos invoices
   * @param reason reason code (LOV REMIT_EXCLUSION_REASON)
   * @param comment comment, may be null
   * @return the batch
   */
  public RemittanceBatch exclude(Long id, List<String> invoiceNos, String reason, String comment) {
    lovs.requireValid(EXCLUSION_LOV, reason, LocalDate.now(clock));
    RemittanceBatch batch = get(id);
    Exclusion exclusion = new Exclusion(reason, comment, currentUser.username(), clock.instant());
    for (String invoiceNo : invoiceNos) {
      BatchLine line = batch.exclude(invoiceNo, exclusion);
      batchLedger.release(line, "Excluded from " + batch.getBatchNo() + " (" + reason + ")");
      audit.record(
          ENTITY,
          batch.getBatchNo(),
          AuditAction.UPDATE,
          "Excluded " + invoiceNo + ": " + reason + (comment == null ? "" : " - " + comment));
    }
    return batch;
  }

  /**
   * Restores an excluded invoice before submission (RMTID.002 addendum) when it is still free to
   * remit: not taken by another batch, hold, adjustment or write-off since.
   *
   * @param id batch
   * @param invoiceNo invoice
   * @return the batch
   */
  public RemittanceBatch restore(Long id, String invoiceNo) {
    RemittanceBatch batch = get(id);
    batchLedger.requireFree(invoiceNo);
    batch.restore(invoiceNo, currentUser.username(), clock.instant());
    batchLedger.take(invoiceNo, batch.getBatchNo());
    audit.record(ENTITY, batch.getBatchNo(), AuditAction.UPDATE, "Restored " + invoiceNo);
    return batch;
  }

  /**
   * What would be submitted (RMTID.002 preview): the lines kept, the totals and the problems that
   * block the submission (RMTID.019).
   *
   * @param id batch
   * @return preview
   */
  @Transactional(readOnly = true)
  public Preview preview(Long id) {
    RemittanceBatch batch = get(id);
    return new Preview(batch, batch.included(), batchLedger.problems(batch.included()));
  }

  /**
   * Submits the batch for approval (RMTID.010, no double submission) and stores the schedule and
   * payment request (RMTID.011).
   *
   * @param id batch
   * @param comment comment, may be null
   * @return the batch
   */
  public RemittanceBatch submit(Long id, String comment) {
    RemittanceBatch batch = get(id);
    requireValid(batch);
    batch.submitted(currentUser.username(), clock.instant());
    workflow.transition(ENTITY, id.toString(), "submit", TransitionNote.comment(comment));
    documents.store(batch);
    audit.record(ENTITY, batch.getBatchNo(), AuditAction.SUBMIT, summary(batch));
    notifications.notifyPermission(
        "REMIT_APPROVE",
        notice(batch, BATCH + batch.getBatchNo() + " for approval"),
        "REMIT_BATCH_FOR_APPROVAL");
    return batch;
  }

  /**
   * Approves the batch (REMIT_APPROVE, four eyes): posts it, pushes it to Disbursement and issues
   * the commission and incentive ORs (RMTID.010/011/023).
   *
   * @param id batch
   * @param comment comment, may be null
   * @return the batch
   */
  public RemittanceBatch approve(Long id, String comment) {
    RemittanceBatch batch = get(id);
    batch.approved(currentUser.username(), clock.instant());
    requireValid(batch);
    workflow.transition(ENTITY, id.toString(), "approve", TransitionNote.comment(comment));
    posting.post(batch);
    documents.store(batch);
    if (batch.getSpecialRequestNo() != null && batch.getSettlement().getSendCycle() == 1) {
      specials.batchApproved(batch.getSpecialRequestNo());
    }
    audit.record(
        ENTITY,
        batch.getBatchNo(),
        AuditAction.AUTHORIZE,
        "Approved and sent to Disbursement as " + batch.getDisbursementRequestNo());
    notifyProcessor(batch, BATCH + batch.getBatchNo() + " approved");
    return batch;
  }

  /**
   * Returns the batch (RMTID.029): its invoices are given back and re-tagged RETURNED, so the next
   * extraction takes them again.
   *
   * @param id batch
   * @param reasonCode reason (LOV REMIT_RETURN_REASON)
   * @param comment comment
   * @return the batch
   */
  public RemittanceBatch returnBatch(Long id, String reasonCode, String comment) {
    RemittanceBatch batch = get(id);
    batch.returned(reasonCode + (comment == null ? "" : " - " + comment));
    workflow.transition(ENTITY, id.toString(), "return", new TransitionNote(reasonCode, comment));
    for (BatchLine line : batch.included()) {
      batchLedger.release(line, "Returned with " + batch.getBatchNo());
      tags.save(
          new InvoiceTag(
              null,
              new InvoiceTag.Facts(
                  batch.getCompanyId(),
                  line.getInvoiceNo(),
                  batch.getInsurerCode(),
                  batch.getRemittanceType()),
              new InvoiceTag.Outcome(ExtractionTag.RETURNED, null, reasonCode),
              new InvoiceTag.Money(
                  line.getAmounts().paidAr(),
                  line.getAmounts().dtip(),
                  line.getAmounts().paidAr())));
    }
    if (batch.getSpecialRequestNo() != null && batch.getSettlement().getSendCycle() == 1) {
      specials.batchReturned(batch.getSpecialRequestNo());
    }
    audit.record(ENTITY, batch.getBatchNo(), AuditAction.REJECT, "Returned: " + reasonCode);
    notifyProcessor(batch, BATCH + batch.getBatchNo() + " returned");
    return batch;
  }

  /**
   * Re-assigns the batch to another processor (RMTID.009).
   *
   * @param id batch
   * @param username processor holding REMIT_PROCESS
   * @return the batch
   */
  public RemittanceBatch assign(Long id, String username) {
    RemittanceBatch batch = get(id);
    List<String> processors = users.usersWithPermission(ExtractionService.PROCESSORS);
    workflowViews
        .view(ENTITY, id.toString())
        .ifPresent(v -> assignments.assign(v.workCase().getId(), username, processors));
    batch.assignProcessor(username);
    audit.record(ENTITY, batch.getBatchNo(), AuditAction.UPDATE, "Assigned to " + username);
    return batch;
  }

  private void requireValid(RemittanceBatch batch) {
    List<String> problems = batchLedger.problems(batch.included());
    if (!problems.isEmpty()) {
      throw new BusinessRuleException(
          "REMIT_SUBMISSION_INVALID",
          BATCH + batch.getBatchNo() + " cannot go on: " + String.join("; ", problems));
    }
  }

  private void notifyProcessor(RemittanceBatch batch, String title) {
    String to = batch.getSubmittedBy() != null ? batch.getSubmittedBy() : batch.getProcessor();
    if (to != null) {
      notifications.notifyUser(to, notice(batch, title), DECIDED);
    }
  }

  private static Notice notice(RemittanceBatch batch, String title) {
    return new Notice(
        title,
        summary(batch),
        "/remittance/batches/" + batch.getId(),
        ENTITY,
        batch.getId().toString());
  }

  private static String summary(RemittanceBatch batch) {
    return batch.getInsurerCode()
        + " "
        + BatchDocuments.typeLabel(batch.getRemittanceType())
        + ": "
        + batch.getLineCount()
        + " account(s), net due "
        + batch.getCurrency()
        + " "
        + batch.amountDue();
  }

  /**
   * A submission preview.
   *
   * @param batch batch
   * @param lines lines that would be remitted
   * @param problems problems blocking the submission
   */
  public record Preview(RemittanceBatch batch, List<BatchLine> lines, List<String> problems) {

    /** Defensive copies. */
    public Preview {
      lines = List.copyOf(lines);
      problems = List.copyOf(problems);
    }
  }
}
