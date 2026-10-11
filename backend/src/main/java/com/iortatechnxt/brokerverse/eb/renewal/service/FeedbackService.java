package com.iortatechnxt.brokerverse.eb.renewal.service;

import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService.Registration;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentSource;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import com.iortatechnxt.brokerverse.eb.domain.EbFeedback;
import com.iortatechnxt.brokerverse.eb.domain.EbFeedbackChannel;
import com.iortatechnxt.brokerverse.eb.domain.EbFeedbackRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbRenewalAdviceRepository;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records the client's feedback on a cycle (BRID-002, 003; FR-EB-023): channel, date received (not
 * after today), text and / or files (PDF, Word, images, e-mails; stored as {@code
 * EB_CLIENT_FEEDBACK} with source CLIENT). The first feedback on a renewal advice moves the cycle
 * from RA_SENT to REQUIREMENTS and stops the reminders. The AO of the programme is notified when
 * someone else records it ({@code EB_FEEDBACK_RECEIVED}).
 */
@Service
@Transactional
public class FeedbackService {

  private final EbFeedbackRepository feedback;
  private final EbRenewalAdviceRepository advices;
  private final EbRecords records;
  private final EbDocumentService documents;
  private final WorkflowService workflow;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param feedback feedback
   * @param advices renewal advices (reminders)
   * @param records cycle look-up
   * @param documents EB document register
   * @param workflow workflow engine
   * @param notifications in-app notices
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public FeedbackService(
      EbFeedbackRepository feedback,
      EbRenewalAdviceRepository advices,
      EbRecords records,
      EbDocumentService documents,
      WorkflowService workflow,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.feedback = feedback;
    this.advices = advices;
    this.records = records;
    this.documents = documents;
    this.workflow = workflow;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Records feedback on an open cycle.
   *
   * @param companyId company
   * @param cycleId cycle
   * @param input channel, date received, text and files
   * @return the feedback
   */
  public EbFeedback record(Long companyId, Long cycleId, FeedbackInput input) {
    EbCycle cycle = records.openCycle(companyId, cycleId);
    if (input.channel() == null) {
      throw new BusinessRuleException("EB_FEEDBACK_CHANNEL_REQUIRED", "Select the channel");
    }
    LocalDate today = BusinessClock.today(clock);
    if (input.receivedOn() == null) {
      throw new BusinessRuleException("EB_FEEDBACK_DATE_REQUIRED", "Enter the date received");
    }
    if (input.receivedOn().isAfter(today)) {
      throw new BusinessRuleException(
          "EB_FEEDBACK_DATE_FUTURE", "The date received cannot be after today");
    }
    EbFeedback saved =
        feedback.save(
            new EbFeedback(
                cycle, input.channel(), input.receivedOn(), input.text(), input.files().size()));
    if (!input.files().isEmpty()) {
      documents.store(
          cycle,
          new Registration(
              EbDocumentTypes.CLIENT_FEEDBACK,
              EbDocumentService.placementProcess(cycle),
              EbDocumentSource.CLIENT,
              false,
              "Client feedback of " + input.receivedOn()),
          input.files());
    }
    advices.findByCycleId(cycle.getId()).ifPresent(a -> a.stopReminders(clock.instant()));
    if (cycle.getStage() == EbCycleStage.RA_SENT) {
      workflow.systemTransition(
          EbCodes.ENTITY_CYCLE,
          cycle.getId().toString(),
          "record_feedback",
          TransitionNote.comment("Client feedback of " + input.receivedOn()));
    }
    audit.record(
        EbCodes.ENTITY_CYCLE,
        cycle.getCycleNo(),
        AuditAction.CREATE,
        "Client feedback received "
            + input.receivedOn()
            + " by "
            + input.channel()
            + " with "
            + input.files().size()
            + " file(s)");
    tellAo(cycle);
    return saved;
  }

  private void tellAo(EbCycle cycle) {
    EbProgramme programme = records.programmeOf(cycle);
    if (CurrentUser.sameUser(programme.getAccountOfficer(), currentUser.username())) {
      return;
    }
    notifications.notifyUser(
        programme.getAccountOfficer(),
        new Notice(
            cycle.getCycleNo() + ": client feedback received",
            programme.getClientName() + " gave feedback on " + programme.getName() + ".",
            EbCodes.PROGRAMME_LINK + programme.getId() + "?tab=cycle",
            EbCodes.ENTITY_CYCLE,
            cycle.getId().toString()),
        EbCodes.EVENT_FEEDBACK_RECEIVED);
  }

  /**
   * The feedback of a programme, latest first.
   *
   * @param companyId company
   * @param programmeId programme
   * @return feedback
   */
  @Transactional(readOnly = true)
  public List<EbFeedback> ofProgramme(Long companyId, Long programmeId) {
    return feedback.findByProgrammeIdOrderByReceivedOnDescIdDesc(
        records.programme(companyId, programmeId).getId());
  }

  /**
   * Feedback as entered.
   *
   * @param channel how it arrived
   * @param receivedOn date received
   * @param text text, may be blank when files are given
   * @param files files, may be empty when text is given
   */
  public record FeedbackInput(
      EbFeedbackChannel channel, LocalDate receivedOn, String text, List<UploadedFile> files) {

    /** Defensive copy. */
    public FeedbackInput {
      files = files == null ? List.of() : List.copyOf(files);
    }
  }
}
