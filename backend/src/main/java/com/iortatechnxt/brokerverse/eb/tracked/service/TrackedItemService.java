package com.iortatechnxt.brokerverse.eb.tracked.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.EmailAddresses;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbResponsibleParty;
import com.iortatechnxt.brokerverse.eb.domain.EbTrackedItem;
import com.iortatechnxt.brokerverse.eb.domain.EbTrackedItemRepository;
import com.iortatechnxt.brokerverse.eb.domain.TatActivity;
import com.iortatechnxt.brokerverse.eb.service.EbParameters;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.eb.service.EbWorkingDays;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tracked items of the EB programmes (BRID-030; FR-EB-057): the contract of each account created at
 * placement (opened automatically, owed by the insurer within {@code EB_TAT_POLICY_SOA} working
 * days) and the HMO cards, card replacements and billings the EB users open, each with the
 * responsible party, due date and follow-up recipients; then received, released or closed (closing
 * a pending item needs its date received). Follow-ups and escalation are the job {@code
 * EB_ITEM_FOLLOWUP}.
 */
@Service
@Transactional
public class TrackedItemService {

  /** Item type of the contract after placement. */
  public static final String CONTRACT = "CONTRACT";

  /** Longest subject of an item. */
  static final int MAX_SUBJECT = 200;

