package com.iortatechnxt.brokerverse.issuance.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.issuance.domain.AdviceRecipient;
import com.iortatechnxt.brokerverse.issuance.domain.AdviceSendMode;
import com.iortatechnxt.brokerverse.issuance.domain.InsuranceAdvice;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail.Protection;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends Insurance Advices (BRNB.060/035): one e-mail per advice to the recipients chosen (the
 * mortgagee bank unit, Q30), the PDF encrypted with a password sent in a separate e-mail. The send
 * log is the messaging outbox of entity {@value InsuranceAdviceService#ENTITY}. An advice whose
 * mortgagee bank is enrolled for automatic sending is sent as soon as it is generated (FR-NB-107);
 * when it cannot be, it stays Generated with the reason and Processing is notified.
 */
@Service
@Transactional
public class AdviceDispatchService {

  /** Messaging purpose of Insurance Advice e-mails. */
  public static final String PURPOSE = "INSURANCE_ADVICE";

  /** Permission of the users notified of a failed automatic sending (Processing). */
  static final String NOTIFIED_PERMISSION = "EPOLICY_SEND";

  private static final int MAX_ADVICES = 50;

  private final InsuranceAdviceService advices;
  private final AdviceRecipientService recipients;
  private final MessageService messages;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param advices insurance advices
   * @param recipients recipient set-up of the mortgagee banks
   * @param messages outbound e-mail
   * @param notifications in-app notifications
   * @param audit audit trail
   * @param clock clock
   */
  public AdviceDispatchService(
      InsuranceAdviceService advices,
      AdviceRecipientService recipients,
      MessageService messages,
      NotificationService notifications,
      AuditTrailService audit,
      Clock clock) {
    this.advices = advices;
    this.recipients = recipients;
    this.messages = messages;
    this.notifications = notifications;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Sends one or more advices.
   *
   * @param request advices, recipients and password hint
   * @return the advices sent
   */
  public List<InsuranceAdvice> send(AdviceEmail request) {
    if (request.adviceIds().isEmpty() || request.adviceIds().size() > MAX_ADVICES) {
      throw new BusinessRuleException(
          "IA_SELECTION", "Select between 1 and " + MAX_ADVICES + " Insurance Advices");
    }
    return request.adviceIds().stream()
        .distinct()
        .map(id -> sendOne(advices.get(id), request, AdviceSendMode.MANUAL))
        .toList();
  }

  /**
   * Sends a new advice at once when its mortgagee bank is enrolled for automatic sending
   * (FR-NB-107). An advice of a bank that is not enrolled waits in the register.
   *
   * @param event advice generated
   */
  @EventListener
  public void on(InsuranceAdviceGenerated event) {
    InsuranceAdvice advice = advices.get(event.adviceId());
    Optional<AdviceRecipient> setup =
        recipients
            .applicable(advice.getCompanyId(), advice.getMortgageeBank(), event.marketSegment())
            .filter(AdviceRecipient::isAutoSend);
    if (setup.isPresent() && setup.get().getTo().isEmpty()) {
      failed(
          advice,
          "Insurance Advice "
              + advice.getIaNo()
              + " was not sent automatically: no recipient is set up for "
              + recipients.bankName(advice.getMortgageeBank()));
    } else if (setup.isPresent()) {
      sendOne(
          advice,
          new AdviceEmail(List.of(advice.getId()), setup.get().getTo(), setup.get().getCc(), null),
          AdviceSendMode.AUTOMATIC);
    }
  }

  private void failed(InsuranceAdvice advice, String reason) {
    advice.autoSendFailed(reason);
    audit.record(InsuranceAdviceService.ENTITY, advice.getIaNo(), AuditAction.UPDATE, reason);
    notifications.notifyPermission(
        NOTIFIED_PERMISSION,
        new Notice(
            "Insurance Advice " + advice.getIaNo() + " not sent",
            reason + ". Send it from the Insurance Advice register.",
            "/issuance/insurance-advice",
            InsuranceAdviceService.ENTITY,
            String.valueOf(advice.getId())));
  }

  private InsuranceAdvice sendOne(
      InsuranceAdvice advice, AdviceEmail request, AdviceSendMode mode) {
    messages.queueEmail(
        new OutboundEmail(
            advice.getCompanyId(),
            PURPOSE,
            request.to(),
            request.cc(),
            "Insurance Advice " + advice.getIaNo() + " - " + advice.getClientName(),
            "Please find attached Insurance Advice "
                + advice.getIaNo()
                + " for "
                + advice.getClientName()
                + " (our reference "
                + advice.getArn()
                + "). The document is password protected; the password follows in a separate"
                + " e-mail.\n\nBDO Insurance and Reinsurance Brokers, Inc.",
            List.of(
                new MessageFile(advice.getFileName(), "application/pdf", advices.content(advice))),
            new Protection(null, true, request.passwordHint()),
            new RecordLink(
                InsuranceAdviceService.ENTITY, String.valueOf(advice.getId()), advice.getIaNo())));
    advice.sent(String.join(", ", request.to()), clock.instant(), mode);
    audit.record(
        InsuranceAdviceService.ENTITY,
        advice.getIaNo(),
        AuditAction.UPDATE,
        (mode == AdviceSendMode.AUTOMATIC ? "Sent automatically to " : "Sent to ")
            + String.join(", ", request.to()));
    return advice;
  }

  /**
   * Insurance Advice e-mail.
   *
   * @param adviceIds advices to send
   * @param to recipients
   * @param cc copy
   * @param passwordHint how the password is built, may be null
   */
  public record AdviceEmail(
      List<Long> adviceIds, List<String> to, List<String> cc, String passwordHint) {

    /** Defensive copies. */
    public AdviceEmail {
      adviceIds = adviceIds == null ? List.of() : List.copyOf(adviceIds);
      to = to == null ? List.of() : List.copyOf(to);
      cc = cc == null ? List.of() : List.copyOf(cc);
    }
  }
}
