package com.iortatechnxt.brokerverse.eb.comparative.service;

import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService.Registration;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbComparative;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentSource;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.TatActivity;
import com.iortatechnxt.brokerverse.eb.service.EbActivityLog;
import com.iortatechnxt.brokerverse.eb.service.EbMailer;
import com.iortatechnxt.brokerverse.eb.service.EbMailer.Mail;
import com.iortatechnxt.brokerverse.eb.service.EbParties;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.eb.service.EbTemplates;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Presents an approved comparative to the client (BRID-011; FR-EB-043): the PDF of the matrix is
 * stored as {@code EB_COMPARATIVE} and e-mailed (protected) to the HR contacts with the template
 * {@code EB_COMPARATIVE}; the cycle moves to WITH_CLIENT. Without the portal the client answers by
 * e-mail and the AO records the comments and the confirmation.
 */
@Service
@Transactional
public class ComparativePresenter {

  private static final String PDF = "application/pdf";

  private final EbComparativeService comparatives;
  private final ComparativeExport export;
  private final EbRecords records;
  private final EbParties parties;
  private final EbTemplates templates;
  private final EbMailer mailer;
  private final EbDocumentService documents;
  private final WorkflowService workflow;
  private final EbActivityLog activity;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param comparatives comparatives
   * @param export PDF of the comparative
   * @param records cycle look-up
   * @param parties contacts and template values
   * @param templates e-mail template
   * @param mailer e-mails
   * @param documents EB document register
   * @param workflow workflow engine
   * @param activity TAT stamps
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ComparativePresenter(
      EbComparativeService comparatives,
      ComparativeExport export,
      EbRecords records,
      EbParties parties,
      EbTemplates templates,
      EbMailer mailer,
      EbDocumentService documents,
      WorkflowService workflow,
      EbActivityLog activity,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.comparatives = comparatives;
    this.export = export;
    this.records = records;
    this.parties = parties;
    this.templates = templates;
    this.mailer = mailer;
    this.documents = documents;
    this.workflow = workflow;
    this.activity = activity;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Presents the comparative to the client.
   *
   * @param companyId company
   * @param comparativeId approved comparative
   * @return the comparative, PRESENTED
   */
  public EbComparative present(Long companyId, Long comparativeId) {
    EbComparative comparative = comparatives.require(companyId, comparativeId);
    EbCycle cycle = records.openCycle(companyId, comparative.getCycleId());
    if (cycle.getStage() != EbCycleStage.READY_TO_PRESENT
        || comparative.getStatus() != EbComparative.Status.APPROVED) {
      throw new BusinessRuleException(
          "EB_COMPARATIVE_NOT_APPROVED", "Only an approved comparative can be presented");
    }
    EbProgramme programme = records.programmeOf(cycle);
    byte[] pdf = export.pdf(comparative);
    String fileName = comparative.getComparativeNo() + ".pdf";
    Long attachment =
        documents
            .store(
                cycle,
                new Registration(
                    EbDocumentTypes.COMPARATIVE,
                    EbDocumentTypes.PROPOSAL_PROCESS,
                    EbDocumentSource.SYSTEM,
                    true,
                    "Comparative " + comparative.getComparativeNo()),
                List.of(new UploadedFile(fileName, pdf)))
            .get(0)
            .getAttachmentId();
    Map<String, Object> values = parties.values(programme, cycle);
    values.put("comparativeNo", comparative.getComparativeNo());
    MergedText text = templates.merge(EbCodes.TEMPLATE_COMPARATIVE, values);
    mailer.send(
        companyId,
        EbCodes.PURPOSE_COMPARATIVE,
        new Mail(
            EbParties.contactEmails(programme),
            parties.aoCopy(programme),
            text.title(),
            text.text(),
            List.of(new MessageFile(fileName, PDF, pdf))),
        new RecordLink(
            EbCodes.ENTITY_COMPARATIVE,
            comparative.getId().toString(),
            comparative.getComparativeNo()));
    comparative.presented(clock.instant(), currentUser.username(), attachment);
    workflow.systemTransition(
        EbCodes.ENTITY_CYCLE, cycle.getId().toString(), "present", TransitionNote.NONE);
    activity.released(
        TatActivity.PROPOSAL_TO_CLIENT, comparative.getComparativeNo(), "Presented to the client");
    audit.record(
        EbCodes.ENTITY_COMPARATIVE,
        comparative.getComparativeNo(),
        AuditAction.SUBMIT,
        "Presented to the client");
    return comparative;
  }
}
