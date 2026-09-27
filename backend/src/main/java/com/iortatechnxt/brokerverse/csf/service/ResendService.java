package com.iortatechnxt.brokerverse.csf.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService.AttachmentFile;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.util.EmailAddresses;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.csf.domain.ActivityAction;
import com.iortatechnxt.brokerverse.csf.domain.CsfActivity;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.DocumentView;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.ResendPreview;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.ResendResult;
import com.iortatechnxt.brokerverse.issuance.domain.Epolicy;
import com.iortatechnxt.brokerverse.issuance.domain.EpolicyStatus;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyDispatchService;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyDispatchService.DispatchEmail;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyService;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail.Protection;
import com.iortatechnxt.brokerverse.messaging.service.QueuedEmail;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resend of the renewal advice and the e-policy (FR-CSF-030, 031; BRCSF-006 / 6.001, CSF-EM09): to
 * the client's registered e-mail, or with {@code CSF_RESEND_OTHER} and a reason to another address.
 * A renewal advice goes through the e-mail outbox password protected, with the password in a
 * separate e-mail; an e-policy goes through the dispatch service of Issuance, which encrypts it and
 * records the dispatch. Every resend is audited and logged.
 */
@Service
@Transactional
public class ResendService {

  /** Permission to send to another address than the registered e-mail. */
  public static final String RESEND_OTHER = "CSF_RESEND_OTHER";

  private final CsfClients clients;
  private final CsfDocumentService csfDocuments;
  private final DocumentService documents;
  private final MessageService messages;
  private final EpolicyService epolicies;
  private final EpolicyDispatchService dispatch;
  private final CsfSupport support;

  /**
   * Creates the service.
   *
   * @param clients clients of the company
   * @param csfDocuments documents of the client's records
   * @param documents platform documents (checked download)
   * @param messages e-mail outbox
   * @param epolicies e-policies
   * @param dispatch e-policy dispatch
   * @param support platform collaborators
   */
  public ResendService(
      CsfClients clients,
      CsfDocumentService csfDocuments,
      DocumentService documents,
      MessageService messages,
      EpolicyService epolicies,
      EpolicyDispatchService dispatch,
      CsfSupport support) {
    this.clients = clients;
    this.csfDocuments = csfDocuments;
    this.documents = documents;
    this.messages = messages;
    this.epolicies = epolicies;
    this.dispatch = dispatch;
    this.support = support;
  }

  /**
   * What a resend will send: the document, the registered e-mail and the text.
   *
   * @param companyId company
   * @param clientId client
   * @param kind RA or EPOLICY
   * @param documentId attachment (RA) or e-policy id
   * @return preview
   */
  @Transactional(readOnly = true)
  public ResendPreview preview(Long companyId, Long clientId, Kind kind, Long documentId) {
    Client client = clients.require(companyId, clientId);
    boolean other = support.currentUser().hasAuthority(RESEND_OTHER);
    if (kind == Kind.RA) {
      DocumentView ra = requireAdvice(companyId, client, documentId);
      return new ResendPreview(
          ra.fileName(), client.getEmail(), raSubject(client), raBody(client, ra), other);
    }
    Epolicy e = requireEpolicy(client, documentId);
    DispatchEmail draft = dispatch.draft(e.getId());
    return new ResendPreview(
        e.getFileName(), client.getEmail(), draft.subject(), draft.body(), other);
  }

  /**
   * Resends a renewal advice of the client's records.
   *
   * @param companyId company
   * @param clientId client
   * @param request renewal advice, recipient and reason
   * @return what was sent
   */
  public ResendResult resendAdvice(Long companyId, Long clientId, ResendRequest request) {
    Client client = clients.require(companyId, clientId);
    DocumentView ra = requireAdvice(companyId, client, request.documentId());
    String to = recipient(client, request);
    AttachmentFile file = documents.download(ra.id());
    QueuedEmail queued =
        messages.queueEmail(
            new OutboundEmail(
                companyId,
                CsfCodes.PURPOSE_RESEND_RA,
                List.of(to),
                List.of(),
                raSubject(client),
                raBody(client, ra),
                List.of(
                    new MessageFile(
                        file.metadata().getFileName(),
                        file.metadata().getContentType(),
                        file.content())),
                new Protection(null, true, null),
                new RecordLink(
                    CsfCodes.ENTITY_CLIENT, String.valueOf(client.getId()), client.getCode())));
    done(client, ActivityAction.RESEND_RA, ra.fileName(), to, request.reason());
    return new ResendResult(to, queued.messageId(), ra.fileName());
  }

