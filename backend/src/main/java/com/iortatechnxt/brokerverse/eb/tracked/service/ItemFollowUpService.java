package com.iortatechnxt.brokerverse.eb.tracked.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeContact;
import com.iortatechnxt.brokerverse.eb.domain.EbResponsibleParty;
import com.iortatechnxt.brokerverse.eb.domain.EbTrackedItem;
import com.iortatechnxt.brokerverse.eb.domain.EbTrackedItemRepository;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One step of the job {@code EB_ITEM_FOLLOWUP} for one pending item past due (BRID-030; FR-EB-057):
 * the follow-up e-mail from template {@code EB_ITEM_FOLLOWUP} to the responsible party (the item's
 * recipients, else the insurer's placement mailboxes, the client's HR contacts or the AO), or,
 * after {@code EB_FOLLOWUP_MAX} follow-ups (or when nobody can be written to), the escalation:
 * alert {@code EB_ITEM_OVERDUE} and notice {@code EB_ITEM_ESCALATED} to the AO, once per item.
 */
@Service
@Transactional
public class ItemFollowUpService {

  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

  private final EbTrackedItemRepository items;
  private final EbRecords records;
  private final DocTemplateService templates;
  private final MessageService messages;
  private final NotificationService notifications;
  private final AlertService alerts;
  private final InsurerService insurers;
  private final AppUserRepository users;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param items tracked items
   * @param records programme look-up
   * @param templates follow-up template
   * @param messages e-mail outbox
   * @param notifications in-app notices
   * @param alerts alert engine
   * @param insurers insurer mailboxes
   * @param users AO name and e-mail
   * @param lovs item type labels
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ItemFollowUpService(
      EbTrackedItemRepository items,
      EbRecords records,
      DocTemplateService templates,
      MessageService messages,
      NotificationService notifications,
      AlertService alerts,
      InsurerService insurers,
      AppUserRepository users,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.items = items;
    this.records = records;
    this.templates = templates;
    this.messages = messages;
    this.notifications = notifications;
    this.alerts = alerts;
    this.insurers = insurers;
    this.users = users;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Follows up or escalates one item when a step is due.
   *
   * @param itemId item
   * @param today business date
   * @param schedule follow-up interval and maximum
   * @param working working-day calendar of the item's company
   * @return the step taken
   */
  public FollowUpSchedule.Step process(
      Long itemId, LocalDate today, FollowUpSchedule schedule, Predicate<LocalDate> working) {
    EbTrackedItem item = items.findById(itemId).orElseThrow();
    FollowUpSchedule.Step step =
        schedule.stepOn(
            item.getDueDate(),
            item.getFollowUpsSent(),
            item.getEscalatedAt() != null,
            today,
            working);
    if (step == FollowUpSchedule.Step.NONE) {
      return step;
    }
    EbProgramme programme = records.programme(item.getProgrammeId());
    List<String> to = recipients(item, programme);
    if (step == FollowUpSchedule.Step.FOLLOW_UP && !to.isEmpty()) {
      followUp(item, programme, to, today);
      return step;
    }
    escalate(item, programme, today);
    return FollowUpSchedule.Step.ESCALATE;
  }

  private void followUp(
      EbTrackedItem item, EbProgramme programme, List<String> to, LocalDate today) {
    Map<String, Object> values = new HashMap<>();
    values.put("recipientName", recipientName(item, programme));
    values.put("itemType", lovs.label(EbCodes.LOV_TRACKED_ITEM_TYPE, item.getItemType()));
    values.put("subject", item.getSubject());
    values.put("programmeName", programme.getName());
    values.put("dueDate", DATE.format(item.getDueDate()));
    values.put("followUpNo", item.getFollowUpsSent() + 1);
    values.put("aoName", aoName(programme));
    MergedText text = templates.merge(EbCodes.TEMPLATE_ITEM_FOLLOWUP, today, values);
    messages.queueEmail(
        new OutboundEmail(
            programme.getCompanyId(),
            EbCodes.PURPOSE_ITEM_FOLLOWUP,
            to,
            List.of(),
            DocTemplateService.fill(text.title(), values),
            text.text(),
            List.of(),
            null,
            new RecordLink(
                EbCodes.ENTITY_PROGRAMME,
                programme.getId().toString(),
                programme.getProgrammeNo())));
    item.followedUp(clock.instant());
    audit.record(
        EbCodes.ENTITY_TRACKED_ITEM,
        item.getId(),
        AuditAction.UPDATE,
        "Follow-up " + item.getFollowUpsSent() + " sent to " + String.join(", ", to));
  }

  private void escalate(EbTrackedItem item, EbProgramme programme, LocalDate today) {
    String message =
        programme.getProgrammeNo()
            + ": "
            + item.getSubject()
            + " is past due since "
            + DATE.format(item.getDueDate())
            + " after "
            + item.getFollowUpsSent()
            + " follow-up(s)";
    alerts.raise(
        EbCodes.ALERT_ITEM_OVERDUE,
        new AlertFacts(
            programme.getCompanyId(),
            null,
            EbCodes.ENTITY_PROGRAMME,
            programme.getId().toString(),
            message,
            null,
            EbCodes.ALERT_ITEM_OVERDUE + ":" + item.getId()));
    notifications.notifyUser(
        programme.getAccountOfficer(),
        new Notice(
            "Pending item escalated",
            message + ".",
            EbCodes.PROGRAMME_LINK + programme.getId() + "?tab=pending",
            EbCodes.ENTITY_PROGRAMME,
            programme.getId().toString()),
        EbCodes.EVENT_ITEM_ESCALATED);
    item.escalate(clock.instant());
    audit.record(
        EbCodes.ENTITY_TRACKED_ITEM, item.getId(), AuditAction.UPDATE, "Escalated on " + today);
  }

  private List<String> recipients(EbTrackedItem item, EbProgramme programme) {
    if (item.getRecipientEmail() != null && !item.getRecipientEmail().isBlank()) {
      return Arrays.stream(item.getRecipientEmail().split(","))
          .map(String::strip)
          .filter(s -> !s.isEmpty())
          .toList();
    }
    if (item.getResponsible() == EbResponsibleParty.INSURER && item.getPartyCode() != null) {
      return insurers.insurers(programme.getCompanyId()).stream()
          .filter(i -> item.getPartyCode().equals(i.getPartyCode()))
          .findFirst()
          .map(i -> i.getPlacementEmailList())
          .orElse(List.of());
    }
    if (item.getResponsible() == EbResponsibleParty.CLIENT) {
      return programme.getContacts().stream()
          .filter(EbProgrammeContact::isActive)
          .map(EbProgrammeContact::getEmail)
          .toList();
    }
    return users
        .findByUsernameIgnoreCase(programme.getAccountOfficer())
        .map(AppUser::getEmail)
        .filter(e -> e != null && !e.isBlank())
        .map(List::of)
        .orElse(List.of());
  }

  private String recipientName(EbTrackedItem item, EbProgramme programme) {
    return switch (item.getResponsible()) {
      case INSURER ->
          item.getPartyCode() == null
              ? "Sir / Madam"
              : insurers.insurers(programme.getCompanyId()).stream()
                  .filter(i -> item.getPartyCode().equals(i.getPartyCode()))
                  .findFirst()
                  .map(i -> i.getName())
                  .orElse(item.getPartyCode());
      case CLIENT -> programme.getClientName();
      case BDOI -> aoName(programme);
    };
  }

  private String aoName(EbProgramme programme) {
    return users
        .findByUsernameIgnoreCase(programme.getAccountOfficer())
        .map(AppUser::getFullName)
        .filter(n -> n != null && !n.isBlank())
        .orElse(programme.getAccountOfficer());
  }
}
