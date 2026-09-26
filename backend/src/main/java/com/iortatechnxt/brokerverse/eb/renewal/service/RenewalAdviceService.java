package com.iortatechnxt.brokerverse.eb.renewal.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.eb.cycle.service.CycleService;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService.Registration;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbDocument;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentSource;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeContact;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeStatus;
import com.iortatechnxt.brokerverse.eb.domain.EbRenewalAdvice;
import com.iortatechnxt.brokerverse.eb.domain.EbRenewalAdviceRepository;
import com.iortatechnxt.brokerverse.eb.domain.TatActivity;
import com.iortatechnxt.brokerverse.eb.service.EbActivityLog;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.messaging.service.QueuedEmail;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends the renewal advice of a programme (BRID-001; FR-EB-022): opens (or reuses) the RENEWAL
 * cycle of the policy year that starts at the next expiry, composes the advice from its template,
 * stores it as a {@code RENEWAL_ADVICE} document of the cycle linked to the programme, the client
 * and the expiring accounts (so Customer Servicing finds it, decision D3), e-mails it
 * password-protected to the HR contacts that receive it (password in a separate e-mail, FR-EB-004),
 * moves the cycle to RA_SENT and stamps the TAT activity. Called by the job {@code
 * EB_RENEWAL_ADVICE} and by the AO (Send RA). No advice goes to a programme that is not flagged for
 * renewal, has no current business or has no contact for it: the job raises {@code EB_RA_NOT_SENT}
 * to the AO instead.
 */
@Service
@Transactional
public class RenewalAdviceService {

  private static final String PDF = "application/pdf";

  private static final String PROGRAMME = "Programme ";

  private final EbRecords records;
  private final EbCycleRepository cycles;
  private final EbRenewalAdviceRepository advices;
  private final CycleService cycleService;
  private final RenewalAdviceLetter letter;
  private final EbDocumentService documents;
  private final AccountRepository accounts;
  private final MessageService messages;
  private final AlertService alerts;
  private final EbActivityLog activity;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final WorkflowService workflow;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records programme look-up
   * @param cycles cycles
   * @param advices renewal advices
   * @param cycleService opens the renewal cycle
   * @param letter advice composer
   * @param documents EB document register
   * @param accounts expiring accounts
   * @param messages e-mail outbox
   * @param alerts alert engine
   * @param activity TAT stamps
   * @param audit audit trail
   * @param currentUser current user
   * @param workflow workflow engine
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public RenewalAdviceService(
      EbRecords records,
      EbCycleRepository cycles,
      EbRenewalAdviceRepository advices,
      CycleService cycleService,
      RenewalAdviceLetter letter,
      EbDocumentService documents,
      AccountRepository accounts,
      MessageService messages,
      AlertService alerts,
      EbActivityLog activity,
      AuditTrailService audit,
      CurrentUser currentUser,
      WorkflowService workflow,
      Clock clock) {
    this.records = records;
    this.cycles = cycles;
    this.advices = advices;
    this.cycleService = cycleService;
    this.letter = letter;
    this.documents = documents;
    this.accounts = accounts;
    this.messages = messages;
    this.alerts = alerts;
    this.activity = activity;
    this.audit = audit;
    this.currentUser = currentUser;
    this.workflow = workflow;
    this.clock = clock;
  }

