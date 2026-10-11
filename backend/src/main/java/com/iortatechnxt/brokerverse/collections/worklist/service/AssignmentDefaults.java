package com.iortatechnxt.brokerverse.collections.worklist.service;

import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.AssignmentKind;
import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.ItemStatus;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItemRepository;
import com.iortatechnxt.brokerverse.collections.common.service.ChangeRecorder;
import com.iortatechnxt.brokerverse.collections.worklist.domain.Assignment;
import com.iortatechnxt.brokerverse.collections.worklist.domain.Assignment.Handlers;
import com.iortatechnxt.brokerverse.collections.worklist.domain.Assignment.Terms;
import com.iortatechnxt.brokerverse.collections.worklist.domain.AssignmentRepository;
import com.iortatechnxt.brokerverse.collections.worklist.domain.AssignmentRule;
import com.iortatechnxt.brokerverse.collections.worklist.domain.AssignmentRuleRepository;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The automatic assignments of the daily refresh (BRCLXN.052): new open items without a handler go
 * to the handler of the first matching active rule, else to their account officer when the AO works
 * collections; temporary assignments whose end date has passed return the item to its previous
 * handler. Each change is logged field by field (BRCLXN.043).
 */
@Service
@Transactional
public class AssignmentDefaults {

  private final CollectionItemRepository items;
  private final AssignmentRepository assignments;
  private final AssignmentRuleRepository rules;
  private final UserDirectory users;
  private final ChangeRecorder changes;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param items items
   * @param assignments assignments
   * @param rules assignment rules
   * @param users user directory
   * @param changes change recorder
   * @param clock clock
   */
  public AssignmentDefaults(
      CollectionItemRepository items,
      AssignmentRepository assignments,
      AssignmentRuleRepository rules,
      UserDirectory users,
      ChangeRecorder changes,
      Clock clock) {
    this.items = items;
    this.assignments = assignments;
    this.rules = rules;
    this.users = users;
    this.changes = changes;
    this.clock = clock;
  }

  /**
   * Returns the items of the temporary assignments that ended before a day to their previous
   * handler (BRCLXN.052; daily refresh).
   *
   * @param companyId company
   * @param today business date
   * @return items returned
   */
  public int revertExpired(Long companyId, LocalDate today) {
    int reverted = 0;
    Instant now = clock.instant();
    for (Assignment temp :
        assignments.findByCompanyIdAndKindAndValidToBeforeAndRevertedAtIsNullOrderByIdAsc(
            companyId, AssignmentKind.TEMPORARY, today)) {
      temp.ended(now);
      boolean latest =
          assignments
              .findFirstByItemIdOrderByIdDesc(temp.getItemId())
              .map(a -> a.getId().equals(temp.getId()))
              .orElse(false);
      Optional<CollectionItem> item = items.findById(temp.getItemId());
      if (latest && item.isPresent()) {
        CollectionItem i = item.get();
        String back = temp.getPreviousHandler();
        i.assignTo(back);
        assignments.save(
            new Assignment(
                companyId,
                i.getId(),
                new Handlers(back, temp.getHandlerUsername()),
                new Terms(AssignmentKind.REVERT, today, null, "Temporary assignment ended"),
                CurrentUser.SYSTEM,
                null));
        changes.record(
            AssignmentService.target(i),
            AssignmentService.HANDLER_FIELD,
            temp.getHandlerUsername(),
            back,
            null);
        reverted++;
      }
    }
    return reverted;
  }

  /**
   * Assigns the open items without a handler by the first matching active rule, else to their
   * account officer when the AO works collections (BRCLXN.052; daily refresh).
   *
   * @param companyId company
   * @param today business date
   * @return items assigned
   */
  public int assignByRules(Long companyId, LocalDate today) {
    Defaults defaults = defaults(companyId);
    int assigned = 0;
    for (CollectionItem item :
        items.findByCompanyIdAndStatusAndCurrentHandlerIsNullOrderByIdAsc(
            companyId, ItemStatus.OPEN)) {
      if (assignDefault(item, defaults, today)) {
        assigned++;
      }
    }
    return assigned;
  }

  /**
   * Assigns one open item without a handler by rule, else to its account officer (BRCLXN.052).
   *
   * @param item item
   * @param today business date
   * @return true when assigned
   */
  public boolean assignByRules(CollectionItem item, LocalDate today) {
    return item.getStatus() == ItemStatus.OPEN
        && item.getCurrentHandler() == null
        && assignDefault(item, defaults(item.getCompanyId()), today);
  }

  private Defaults defaults(Long companyId) {
    return new Defaults(
        rules.findByCompanyIdAndActiveTrueOrderByPriorityAscIdAsc(companyId),
        users.usersWithPermission(AssignmentService.HANDLER_PERMISSION));
  }

  private boolean assignDefault(CollectionItem item, Defaults defaults, LocalDate today) {
    Optional<AssignmentRule> rule =
        defaults.rules().stream().filter(r -> r.matches(item)).findFirst();
    String ao = item.getClassification().aoUsername();
    String handler =
        rule.map(AssignmentRule::getHandlerUsername)
            .orElse(ao != null && AssignmentService.contains(defaults.handlers(), ao) ? ao : null);
    if (handler == null) {
      return false;
    }
    String reason = rule.map(r -> "Rule " + r.getName()).orElse("Account officer");
    item.assignTo(handler);
    assignments.save(
        new Assignment(
            item.getCompanyId(),
            item.getId(),
            new Handlers(handler, null),
            new Terms(AssignmentKind.RULE, today, null, reason),
            CurrentUser.SYSTEM,
            null));
    changes.record(
        AssignmentService.target(item), AssignmentService.HANDLER_FIELD, null, handler, null);
    return true;
  }

  private record Defaults(List<AssignmentRule> rules, List<String> handlers) {}
}
