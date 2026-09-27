package com.iortatechnxt.brokerverse.eb.member.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberChange;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.service.EbMailer;
import com.iortatechnxt.brokerverse.eb.service.EbMailer.Mail;
import com.iortatechnxt.brokerverse.eb.service.EbParties;
import com.iortatechnxt.brokerverse.eb.service.EbTemplates;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * The e-mail relaying a member change to the insurer of its line (template {@code
 * EB_MEMBER_CHANGE_RELAY}), with the client's request attached and password-protected.
 */
@Component
public class MemberChangeMail {

  private final EbParties parties;
  private final EbMailer mailer;
  private final EbTemplates templates;
  private final DocumentService documents;

  /**
   * Creates the helper.
   *
   * @param parties insurer and template values
   * @param mailer e-mails
   * @param templates relay template
   * @param documents files of the change
   */
  public MemberChangeMail(
      EbParties parties, EbMailer mailer, EbTemplates templates, DocumentService documents) {
    this.parties = parties;
    this.mailer = mailer;
    this.templates = templates;
    this.documents = documents;
  }

  /**
   * E-mails the change to the insurer of the line.
   *
   * @param programme programme
   * @param line programme line
   * @param change change
   * @return outbox message
   */
  Long relay(EbProgramme programme, EbProgrammeLine line, EbMemberChange change) {
    if (line.getIncumbentInsurer() == null) {
      throw new BusinessRuleException(
          "EB_LINE_NO_INSURER", "Line " + line.getLineNo() + " has no insurer to relay the change to");
    }
    InsurerProfile insurer = parties.insurer(programme.getCompanyId(), line.getIncumbentInsurer());
    Map<String, Object> values = parties.values(programme, null);
    values.put("insurerName", insurer.getName());
    values.put("changeNo", change.getChangeNo());
    values.put("policyNo", line.getCurrentPolicyNo() == null ? "" : line.getCurrentPolicyNo());
    values.put(
        "memberLines",
        change.getLines().stream()
            .map(
                l ->
                    l.getSortOrder() + ". " + l.getAction().name().replace('_', ' ') + " "
                        + l.getEmployeeNo()
                        + (l.getLastName() == null ? "" : " " + l.getLastName() + ", " + l.getFirstName())
                        + (l.getPlanCode() == null ? "" : ", plan " + l.getPlanCode())
                        + ", effective " + EbParties.date(l.getEffectiveDate()))
            .collect(Collectors.joining("\n")));
    MergedText text = templates.merge(EbCodes.TEMPLATE_MEMBER_CHANGE_RELAY, values);
    List<Long> files =
        documents.all(new AttachmentTarget(EbCodes.ENTITY_MEMBER_CHANGE, change.getId().toString()))
            .stream()
            .map(Attachment::getId)
            .toList();
    return mailer
        .send(
            programme.getCompanyId(),
            EbCodes.PURPOSE_MEMBER_CHANGE,
            new Mail(
                EbParties.mailboxes(insurer),
                parties.aoCopy(programme),
                text.title(),
                text.text(),
                mailer.files(files)),
            new RecordLink(
                EbCodes.ENTITY_MEMBER_CHANGE, change.getId().toString(), change.getChangeNo()))
        .messageId();
  }
}
