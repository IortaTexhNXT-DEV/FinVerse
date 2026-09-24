package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.messaging.service.QueuedEmail;
import com.iortatechnxt.brokerverse.remittance.domain.BatchDocument.StoredFile;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatchRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.DocumentKind;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends the remittance schedule of an approved batch to the insurer (MKTID.001, RMTID.011):
 * password-protected Excel through the messaging outbox, the password in a separate e-mail, once
 * per batch (duplicate-send guard). Insurer channels other than e-mail are parked (OQ22).
 */
@Service
@Transactional
public class ScheduleDispatch {

  private static final Set<BatchStage> SENDABLE =
      Set.of(
          BatchStage.APPROVED,
          BatchStage.PARTIALLY_REMITTED,
          BatchStage.FULLY_REMITTED,
          BatchStage.OR_RECEIVED);

  private final RemittanceBatchRepository batches;
  private final BatchDocuments documents;
  private final MessageService messages;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param batches batches
   * @param documents schedule
   * @param messages outbox
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public ScheduleDispatch(
      RemittanceBatchRepository batches,
      BatchDocuments documents,
      MessageService messages,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.batches = batches;
    this.documents = documents;
    this.messages = messages;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Sends the schedule.
   *
   * @param id batch
   * @param mail recipients, subject and body
   * @return the queued e-mail
   */
  public QueuedEmail send(Long id, Mail mail) {
    RemittanceBatch batch =
        batches
            .findWithLinesById(id)
            .orElseThrow(() -> new ResourceNotFoundException(BatchService.ENTITY, id));
    if (!SENDABLE.contains(batch.getStage())) {
      throw new BusinessRuleException(
          "REMIT_SCHEDULE_NOT_APPROVED",
          "The schedule of " + batch.getBatchNo() + " can be sent once the batch is approved");
    }
    batch.scheduleSent(currentUser.username(), clock.instant());
    StoredFile schedule = documents.document(batch, DocumentKind.SCHEDULE_XLSX);
    QueuedEmail queued =
        messages.queueEmail(
            new OutboundEmail(
                batch.getCompanyId(),
                "REMITTANCE_SCHEDULE",
                mail.to(),
                mail.cc(),
                mail.subject(),
                mail.body(),
                List.of(
                    new MessageFile(schedule.fileName(), BatchDocuments.XLSX, schedule.content())),
                new OutboundEmail.Protection(null, true, null),
                new RecordLink(BatchService.ENTITY, id.toString(), batch.getBatchNo())));
    audit.record(
        BatchService.ENTITY,
        batch.getBatchNo(),
        AuditAction.UPDATE,
        "Schedule sent to " + String.join(", ", mail.to()));
    return queued;
  }

  /**
   * The e-mail.
   *
   * @param to recipients
   * @param cc copy recipients
   * @param subject subject
   * @param body body
   */
  public record Mail(List<String> to, List<String> cc, String subject, String body) {

    /** Defensive copies. */
    public Mail {
      to = List.copyOf(to);
      cc = cc == null ? List.of() : List.copyOf(cc);
    }
  }
}
