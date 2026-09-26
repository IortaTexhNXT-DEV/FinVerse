package com.iortatechnxt.brokerverse.collections.worklist.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.AssignmentKind;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItemRepository;
import com.iortatechnxt.brokerverse.collections.common.domain.FieldChange.Target;
import com.iortatechnxt.brokerverse.collections.common.service.ChangeRecorder;
import com.iortatechnxt.brokerverse.collections.common.service.CollectionItems;
import com.iortatechnxt.brokerverse.collections.worklist.domain.Assignment;
import com.iortatechnxt.brokerverse.collections.worklist.domain.Assignment.Handlers;
import com.iortatechnxt.brokerverse.collections.worklist.domain.Assignment.Terms;
import com.iortatechnxt.brokerverse.collections.worklist.domain.AssignmentRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assignment of collection items (BRCLXN.052): default assignment by rule of the unassigned open
 * items, reassignment of selected items or by criteria (permanent, or temporary with an end date)
 * with a preview, the automatic return at the end of a temporary assignment, and the history of an
 * item. Every change is logged field by field and audited; the new handler is notified ({@code
 * CLX_REASSIGNED}).
 */
@Service
@Transactional
public class AssignmentService {

  /** Permission of the handlers who may receive accounts. */
  public static final String HANDLER_PERMISSION = "CLX_WORK";

  private static final String REASSIGNED = "CLX_REASSIGNED";
  static final String HANDLER_FIELD = "currentHandler";
  private static final int MAX_SELECTION = 2000;
  private static final int PREVIEW_ROWS = 200;