  private final EbTrackedItemRepository items;
  private final EbRecords records;
  private final EbParameters parameters;
  private final EbWorkingDays workingDays;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param items tracked items
   * @param records programme and cycle look-up
   * @param parameters TAT targets
   * @param workingDays working-day calendar
   * @param lovs item types
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public TrackedItemService(
      EbTrackedItemRepository items,
      EbRecords records,
      EbParameters parameters,
      EbWorkingDays workingDays,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.items = items;
    this.records = records;
    this.parameters = parameters;
    this.workingDays = workingDays;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Opens an item on a programme.
   *
   * @param companyId company
   * @param programmeId programme
   * @param input type, cycle and what is expected
   * @return the item
   */
  public EbTrackedItem open(Long companyId, Long programmeId, ItemInput input) {
    EbProgramme programme = records.programme(companyId, programmeId);
    Long cycleId = null;
    if (input.cycleId() != null) {
      cycleId = records.cycle(companyId, input.cycleId()).getId();
    }
    if (input.itemType() == null || input.itemType().isBlank()) {
      throw new BusinessRuleException("EB_ITEM_TYPE_REQUIRED", "Select the item type");
    }
    lovs.requireValid(EbCodes.LOV_TRACKED_ITEM_TYPE, input.itemType(), today());
    check(input.details());
    EbTrackedItem item =
        items.save(
            new EbTrackedItem(
                companyId, programme.getId(), cycleId, input.itemType(), input.details()));
    audit.record(
        EbCodes.ENTITY_TRACKED_ITEM,
        item.getId(),
        AuditAction.CREATE,
        programme.getProgrammeNo() + ": " + item.getSubject() + " due " + item.getDueDate());
    return item;
  }

  /**
   * Opens the contract item of an account created at placement (one per account).
   *
   * @param cycle cycle
   * @param arn account
   * @param insurerCode insurer that issues the contract
   * @param productLabel product or benefit line, for the subject
   */
  public void openContract(EbCycle cycle, String arn, String insurerCode, String productLabel) {
    if (items.existsByItemTypeAndAccountArn(CONTRACT, arn)) {
      return;
    }
    LocalDate due =
        workingDays.plus(cycle.getCompanyId(), today(), parameters.tatDays(TatActivity.POLICY_SOA));
    EbTrackedItem item =
        items.save(
            new EbTrackedItem(
                cycle.getCompanyId(),
                cycle.getProgrammeId(),
                cycle.getId(),
                CONTRACT,
                new EbTrackedItem.Details(
                    "Contract of " + arn + " (" + productLabel + ")",
                    null,
                    null,
                    arn,
                    EbResponsibleParty.INSURER,
                    insurerCode,
                    null,
                    due,
                    null)));
    audit.record(
        EbCodes.ENTITY_TRACKED_ITEM,
        item.getId(),
        AuditAction.CREATE,
        cycle.getCycleNo() + ": " + item.getSubject() + " due " + due);
  }

  /**
   * Changes a pending item.
   *
   * @param companyId company
   * @param itemId item
   * @param details what is expected
   * @return the item
   */
  public EbTrackedItem update(Long companyId, Long itemId, EbTrackedItem.Details details) {
    EbTrackedItem item = require(companyId, itemId);
    check(details);
    item.update(details);
    audit.record(
        EbCodes.ENTITY_TRACKED_ITEM,
        item.getId(),
        AuditAction.UPDATE,
        item.getSubject() + " due " + item.getDueDate());
    return item;
  }

  /**
   * Moves an item on: received, released or closed.
   *
   * @param companyId company
   * @param itemId item
   * @param change new status, dates and remarks
   * @return the item
   */
  public EbTrackedItem change(Long companyId, Long itemId, StatusChange change) {
    EbTrackedItem item = require(companyId, itemId);
    LocalDate today = today();
    if (change.date() != null && change.date().isAfter(today)) {
      throw new BusinessRuleException("EB_ITEM_DATE_FUTURE", "The date cannot be after today");
    }
    LocalDate on = change.date() == null ? today : change.date();
    if (change.action() == Action.RECEIVE) {
      item.receive(change.date(), change.remarks());
    } else if (change.action() == Action.RELEASE) {
      item.release(on, change.remarks());
    } else {
      item.close(on, change.receivedOn(), change.remarks());
    }
    audit.record(
        EbCodes.ENTITY_TRACKED_ITEM,
        item.getId(),
        AuditAction.UPDATE,
        item.getSubject() + ": " + item.getStatus() + " on " + on);
    return item;
  }

  private void check(EbTrackedItem.Details details) {
    checkSubject(details.subject());
    if (details.responsible() == null) {
      throw new BusinessRuleException(
          "EB_ITEM_RESPONSIBLE_REQUIRED", "Select who is responsible for the item");
    }
    if (details.dueDate() == null) {
      throw new BusinessRuleException("EB_ITEM_DUE_REQUIRED", "Enter the due date");
    }
    checkRecipients(details.recipientEmail());
  }

  private static void checkSubject(String subject) {
    if (subject == null || subject.isBlank()) {
      throw new BusinessRuleException("EB_ITEM_SUBJECT_REQUIRED", "Enter what is expected");
    }
    if (subject.length() > MAX_SUBJECT) {
      throw new BusinessRuleException(
          "EB_ITEM_SUBJECT_TOO_LONG",
          "The description can have at most " + MAX_SUBJECT + " characters");
    }
  }

  private static void checkRecipients(String recipients) {
    if (recipients == null || recipients.isBlank()) {
      return;
    }
    boolean valid =
        Arrays.stream(recipients.split(",")).map(String::strip).allMatch(EmailAddresses::isValid);
    if (!valid) {
      throw new BusinessRuleException("EMAIL_ADDRESS_INVALID", "Enter a valid e-mail address");
    }
  }

  /**
   * An item of a company.
   *
   * @param companyId company
   * @param itemId item
   * @return the item
   */
  @Transactional(readOnly = true)
  public EbTrackedItem require(Long companyId, Long itemId) {
    return items
        .findById(itemId)
        .filter(i -> i.getCompanyId().equals(companyId))
        .orElseThrow(() -> new ResourceNotFoundException(EbCodes.ENTITY_TRACKED_ITEM, itemId));
  }

  private LocalDate today() {
    return BusinessClock.today(clock);
  }

  /**
   * An item to open.
   *
   * @param itemType type (list EB_TRACKED_ITEM_TYPE)
   * @param cycleId cycle, may be null
   * @param details what is expected, from whom and by when
   */
  public record ItemInput(String itemType, Long cycleId, EbTrackedItem.Details details) {}

  /** How an item moves on. */
  public enum Action {
    /** Received from the responsible party. */
    RECEIVE,
    /** Released to the client or member. */
    RELEASE,
    /** Closed. */
    CLOSE
  }

  /**
   * A status change.
   *
   * @param action receive, release or close
   * @param date date of the change (received, released or closed); today when null except for
   *     receive, which needs it
   * @param receivedOn date received, when closing a pending item
   * @param remarks remarks, may be null
   */
  public record StatusChange(Action action, LocalDate date, LocalDate receivedOn, String remarks) {}
}
