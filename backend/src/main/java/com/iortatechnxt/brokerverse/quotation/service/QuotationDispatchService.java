package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail.Protection;
import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationStatus;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends approved quotations to the client (BRNB.013/014/042/043): the PDF and Excel of the current
 * version, password protected, with the password in a separate e-mail, through the messaging outbox
 * (send log per quotation). The send action is refused until the quotation is approved. Batch send
 * groups the selected quotations into one e-mail per client.
 */
@Service
@Transactional
public class QuotationDispatchService {

  /** Purpose code of the quotation e-mails in the outbox. */
  public static final String PURPOSE = "QUOTATION";

  private final QuotationService service;
  private final QuotationDocuments documents;
  private final MessageService messages;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param service quotation reads
   * @param documents PDF and Excel builder
   * @param messages outbox
   * @param workflow workflow engine
   * @param audit audit trail
   * @param clock clock
   */
  public QuotationDispatchService(
      QuotationService service,
      QuotationDocuments documents,
      MessageService messages,
      WorkflowService workflow,
      AuditTrailService audit,
      Clock clock) {
    this.service = service;
    this.documents = documents;
    this.messages = messages;
    this.workflow = workflow;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Sends one approved quotation.
   *
   * @param id quotation
   * @param email recipients, subject, message and password hint
   * @return the quotation, now SENT_TO_CLIENT
   */
  public Quotation send(Long id, EmailRequest email) {
    Quotation q = service.get(id);
    requireApproved(q);
    messages.queueEmail(
        new OutboundEmail(
            q.getCompanyId(),
            PURPOSE,
            email.to(),
            email.cc(),
            email.subject(),
            email.body(),
            documents.files(q),
            new Protection(null, true, email.passwordHint()),
            link(q)));
    markSent(q, "Sent to " + String.join(", ", email.to()));
    return q;
  }

  /**
   * Sends several approved quotations: one e-mail per client with all its quotations attached, the
   * password in a separate e-mail (BRNB.042).
   *
   * @param ids quotations
   * @param message optional subject and message; defaults are used when blank
   * @return what was sent
   */
  public BatchResult sendBatch(List<Long> ids, EmailRequest message) {
    if (ids == null || ids.isEmpty()) {
      throw new BusinessRuleException("QUOTATION_BATCH_EMPTY", "Select the quotations to send");
    }
    List<Quotation> selected = ids.stream().distinct().map(service::get).toList();
    requireSendable(selected);
    Map<Long, List<Quotation>> byClient =
        selected.stream()
            .collect(
                Collectors.groupingBy(
                    Quotation::getClientId, LinkedHashMap::new, Collectors.toList()));
    byClient.values().forEach(group -> sendGroup(group, message));
    return new BatchResult(
        selected.size(),
        byClient.size(),
        selected.stream().map(Quotation::getQuotationNo).toList());
  }

  private static void requireSendable(List<Quotation> selected) {
    List<String> problems = new ArrayList<>();
    for (Quotation q : selected) {
      if (q.getStatus() != QuotationStatus.APPROVED) {
        problems.add(q.getQuotationNo() + " is " + q.getStatus());
      } else if (blank(q.getClientEmail())) {
        problems.add(q.getQuotationNo() + ": client " + q.getClientCode() + " has no e-mail");
      }
    }
    if (!problems.isEmpty()) {
      throw new BusinessRuleException(
          "QUOTATION_BATCH_INVALID", "Cannot send: " + String.join("; ", problems));
    }
  }

  private void sendGroup(List<Quotation> group, EmailRequest message) {
    Quotation first = group.get(0);
    List<MessageFile> files = new ArrayList<>();
    group.forEach(q -> files.addAll(documents.files(q)));
    String refs = group.stream().map(Quotation::getQuotationNo).collect(Collectors.joining(", "));
    String subject =
        blank(message == null ? null : message.subject())
            ? "Your insurance quotation(s) " + refs
            : message.subject();
    String body =
        blank(message == null ? null : message.body())
            ? "Dear "
                + first.getClientName()
                + ",\n\nPlease find attached our quotation(s) "
                + refs
                + ". The documents are password protected; the password follows in a separate"
                + " e-mail.\n\nBDO Insurance and Reinsurance Brokers, Inc."
            : message.body();
    messages.queueEmail(
        new OutboundEmail(
            first.getCompanyId(),
            PURPOSE,
            List.of(first.getClientEmail()),
            List.of(),
            subject,
            body,
            files,
            new Protection(null, true, message == null ? null : message.passwordHint()),
            link(first)));
    group.forEach(q -> markSent(q, "Sent in batch to " + q.getClientEmail() + " with " + refs));
  }

  private void markSent(Quotation q, String comment) {
    workflow.transition(
        QuotationService.ENTITY,
        String.valueOf(q.getId()),
        "send",
        TransitionNote.comment(comment));
    q.markSent(clock.instant());
    audit.record(QuotationService.ENTITY, q.getQuotationNo(), AuditAction.UPDATE, comment);
  }

  private static void requireApproved(Quotation q) {
    if (q.getStatus() != QuotationStatus.APPROVED) {
      throw new BusinessRuleException(
          "QUOTATION_NOT_APPROVED",
          "Quotation " + q.getQuotationNo() + " must be approved before it is sent");
    }
  }

  private static RecordLink link(Quotation q) {
    return new RecordLink(QuotationService.ENTITY, String.valueOf(q.getId()), q.getQuotationNo());
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  /**
   * An e-mail to the client.
   *
   * @param to recipients
   * @param cc copy recipients
   * @param subject subject
   * @param body message
   * @param passwordHint how the password is built, may be null
   */
  public record EmailRequest(
      List<String> to, List<String> cc, String subject, String body, String passwordHint) {

    /** Defensive copies. */
    public EmailRequest {
      to = to == null ? List.of() : List.copyOf(to);
      cc = cc == null ? List.of() : List.copyOf(cc);
    }
  }

  /**
   * Outcome of a batch send.
   *
   * @param quotations quotations sent
   * @param emails e-mails queued (one per client, plus the password e-mails)
   * @param references quotation numbers
   */
  public record BatchResult(int quotations, int emails, List<String> references) {

    /** Defensive copy. */
    public BatchResult {
      references = List.copyOf(references);
    }
  }
}