  private final CollectionItemRepository items;
  private final AssignmentRepository assignments;
  private final UserDirectory users;
  private final NotificationService notifications;
  private final ChangeRecorder changes;
  private final AuditTrailService audit;
  private final DocumentNumberService numbers;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param items items
   * @param assignments assignments
   * @param users user directory
   * @param notifications notifications
   * @param changes change recorder
   * @param audit audit trail
   * @param numbers bulk references
   * @param currentUser signed-in user
   * @param clock clock
   */
  public AssignmentService(
      CollectionItemRepository items,
      AssignmentRepository assignments,
      UserDirectory users,
      NotificationService notifications,
      ChangeRecorder changes,
      AuditTrailService audit,
      DocumentNumberService numbers,
      CurrentUser currentUser,
      Clock clock) {
    this.items = items;
    this.assignments = assignments;
    this.users = users;
    this.notifications = notifications;
    this.changes = changes;
    this.audit = audit;
    this.numbers = numbers;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The items a reassignment would move (preview, BRCLXN.052).
   *
   * @param companyId company
   * @param selection selected invoices, or criteria when no invoice is given
   * @return up to 200 items
   */
  @Transactional(readOnly = true)
  public Selection preview(Long companyId, ItemSelection selection) {
    List<CollectionItem> selected = select(companyId, selection);
    return new Selection(
        selected.size(), selected.subList(0, Math.min(selected.size(), PREVIEW_ROWS)));
  }

  /**
   * Reassigns the selected items (BRCLXN.052): permanent, or temporary until an end date after
   * which the daily refresh returns them to their handler.
   *
   * @param companyId company
   * @param selection selected invoices or criteria
   * @param command new handler, kind, end date and reason
   * @return the bulk reference and the items moved
   */
  public Reassigned reassign(Long companyId, ItemSelection selection, Reassign command) {
    LocalDate today = LocalDate.now(clock);
    validate(command, today);
    boolean temporary = command.kind() == AssignmentKind.TEMPORARY;
    List<CollectionItem> selected = select(companyId, selection);
    if (selected.isEmpty()) {
      throw new BusinessRuleException("CLX_REASSIGN_EMPTY", "No account matches the selection");
    }
    String bulkRef = selected.size() > 1 ? numbers.next("CLXRA-" + today.getYear()) : null;
    Terms terms =
        new Terms(command.kind(), today, temporary ? command.validTo() : null, command.reason());
    Map<String, Integer> taken = new LinkedHashMap<>();
    for (CollectionItem item : selected) {
      String previous = move(item, command.handler(), terms, bulkRef);
      if (previous != null && !CurrentUser.sameUser(previous, command.handler())) {
        taken.merge(previous, 1, Integer::sum);
      }
    }
    notifyHandlers(command.handler(), selected.size(), taken, terms);
    return new Reassigned(bulkRef, selected.size());
  }

  private void validate(Reassign command, LocalDate today) {
    requireHandler(command.handler());
    if (command.reason() == null || command.reason().isBlank()) {
      throw new BusinessRuleException("CLX_REASSIGN_REASON", "Give the reason of the reassignment");
    }
    switch (command.kind()) {
      case PERMANENT -> {
        // no end date
      }
      case TEMPORARY -> requireEndDate(command.validTo(), today);
      default ->
          throw new BusinessRuleException(
              "CLX_REASSIGN_KIND", "A reassignment is PERMANENT or TEMPORARY");
    }
  }

  private static void requireEndDate(LocalDate validTo, LocalDate today) {
    if (validTo == null || !validTo.isAfter(today)) {
      throw new BusinessRuleException(
          "CLX_REASSIGN_END_DATE", "A temporary reassignment needs an end date after today");
    }
  }

  private String move(CollectionItem item, String handler, Terms terms, String bulkRef) {
    Instant now = clock.instant();
    assignments
        .findFirstByItemIdOrderByIdDesc(item.getId())
        .filter(a -> a.getKind() == AssignmentKind.TEMPORARY)
        .ifPresent(a -> a.ended(now));
    String previous = item.assignTo(handler);
    assignments.save(
        new Assignment(
            item.getCompanyId(),
            item.getId(),
            new Handlers(handler, previous),
            terms,
            currentUser.username(),
            bulkRef));
    changes.record(target(item), HANDLER_FIELD, previous, handler, bulkRef);
    audit.record(
        CollectionItems.ENTITY,
        item.getInvoiceNo(),
        AuditAction.UPDATE,
        terms.kind() + " reassignment from " + previous + " to " + handler + ": " + terms.reason());
    return previous;
  }

  private void notifyHandlers(String handler, int count, Map<String, Integer> taken, Terms terms) {
    String until = terms.validTo() == null ? "" : " until " + terms.validTo();
    notifications.notifyUser(
        handler,
        new Notice(
            count + " collection account(s) assigned to you" + until,
            terms.reason(),
            "/collections/worklist?mine=true",
            CollectionItems.ENTITY,
            null),
        REASSIGNED);
    taken.forEach(
        (user, n) ->
            notifications.notifyUser(
                user,
                new Notice(
                    n + " collection account(s) reassigned to " + handler + until,
                    terms.reason(),
                    "/collections/worklist",
                    CollectionItems.ENTITY,
                    null),
                REASSIGNED));
  }

  /**
   * The assignment history of an item, newest first.
   *
   * @param itemId item
   * @return assignments
   */
  @Transactional(readOnly = true)
  public List<Assignment> history(Long itemId) {
    return assignments.findByItemIdOrderByIdDesc(itemId);
  }

  /**
   * Users who may handle collection accounts (assignee pick list).
   *
   * @return user names
   */
  @Transactional(readOnly = true)
  public List<String> handlers() {
    return users.usersWithPermission(HANDLER_PERMISSION);
  }

  /**
   * Refuses a handler without the collections work permission.
   *
   * @param handler user name
   */
  public void requireHandler(String handler) {
    if (handler == null || !contains(users.usersWithPermission(HANDLER_PERMISSION), handler)) {
      throw new BusinessRuleException(
          "CLX_HANDLER_NOT_ELIGIBLE", handler + " is not an active collection handler");
    }
  }

  private List<CollectionItem> select(Long companyId, ItemSelection selection) {
    List<CollectionItem> selected = new ArrayList<>();
    if (selection.invoiceNos() != null && !selection.invoiceNos().isEmpty()) {
      selected.addAll(items.findByCompanyIdAndInvoiceNoIn(companyId, selection.invoiceNos()));
    } else if (selection.criteria() != null) {
      selected.addAll(
          items
              .findAll(
                  WorklistSpecifications.of(companyId, selection.criteria()),
                  PageRequest.of(0, MAX_SELECTION, Sort.by("id")))
              .getContent());
    }
    return selected;
  }

  static boolean contains(List<String> users, String user) {
    return users.stream().anyMatch(u -> CurrentUser.sameUser(u, user));
  }

  static Target target(CollectionItem item) {
    return new Target(
        item.getCompanyId(), CollectionItems.ENTITY, item.getInvoiceNo(), item.getId());
  }

  /**
   * Which items to reassign.
   *
   * @param invoiceNos selected invoices; when empty the criteria apply
   * @param criteria worklist filters
   */
  public record ItemSelection(List<String> invoiceNos, WorklistFilter criteria) {

    /** Defensive copy. */
    public ItemSelection {
      invoiceNos = invoiceNos == null ? List.of() : List.copyOf(invoiceNos);
    }
  }

  /**
   * A reassignment.
   *
   * @param handler new handler
   * @param kind PERMANENT or TEMPORARY
   * @param validTo last day of a temporary reassignment
   * @param reason reason
   */
  public record Reassign(String handler, AssignmentKind kind, LocalDate validTo, String reason) {}

  /**
   * Items a reassignment would move.
   *
   * @param total items selected
   * @param items the first items
   */
  public record Selection(int total, List<CollectionItem> items) {

    /** Defensive copy. */
    public Selection {
      items = List.copyOf(items);
    }
  }

  /**
   * Result of a reassignment.
   *
   * @param bulkRef bulk reference, null for one item
   * @param moved items moved
   */
  public record Reassigned(String bulkRef, int moved) {}
}
