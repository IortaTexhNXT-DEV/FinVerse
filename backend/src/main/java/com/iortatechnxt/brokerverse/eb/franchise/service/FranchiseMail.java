package com.iortatechnxt.brokerverse.eb.franchise.service;

import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.eb.document.service.CycleDocuments;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService.Registration;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentSource;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import com.iortatechnxt.brokerverse.eb.domain.EbFranchiseRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.service.EbMailer;
import com.iortatechnxt.brokerverse.eb.service.EbMailer.Mail;
import com.iortatechnxt.brokerverse.eb.service.EbParties;
import com.iortatechnxt.brokerverse.eb.service.RequiredDocumentCheck;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The documents and e-mails of the franchise requests: the files sent (the Broker on Record in
 * force and the documents required for the process FRANCHISE), the request to the insurer, the
 * insurer's reply stored as evidence, the notice to the AO and the advice to the client.
 */
@Component
public class FranchiseMail {

  private final EbParties parties;
  private final EbMailer mailer;
  private final CycleDocuments cycleDocuments;
  private final RequiredDocumentCheck required;
  private final EbDocumentService documents;
  private final DocTemplateService templates;
  private final NotificationService notifications;
  private final LovService lovs;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the helper.
   *
   * @param parties insurers, contacts and template values
   * @param mailer e-mails
   * @param cycleDocuments documents of the cycle
   * @param required required documents
   * @param documents EB document register
   * @param templates e-mail templates
   * @param notifications in-app notices
   * @param lovs reason labels
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public FranchiseMail(
      EbParties parties,
      EbMailer mailer,
      CycleDocuments cycleDocuments,
      RequiredDocumentCheck required,
      EbDocumentService documents,
      DocTemplateService templates,
      NotificationService notifications,
      LovService lovs,
      CurrentUser currentUser,
      Clock clock) {
    this.parties = parties;
    this.mailer = mailer;
    this.cycleDocuments = cycleDocuments;
    this.required = required;
    this.documents = documents;
    this.templates = templates;
    this.notifications = notifications;
    this.lovs = lovs;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * An insurer of the catalogue.
   *
   * @param companyId company
   * @param code insurer code
   * @return insurer
   */
  InsurerProfile insurer(Long companyId, String code) {
    return parties.insurer(companyId, code);
  }

  /**
   * The files of a franchise request: the BOR in force and the required documents, refused while a
   * mandatory document is missing.
   *
   * @param programme programme
   * @param cycle cycle
   * @param date business date
   * @return stored files
   */
  List<Long> requestFiles(EbProgramme programme, EbCycle cycle, LocalDate date) {
    List<String> lines = benefitLines(programme);
    Set<String> present = cycleDocuments.presentTypes(cycle, date);
    required.require(programme.getCompanyId(), EbDocumentTypes.FRANCHISE, lines, present);
    List<String> types = new ArrayList<>();
    types.add(EbDocumentTypes.BOR);
    required.of(programme.getCompanyId(), EbDocumentTypes.FRANCHISE, lines).stream()
        .map(r -> r.getDocumentType())
        .forEach(types::add);
    return cycleDocuments.files(cycle, types, date);
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
   * @param files files to attach
   * @param due decision due date
   * @return outbox message
   */
  Long sendRequest(
      EbProgramme programme,
      EbCycle cycle,
      EbFranchiseRequest request,
      InsurerProfile insurer,
      List<Long> files,
      LocalDate due) {
    Map<String, Object> values = parties.values(programme, cycle);
    values.put("insurerName", insurer.getName());
    values.put("franchiseNo", request.getFranchiseNo());
    values.put("dueDate", EbParties.date(due));
    MergedText text = merge(EbCodes.TEMPLATE_FRANCHISE_REQUEST, values);
    return mailer
        .send(
            programme.getCompanyId(),
            EbCodes.PURPOSE_FRANCHISE,
            new Mail(
                EbParties.mailboxes(insurer),
                parties.aoCopy(programme),
                text.title(),
                text.text(),
                mailer.files(files)),
            link(request))
        .messageId();
  }

  /**
   * Stores the insurer's reply as evidence of the decision.
   *
   * @param cycle cycle
   * @param request request
   * @param reply the reply
   * @return stored file
   */
  Long storeReply(EbCycle cycle, EbFranchiseRequest request, UploadedFile reply) {
    return documents
        .store(
            cycle,
            new Registration(
                EbDocumentTypes.FRANCHISE_FORM,
                EbDocumentTypes.FRANCHISE,
                EbDocumentSource.INSURER,
                false,
                "Insurer reply to " + request.getFranchiseNo()),
            List.of(reply))
        .get(0)
        .getAttachmentId();
  }

  /**
   * Tells the AO of a decision recorded by someone else.
   *
   * @param programme programme
   * @param request request
   * @param outcome APPROVED or REJECTED
   */
  void tellAo(
      EbProgramme programme, EbFranchiseRequest request, EbFranchiseRequest.Status outcome) {
    if (CurrentUser.sameUser(programme.getAccountOfficer(), currentUser.username())) {
      return;
    }
    String insurerName = parties.insurerName(programme.getCompanyId(), request.getInsurerCode());
    notifications.notifyUser(
        programme.getAccountOfficer(),
        new Notice(
            request.getFranchiseNo() + ": franchise " + word(outcome),
            insurerName + " " + word(outcome) + " the franchise for " + programme.getName(),
            EbCodes.PROGRAMME_LINK + programme.getId() + "?tab=franchise",
            EbCodes.ENTITY_FRANCHISE,
            request.getId().toString()),
        EbCodes.EVENT_FRANCHISE_DECIDED);
  }

  /**
   * E-mails the outcome to the client's HR contacts.
   *
   * @param programme programme
   * @param cycle cycle
   * @param request decided request
   */
  void sendAdvice(EbProgramme programme, EbCycle cycle, EbFranchiseRequest request) {
    Map<String, Object> values = parties.values(programme, cycle);
    values.put(
        "insurerName", parties.insurerName(programme.getCompanyId(), request.getInsurerCode()));
    values.put("outcome", word(request.getDecision()));
    values.put(
        "reasonText",
        request.getReasonCode() == null
            ? ""
            : " Reason: "
                + lovs.label(EbCodes.LOV_FRANCHISE_REJECT_REASON, request.getReasonCode())
                + ".");
    MergedText text = merge(EbCodes.TEMPLATE_FRANCHISE_ADVICE, values);
    mailer.send(
        programme.getCompanyId(),
        EbCodes.PURPOSE_FRANCHISE,
        new Mail(
            EbParties.contactEmails(programme),
            parties.aoCopy(programme),
            text.title(),
            text.text(),
            List.of()),
        link(request));
  }

  private static String word(EbFranchiseRequest.Status outcome) {
    return outcome == EbFranchiseRequest.Status.APPROVED ? "approved" : "rejected";
  }

  private MergedText merge(String template, Map<String, Object> values) {
    MergedText text = templates.merge(template, BusinessClock.today(clock), values);
    return new MergedText(
        text.code(), text.versionNo(), DocTemplateService.fill(text.title(), values), text.text());
  }

  private static RecordLink link(EbFranchiseRequest request) {
    return new RecordLink(
        EbCodes.ENTITY_FRANCHISE, request.getId().toString(), request.getFranchiseNo());
  }
}