  /**
   * Sends the renewal advice of a programme at the AO's request (Send RA), for its next expiry.
   *
   * @param companyId company
   * @param programmeId programme
   * @param today business date
   * @return the advice sent
   */
  public EbRenewalAdvice sendManual(Long companyId, Long programmeId, LocalDate today) {
    EbProgramme programme = records.programme(companyId, programmeId);
    String refusal = refusal(programme);
    if (refusal != null) {
      throw new BusinessRuleException("EB_RA_NOT_ALLOWED", refusal);
    }
    RenewalTarget target =
        RenewalTarget.of(programme, today, null)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "EB_RA_NO_EXPIRY",
                        PROGRAMME
                            + programme.getProgrammeNo()
                            + " has no benefit line ending on or after today"));
    EbCycle cycle = cycleFor(programme, target, true);
    return send(programme, cycle, target, true);
  }

  /**
   * The job's step for one programme with a line expiring inside the lead time: sends the advice,
   * or raises {@code EB_RA_NOT_SENT} when the programme cannot receive it.
   *
   * @param programmeId programme
   * @param today business date
   * @param leadUntil last expiry date inside the lead time
   * @return what happened
   */
  public JobStep sendDue(Long programmeId, LocalDate today, LocalDate leadUntil) {
    EbProgramme programme = records.programme(programmeId);
    Optional<RenewalTarget> target = RenewalTarget.of(programme, today, leadUntil);
    if (target.isEmpty() || programme.getStatus() != EbProgrammeStatus.ACTIVE) {
      return JobStep.SKIPPED;
    }
    if (alreadyHandled(programme, target.get())) {
      return JobStep.SKIPPED;
    }
    String refusal = refusal(programme);
    if (refusal != null) {
      alerts.raise(
          EbCodes.ALERT_RA_NOT_SENT,
          new AlertFacts(
              programme.getCompanyId(),
              null,
              EbCodes.ENTITY_PROGRAMME,
              programme.getId().toString(),
              refusal + "; the renewal advice for " + target.get().expiry() + " was not sent",
              null,
              EbCodes.ALERT_RA_NOT_SENT + ":" + programme.getId() + ":" + target.get().expiry()));
      return JobStep.ALERTED;
    }
    send(programme, cycleFor(programme, target.get(), false), target.get(), false);
    return JobStep.SENT;
  }

  private static String refusal(EbProgramme programme) {
    if (!programme.isRenewalEligible()) {
      return PROGRAMME + programme.getProgrammeNo() + " is not flagged for renewal";
    }
    if (programme.getStatus() != EbProgrammeStatus.ACTIVE) {
      return PROGRAMME + programme.getProgrammeNo() + " has no current business to renew";
    }
    if (RenewalAdviceLetter.recipients(programme).isEmpty()) {
      return PROGRAMME
          + programme.getProgrammeNo()
          + " has no HR contact receiving the renewal advice";
    }
    return null;
  }

  /** A cycle of the policy year exists already: open with its advice, or closed. */
  private boolean alreadyHandled(EbProgramme programme, RenewalTarget target) {
    return cycles.findByProgrammeIdOrderByPolicyYearDescIdDesc(programme.getId()).stream()
        .filter(c -> c.getPolicyYear() == target.policyYear())
        .anyMatch(c -> !reusable(c));
  }

  private boolean reusable(EbCycle cycle) {
    return cycle.isOpen()
        && cycle.getBusinessType() == BusinessType.RENEWAL
        && cycle.getStage() == EbCycleStage.OPEN
        && advices.findByCycleId(cycle.getId()).isEmpty();
  }

  private EbCycle cycleFor(EbProgramme programme, RenewalTarget target, boolean manual) {
    Optional<EbCycle> open = cycles.findOpen(programme.getId(), target.policyYear());
    if (open.isPresent()) {
      if (!reusable(open.get())) {
        // The manual message names the stage; the job never gets here (alreadyHandled).
        throw new BusinessRuleException(
            "EB_RA_ALREADY_SENT",
            manual
                ? "Cycle "
                    + open.get().getCycleNo()
                    + " of "
                    + target.policyYear()
                    + " is already past the renewal advice"
                : "Cycle " + open.get().getCycleNo() + " is not open for a renewal advice");
      }
      return open.get();
    }
    return cycleService.open(
        programme,
        new CycleService.OpenCycle(BusinessType.RENEWAL, target.policyYear(), target.expiry()));
  }

  private EbRenewalAdvice send(
      EbProgramme programme, EbCycle cycle, RenewalTarget target, boolean manual) {
    LocalDate today = BusinessClock.today(clock);
    MergedText text = letter.advice(programme, cycle, target.expiry(), today);
    byte[] pdf = letter.pdf(programme, cycle, target.expiry(), text);
    String fileName = cycle.getCycleNo() + "_RENEWAL_ADVICE.pdf";
    EbDocument stored =
        documents
            .store(
                cycle,
                new Registration(
                    EbDocumentTypes.RENEWAL_ADVICE,
                    EbDocumentTypes.RENEWAL_PLACEMENT,
                    EbDocumentSource.SYSTEM,
                    true,
                    "Renewal advice " + target.policyYear()),
                List.of(new UploadedFile(fileName, pdf)),
                accountTargets(programme))
            .get(0);
    List<String> to =
        RenewalAdviceLetter.recipients(programme).stream()
            .map(EbProgrammeContact::getEmail)
            .toList();
    String ao = letter.aoEmail(programme);
    QueuedEmail queued =
        messages.queueEmail(
            new OutboundEmail(
                programme.getCompanyId(),
                EbCodes.PURPOSE_RENEWAL_ADVICE,
                to,
                ao == null ? List.of() : List.of(ao),
                text.title(),
                text.text(),
                List.of(new MessageFile(fileName, PDF, pdf)),
                new OutboundEmail.Protection(null, true, null),
                link(programme)));
    String by = currentUser.username();
    EbRenewalAdvice advice =
        advices.save(
            new EbRenewalAdvice(
                cycle,
                target.expiry(),
                new EbRenewalAdvice.Sending(clock.instant(), by, manual, to),
                queued.messageId(),
                stored.getAttachmentId()));
    workflow.systemTransition(
        EbCodes.ENTITY_CYCLE, cycle.getId().toString(), "send_ra", TransitionNote.NONE);
    activity.done(
        cycle,
        TatActivity.RENEWAL_ADVICE,
        cycle.getCycleNo(),
        by,
        "Sent to " + String.join(", ", to) + " for the expiry of " + target.expiry());
    audit.record(
        EbCodes.ENTITY_PROGRAMME,
        programme.getProgrammeNo(),
        AuditAction.SUBMIT,
        "Renewal advice of " + cycle.getCycleNo() + " sent to " + String.join(", ", to));
    return advice;
  }

  private List<AttachmentTarget> accountTargets(EbProgramme programme) {
    List<AttachmentTarget> targets = new ArrayList<>();
    programme.getLines().stream()
        .filter(EbProgrammeLine::isActive)
        .map(EbProgrammeLine::getCurrentArn)
        .filter(Objects::nonNull)
        .distinct()
        .map(accounts::findByArn)
        .flatMap(Optional::stream)
        .map(Account::getId)
        .forEach(id -> targets.add(new AttachmentTarget("Account", id.toString())));
    return targets;
  }

  /**
   * Sends the next reminder of an advice still waiting for feedback, when one is due (BRID-002):
   * one reminder per run for each reminder day reached; a cycle that left RA_SENT stops them.
   *
   * @param adviceId renewal advice
   * @param today business date
   * @param reminderDays days before expiry of the reminders
   * @return true when a reminder was sent
   */
  public boolean remindIfDue(Long adviceId, LocalDate today, List<Integer> reminderDays) {
    EbRenewalAdvice advice = advices.findById(adviceId).orElseThrow();
    EbCycle cycle = records.cycle(advice.getCompanyId(), advice.getCycleId());
    if (cycle.getStage() != EbCycleStage.RA_SENT) {
      advice.stopReminders(clock.instant());
      return false;
    }
    int due = RenewalTarget.remindersDue(advice.getExpiryDate(), reminderDays, today);
    if (due <= advice.getRemindersSent()) {
      return false;
    }
    EbProgramme programme = records.programmeOf(cycle);
    List<String> to =
        RenewalAdviceLetter.recipients(programme).stream()
            .map(EbProgrammeContact::getEmail)
            .toList();
    if (to.isEmpty()) {
      return false;
    }
    MergedText text = letter.reminder(programme, cycle, advice.getExpiryDate(), today);
    messages.queueEmail(
        new OutboundEmail(
            programme.getCompanyId(),
            EbCodes.PURPOSE_RENEWAL_ADVICE,
            to,
            List.of(),
            text.title(),
            text.text(),
            List.of(),
            null,
            link(programme)));
    advice.remind(clock.instant());
    audit.record(
        EbCodes.ENTITY_PROGRAMME,
        programme.getProgrammeNo(),
        AuditAction.UPDATE,
        "Renewal advice reminder "
            + advice.getRemindersSent()
            + " of "
            + cycle.getCycleNo()
            + " sent to "
            + String.join(", ", to));
    return true;
  }

  private static RecordLink link(EbProgramme programme) {
    return new RecordLink(
        EbCodes.ENTITY_PROGRAMME, programme.getId().toString(), programme.getProgrammeNo());
  }

  /** What the job did for one programme. */
  public enum JobStep {
    /** Advice sent. */
    SENT,
    /** Nothing to do. */
    SKIPPED,
    /** Not sendable: alert raised to the AO. */
    ALERTED
  }
}
