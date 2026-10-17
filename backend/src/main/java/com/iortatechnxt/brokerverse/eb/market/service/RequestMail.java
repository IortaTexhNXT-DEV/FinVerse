package com.iortatechnxt.brokerverse.eb.market.service;

import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.eb.document.service.CycleDocuments;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import com.iortatechnxt.brokerverse.eb.domain.EbInsurerRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.domain.EbTor;
import com.iortatechnxt.brokerverse.eb.service.EbMailer;
import com.iortatechnxt.brokerverse.eb.service.EbMailer.Mail;
import com.iortatechnxt.brokerverse.eb.service.EbParties;
import com.iortatechnxt.brokerverse.eb.service.EbTemplates;
import com.iortatechnxt.brokerverse.eb.service.RequiredDocumentCheck;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The e-mails of the requests for proposal (FR-EB-035): the TOR released, the unnamed master list,
 * the utilization report and the documents required for the process PROPOSAL, sent protected to the
 * insurer's placement mailboxes with the template {@code EB_RFP_COVER}.
 */
@Component
public class RequestMail {

  /** Documents distributed with the TOR besides the required ones. */
  private static final List<String> DISTRIBUTED =
      List.of(EbDocumentTypes.MASTERLIST_UNNAMED, EbDocumentTypes.UTILIZATION);

  private final EbParties parties;
  private final EbMailer mailer;
  private final EbTemplates templates;
  private final CycleDocuments cycleDocuments;
  private final RequiredDocumentCheck required;
  private final Clock clock;

  /**
   * Creates the helper.
   *
   * @param parties insurers and template values
   * @param mailer e-mails
   * @param templates e-mail template
   * @param cycleDocuments documents of the cycle
   * @param required required documents
   * @param clock clock
   */
  public RequestMail(
      EbParties parties,
      EbMailer mailer,
      EbTemplates templates,
      CycleDocuments cycleDocuments,
      RequiredDocumentCheck required,
      Clock clock) {
    this.parties = parties;
    this.mailer = mailer;
    this.templates = templates;
    this.cycleDocuments = cycleDocuments;
    this.required = required;
    this.clock = clock;
  }

  /**
   * The files sent with a request, refused while a document required for PROPOSAL is missing.
   *
   * @param programme programme
   * @param cycle cycle
   * @param tor released TOR
   * @return stored files, the TOR first
   */
  List<Long> files(EbProgramme programme, EbCycle cycle, EbTor tor) {
    LocalDate today = BusinessClock.today(clock);
    List<String> lines = benefitLines(programme);
    required.require(
        programme.getCompanyId(),
        EbDocumentTypes.PROPOSAL_PROCESS,
        lines,
        cycleDocuments.presentTypes(cycle, today));
    List<String> types = new ArrayList<>(DISTRIBUTED);
    required.of(programme.getCompanyId(), EbDocumentTypes.PROPOSAL_PROCESS, lines).stream()
        .map(r -> r.getDocumentType())
        .filter(t -> !EbDocumentTypes.TOR.equals(t))
        .forEach(types::add);
    List<Long> files = new ArrayList<>();
    files.add(tor.getAttachmentId());
    cycleDocuments.files(cycle, types, today).stream()
        .filter(id -> !files.contains(id))
        .forEach(files::add);
    return files;
  }

  /**
   * The benefit lines of the programme's active lines.
   *
   * @param programme programme
   * @return lines
   */
  static List<String> benefitLines(EbProgramme programme) {
    return programme.getLines().stream()
        .filter(EbProgrammeLine::isActive)
        .map(EbProgrammeLine::getBenefitLine)
        .distinct()
        .toList();
  }

  /**
   * E-mails a request to the insurer.
   *
   * @param programme programme
   * @param cycle cycle
   * @param request request
   * @param insurer insurer
   * @param files files
   * @return outbox message
   */
  Long send(
      EbProgramme programme,
      EbCycle cycle,
      EbInsurerRequest request,
      InsurerProfile insurer,
      List<Long> files) {
    Map<String, Object> values = parties.values(programme, cycle);
    values.put("insurerName", insurer.getName());
    values.put("requestNo", request.getRequestNo());
    values.put("dueDate", EbParties.date(request.getDueDate()));
    MergedText text = templates.merge(EbCodes.TEMPLATE_RFP_COVER, values);
    return mailer
        .send(
            programme.getCompanyId(),
            EbCodes.PURPOSE_RFP,
            new Mail(
                EbParties.mailboxes(insurer),
                parties.aoCopy(programme),
                text.title(),
                text.text(),
                mailer.files(files)),
            new RecordLink(
                EbCodes.ENTITY_REQUEST, request.getId().toString(), request.getRequestNo()))
        .messageId();
  }
}
