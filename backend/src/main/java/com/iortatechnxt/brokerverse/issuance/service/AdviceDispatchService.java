package com.iortatechnxt.brokerverse.issuance.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.issuance.domain.InsuranceAdvice;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail.Protection;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends Insurance Advices (BRNB.060/035): one e-mail per advice to the recipients chosen (the
 * mortgagee bank unit, Q30), the PDF encrypted with a password sent in a separate e-mail. The send
 * log is the messaging outbox of entity {@value InsuranceAdviceService#ENTITY}.
 */
@Service
@Transactional
public class AdviceDispatchService {

  /** Messaging purpose of Insurance Advice e-mails. */
  public static final String PURPOSE = "INSURANCE_ADVICE";

  private static final int MAX_ADVICES = 50;

  private final InsuranceAdviceService advices;
  private final MessageService messages;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param advices insurance advices
   * @param messages outbound e-mail
   * @param audit audit trail
   * @param clock clock
   */
  public AdviceDispatchService(
      InsuranceAdviceService advices,
      MessageService messages,
      AuditTrailService audit,
      Clock clock) {
    this.advices = advices;
    this.messages = messages;
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
        .map(id -> sendOne(advices.get(id), request))
        .toList();
  }

  private InsuranceAdvice sendOne(InsuranceAdvice advice, AdviceEmail request) {
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
            List.of(new MessageFile(advice.getFileName(), "application/pdf", advice.getContent())),
            new Protection(null, true, request.passwordHint()),
            new RecordLink(
                InsuranceAdviceService.ENTITY, String.valueOf(advice.getId()), advice.getIaNo())));
    advice.sent(String.join(", ", request.to()), clock.instant());
    audit.record(
        InsuranceAdviceService.ENTITY,
        advice.getIaNo(),
        AuditAction.UPDATE,
        "Sent to " + String.join(", ", request.to()));
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
