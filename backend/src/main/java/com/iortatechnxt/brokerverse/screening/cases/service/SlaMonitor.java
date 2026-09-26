package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEvent.EventFacts;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEventType;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStatus;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCaseRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The SLA check of open cases (SNSRP-405, 108, 802; FR-SS-044, 081), run hourly by {@code
 * SCR_SLA_MONITOR}: inside the reminder lead the owner gets one reminder per stage entry; past the
 * due time the case is flagged breached, the escalation role of the SLA matrix is notified and the
 * alert {@code SCR_SLA_BREACH} is raised, once per stage entry; and, inside the lead or past the
 * SLA, the assignee and the UCC are reminded of the required documents still missing, at most once
 * a day. Every reminder and breach is on the case timeline.
 */
@Service
@Transactional
public class SlaMonitor {

  private static final Set<CaseStage> MONITORED =
      EnumSet.complementOf(EnumSet.of(CaseStage.NEW, CaseStage.CLOSED));

  private final ScreeningCaseRepository cases;
  private final CaseValidator validator;
  private final CaseNotifier notifier;
  private final RoleMembers members;
  private final CaseTimeline timeline;
  private final AlertService alerts;
  private final Clock clock;

  /**
   * Creates the monitor.
   *
   * @param cases cases
   * @param validator document rules (advisory)
   * @param notifier notices
   * @param members role holders
   * @param timeline case timeline
   * @param alerts alerts
   * @param clock clock
   */
  public SlaMonitor(
      ScreeningCaseRepository cases,
      CaseValidator validator,
      CaseNotifier notifier,
      RoleMembers members,
      CaseTimeline timeline,
      AlertService alerts,
      Clock clock) {
    this.cases = cases;
    this.validator = validator;
    this.notifier = notifier;
    this.members = members;
    this.timeline = timeline;
    this.alerts = alerts;
    this.clock = clock;
  }

  /**
   * Checks every open case with a due time.
   *
   * @return what was sent
   */
  public Result run() {
    Instant now = clock.instant();
    LocalDate today = LocalDate.now(clock.withZone(CaseSpecs.MANILA));
    int reminders = 0;
    int breaches = 0;
    int documents = 0;
    for (ScreeningCase c :
        cases.findByStatusAndStageInAndDueAtIsNotNullOrderByDueAtAsc(CaseStatus.OPEN, MONITORED)) {
      boolean late = !now.isBefore(c.getDueAt());
      boolean inLead = c.getRemindAt() != null && !now.isBefore(c.getRemindAt());
      if (late && !c.isBreached()) {
        breach(c, now);
        breaches++;
      } else if (!late && inLead && c.getRemindedAt() == null) {
        remind(c, now);
        reminders++;
      }
      if ((late || inLead) && !today.equals(c.getDocumentRemindedOn()) && documents(c, today)) {
        documents++;
      }
    }
    return new Result(reminders, breaches, documents);
  }

  private void remind(ScreeningCase c, Instant now) {
    c.reminded(now);
    List<String> sent =
        notifier.owner(
            c,
            CaseAccess.ownerOf(c.getStage()),
            CaseCodes.EVENT_SLA_REMINDER,
            "due " + c.getDueAt() + " (" + c.getStage() + ")");
    timeline.record(
        c,
        CaseEventType.REMINDER,
        new EventFacts(
            c.getStage().name(),
            null,
            null,
            String.join(", ", sent),
            null,
            "SLA reminder; due " + c.getDueAt()));
  }

  private void breach(ScreeningCase c, Instant now) {
    String role =
        c.getEscalateToRole() == null ? CaseSla.DEFAULT_ESCALATION_ROLE : c.getEscalateToRole();
    List<String> sent =
        notifier.role(role, c, CaseCodes.EVENT_SLA_ESCALATION, "SLA breached in " + c.getStage());
    c.breach(now, role + (sent.isEmpty() ? "" : ": " + String.join(", ", sent)));
    alerts.raise(
        CaseCodes.ALERT_SLA_BREACH,
        new AlertFacts(
            c.getCompanyId(),
            null,
            CaseCodes.ENTITY,
            c.getCaseNo(),
            "Case "
                + c.getCaseNo()
                + " passed its SLA in "
                + c.getStage()
                + " (due "
                + c.getDueAt()
                + ")",
            null,
            CaseCodes.ALERT_SLA_BREACH
                + ":"
                + c.getCaseNo()
                + ":"
                + c.getStage()
                + ":"
                + c.getStageEnteredAt()));
    timeline.record(
        c,
        CaseEventType.BREACH,
        new EventFacts(
            c.getStage().name(),
            null,
            null,
            role,
            null,
            "SLA breached; due " + c.getDueAt() + "; escalated to " + c.getEscalatedTo()));
  }

  private boolean documents(ScreeningCase c, LocalDate today) {
    List<String> missing = validator.missingDocuments(c);
    if (missing.isEmpty()) {
      return false;
    }
    List<String> recipients = new ArrayList<>();
    if (c.getAssignee() != null) {
      recipients.add(c.getAssignee());
    } else {
      recipients.addAll(members.withPermission(CaseAccess.ownerOf(c.getStage())));
    }
    recipients.addAll(members.ofRole(CaseCodes.UCC_ROLE));
    String text = String.join("; ", missing);
    notifier.users(recipients, c, CaseCodes.EVENT_DOCUMENT_REMINDER, "documents missing: " + text);
    c.documentReminded(today);
    timeline.record(c, CaseEventType.DOCUMENT_REMINDER, EventFacts.remarks(text));
    return true;
  }

  /**
   * What an SLA check sent.
   *
   * @param reminders SLA reminders
   * @param breaches breaches flagged and escalated
   * @param documentReminders missing-document reminders
   */
  public record Result(int reminders, int breaches, int documentReminders) {}
}
