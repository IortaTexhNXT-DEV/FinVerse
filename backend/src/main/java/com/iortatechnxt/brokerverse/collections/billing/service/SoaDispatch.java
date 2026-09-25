package com.iortatechnxt.brokerverse.collections.billing.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingDocument;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatement;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.messaging.service.QueuedEmail;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends a statement of account by e-mail through the messaging outbox (BRCLXN.058): the PDF is
 * attached, protected with a generated password sent in a separate e-mail. The recipient (client or
 * bank), the wording and whether statements are e-mailed at all are open with BDOI (CQ18); the
 * collector reviews the recipients and text before sending.
 */
@Service
@Transactional
public class SoaDispatch {

  private static final String PURPOSE = "CLX_SOA";

  private final BillingStatementService statements;
  private final MessageService messages;
  private final ClientService clients;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the dispatcher.
   *
   * @param statements statements
   * @param messages e-mail outbox
   * @param clients client e-mail address
   * @param audit audit trail
   * @param clock clock
   */
  public SoaDispatch(
      BillingStatementService statements,
      MessageService messages,
      ClientService clients,
      AuditTrailService audit,
      Clock clock) {
    this.statements = statements;
    this.messages = messages;
    this.clients = clients;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The client's e-mail address, suggested as the recipient.
   *
   * @param id statement
   * @return address, if the client has one
   */
  @Transactional(readOnly = true)
  public Optional<String> clientEmail(Long id) {
    BillingStatement soa = statements.get(id);
    return Optional.ofNullable(
        clients.requireByCode(soa.getCompanyId(), soa.getClientCode()).getEmail());
  }

  /**
   * E-mails a statement.
   *
   * @param id statement
   * @param mail recipients, subject and body
   * @return the statement, marked sent
   */
  public BillingStatement send(Long id, Mail mail) {
    if (mail.to().isEmpty()) {
      throw new BusinessRuleException("CLX_SOA_RECIPIENT", "Enter at least one recipient");
    }
    BillingStatement soa = statements.get(id);
    BillingDocument pdf = statements.document(id);
    QueuedEmail queued =
        messages.queueEmail(
            new OutboundEmail(
                soa.getCompanyId(),
                PURPOSE,
                mail.to(),
                mail.cc(),
                mail.subject(),
                mail.body(),
                List.of(new MessageFile(pdf.getFileName(), pdf.getContentType(), pdf.getContent())),
                new OutboundEmail.Protection(null, true, mail.passwordHint()),
                new RecordLink(
                    BillingStatementService.ENTITY, String.valueOf(id), soa.getSoaNo())));
    soa.sent(String.join(", ", mail.to()), queued.messageId(), clock.instant());
    audit.record(
        BillingStatementService.ENTITY,
        soa.getSoaNo(),
        AuditAction.UPDATE,
        "Sent to " + String.join(", ", mail.to()));
    return soa;
  }

  /**
   * An e-mail of a statement.
   *
   * @param to recipients
   * @param cc copy recipients
   * @param subject subject
   * @param body body
   * @param passwordHint how the recipient builds the password, may be null
   */
  public record Mail(
      List<String> to, List<String> cc, String subject, String body, String passwordHint) {

    /** Defensive copies. */
    public Mail {
      to = to == null ? List.of() : List.copyOf(to);
      cc = cc == null ? List.of() : List.copyOf(cc);
    }

    /**
     * An e-mail without a password hint.
     *
     * @param to recipients
     * @param cc copy recipients
     * @param subject subject
     * @param body body
     */
    public Mail(List<String> to, List<String> cc, String subject, String body) {
      this(to, cc, subject, body, null);
    }
  }
}