  /**
   * Resends the confirmed e-policy of an account of the client.
   *
   * @param companyId company
   * @param clientId client
   * @param request e-policy, recipient and reason
   * @return what was sent
   */
  public ResendResult resendEpolicy(Long companyId, Long clientId, ResendRequest request) {
    Client client = clients.require(companyId, clientId);
    Epolicy e = requireEpolicy(client, request.documentId());
    String to = recipient(client, request);
    DispatchEmail draft = dispatch.draft(e.getId());
    dispatch.dispatch(
        e.getId(),
        new DispatchEmail(
            List.of(to), List.of(), draft.subject(), draft.body(), draft.passwordHint()));
    done(
        client,
        ActivityAction.RESEND_EPOLICY,
        e.getFileName() + " " + e.getArn(),
        to,
        request.reason());
    return new ResendResult(to, null, e.getFileName());
  }

  private DocumentView requireAdvice(Long companyId, Client client, Long attachmentId) {
    DocumentView d = csfDocuments.require(companyId, client.getId(), attachmentId);
    if (!CsfCodes.DOC_RENEWAL_ADVICE.equals(d.documentType())) {
      throw new BusinessRuleException(
          "CSF_NOT_A_RENEWAL_ADVICE", "The document is not a renewal advice");
    }
    return d;
  }

  private Epolicy requireEpolicy(Client client, Long epolicyId) {
    Epolicy e = epolicies.get(epolicyId);
    boolean own =
        clients.accountsOf(client).stream().map(Account::getId).anyMatch(e.getAccountId()::equals);
    if (!own) {
      throw new ResourceNotFoundException("E-policy", epolicyId);
    }
    if (e.getStatus() != EpolicyStatus.CONFIRMED) {
      throw new BusinessRuleException(
          "CSF_NO_CONFIRMED_EPOLICY", "The account has no confirmed e-policy");
    }
    return e;
  }

  private String recipient(Client client, ResendRequest request) {
    String registered = blankToNull(client.getEmail());
    String asked = blankToNull(request.recipient());
    if (asked == null || asked.equalsIgnoreCase(registered)) {
      if (registered == null) {
        throw new BusinessRuleException(
            "CSF_NO_REGISTERED_EMAIL",
            "The client has no registered e-mail. Update the contact details first");
      }
      return registered;
    }
    if (!support.currentUser().hasAuthority(RESEND_OTHER)) {
      throw new AccessDeniedException("You are not permitted to perform this action");
    }
    if (blankToNull(request.reason()) == null) {
      throw new BusinessRuleException(
          "CSF_RESEND_REASON_REQUIRED", "Enter the reason for sending to another address");
    }
    if (!EmailAddresses.isValid(asked)) {
      throw new BusinessRuleException("EMAIL_ADDRESS_INVALID", "Enter a valid e-mail address");
    }
    return asked;
  }

  private void done(
      Client client, ActivityAction action, String document, String to, String reason) {
    String other =
        to.equalsIgnoreCase(String.valueOf(client.getEmail()))
            ? ""
            : " (another address: " + reason.strip() + ")";
    support
        .audit()
        .record(
            CsfCodes.ENTITY_CLIENT,
            client.getProspectCode(),
            AuditAction.UPDATE,
            CsfSupport.cut(document + " resent to " + to + other));
    support
        .activity()
        .record(
            client.getCompanyId(),
            action,
            new CsfActivity.Subject(
                client.getId(), client.getCode(), document, "To " + to + other));
  }

  private static String raSubject(Client client) {
    return "Your renewal advice - " + client.getDisplayName();
  }

  private static String raBody(Client client, DocumentView ra) {
    return "Dear "
        + client.getDisplayName()
        + ",\n\nAs requested, we send you again your renewal advice ("
        + ra.fileName()
        + ", reference "
        + ra.reference()
        + "). The file is protected with a password, which follows in a separate e-mail.\n\n"
        + "Thank you for insuring with us.\n\nBDO Insure Contact Center";
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /** What is resent. */
  public enum Kind {
    /** A renewal advice document. */
    RA,
    /** A confirmed e-policy. */
    EPOLICY
  }

  /**
   * A resend asked by the agent.
   *
   * @param documentId attachment of the renewal advice, or e-policy id
   * @param recipient another address, null for the registered e-mail
   * @param reason reason for another address
   */
  public record ResendRequest(Long documentId, String recipient, String reason) {}
}
