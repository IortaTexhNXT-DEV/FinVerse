package com.iortatechnxt.brokerverse.issuance.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService.AttachmentFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.issuance.domain.Epolicy;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageSearch;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail.Protection;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E-policy dispatch to the client (BRNB.077/035): the e-policy PDF is encrypted, the password is
 * generated with the standard password policy and sent in a separate e-mail, the body comes from
 * the EPOLICY_EMAIL template. The dispatch report is the messaging log filtered on the purpose
 * {@value #PURPOSE}.
 */
@Service
@Transactional
public class EpolicyDispatchService {

  /** Messaging purpose of e-policy e-mails (dispatch report filter). */
  public static final String PURPOSE = "EPOLICY";

  /** Parameter: password hint sent to the client. */
  public static final String HINT_PARAMETER = "EPOLICY_PASSWORD_HINT";

  private static final String TEMPLATE = "EPOLICY_EMAIL";

  private final EpolicyService epolicies;
  private final AccountQueryService accounts;
  private final AttachmentService attachments;
  private final InsurerService insurers;
  private final DocTemplateService templates;
  private final MessageService messages;
  private final SystemParameterService parameters;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param epolicies e-policies
   * @param accounts account reads
   * @param attachments stored files
   * @param insurers insurer panel
   * @param templates document templates
   * @param messages outbound e-mail
   * @param parameters business parameters
   * @param audit audit trail
   * @param clock clock
   */
  public EpolicyDispatchService(
      EpolicyService epolicies,
      AccountQueryService accounts,
      AttachmentService attachments,
      InsurerService insurers,
      DocTemplateService templates,
      MessageService messages,
      SystemParameterService parameters,
      AuditTrailService audit,
      Clock clock) {
    this.epolicies = epolicies;
    this.accounts = accounts;
    this.attachments = attachments;
    this.insurers = insurers;
    this.templates = templates;
    this.messages = messages;
    this.parameters = parameters;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The e-mail proposed for an e-policy: the account contact, the template text and the password
   * hint.
   *
   * @param epolicyId e-policy
   * @return draft
   */
  @Transactional(readOnly = true)
  public DispatchEmail draft(Long epolicyId) {
    Epolicy epolicy = epolicies.get(epolicyId);
    Account account = accounts.requireByArn(epolicy.getArn());
    String email = account.getContact().email();
    MergedText text =
        templates.merge(
            TEMPLATE,
            LocalDate.now(clock),
            Map.of(
                "clientName", account.getClientName(),
                "policyNo", String.join(", ", epolicy.getPolicyNumberList()),
                "insurerName", insurerName(account),
                "reference", account.getArn()));
    return new DispatchEmail(
        email == null || email.isBlank() ? List.of() : List.of(email.strip()),
        List.of(),
        text.title() + " " + String.join(", ", epolicy.getPolicyNumberList()),
        text.text(),
        parameters.text(HINT_PARAMETER, null));
  }

  private String insurerName(Account account) {
    if (account.getInsurerCode() == null) {
      return "your insurer";
    }
    try {
      return insurers.requireInsurer(account.getCompanyId(), account.getInsurerCode()).getName();
    } catch (ResourceNotFoundException e) {
      return account.getInsurerCode();
    }
  }

  /**
   * Sends an e-policy to the client, encrypted, with the password in a separate e-mail.
   *
   * @param epolicyId confirmed e-policy
   * @param email recipients, subject, body and password hint
   * @return the e-policy
   */
  public Epolicy dispatch(Long epolicyId, DispatchEmail email) {
    Epolicy epolicy = epolicies.get(epolicyId);
    if (email.to().isEmpty()) {
      throw new BusinessRuleException(
          "EPOLICY_NO_RECIPIENT",
          "Account " + epolicy.getArn() + " has no contact e-mail: enter the recipient");
    }
    AttachmentFile file = attachments.download(epolicy.getAttachmentId());
    epolicy.dispatched(String.join(", ", email.to()), clock.instant());
    messages.queueEmail(
        new OutboundEmail(
            epolicy.getCompanyId(),
            PURPOSE,
            email.to(),
            email.cc(),
            email.subject(),
            email.body(),
            List.of(
                new MessageFile(file.metadata().getFileName(), "application/pdf", file.content())),
            new Protection(null, true, email.passwordHint()),
            new RecordLink(
                AccountService.ENTITY, String.valueOf(epolicy.getAccountId()), epolicy.getArn())));
    audit.record(
        EpolicyService.ENTITY,
        epolicy.getArn(),
        AuditAction.UPDATE,
        "E-policy " + epolicy.getFileName() + " sent to " + String.join(", ", email.to()));
    return epolicy;
  }

  /**
   * The dispatch report: e-policy e-mails from the messaging log, newest first.
   *
   * @param text recipient, subject or ARN fragment
   * @param pageable page
   * @return messages
   */
  @Transactional(readOnly = true)
  public Page<OutboundMessage> log(String text, Pageable pageable) {
    return messages.search(new MessageSearch(null, PURPOSE, text), pageable);
  }

  /**
   * An e-policy e-mail.
   *
   * @param to recipients
   * @param cc copy
   * @param subject subject
   * @param body body
   * @param passwordHint how the password is built
   */
  public record DispatchEmail(
      List<String> to, List<String> cc, String subject, String body, String passwordHint) {

    /** Defensive copies. */
    public DispatchEmail {
      to = to == null ? List.of() : List.copyOf(to);
      cc = cc == null ? List.of() : List.copyOf(cc);
    }
  }
}
