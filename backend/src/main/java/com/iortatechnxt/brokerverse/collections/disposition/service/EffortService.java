package com.iortatechnxt.brokerverse.collections.disposition.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.common.domain.FieldChange.Target;
import com.iortatechnxt.brokerverse.collections.common.service.ChangeRecorder;
import com.iortatechnxt.brokerverse.collections.common.service.CollectionItems;
import com.iortatechnxt.brokerverse.collections.disposition.domain.Effort;
import com.iortatechnxt.brokerverse.collections.disposition.domain.Effort.Facts;
import com.iortatechnxt.brokerverse.collections.disposition.domain.EffortRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Collection efforts and the collector's remarks and tagging category (p.40-46, p.59; BRCLXN.021,
 * 051): an effort of the active LOV {@code CLX_EFFORT_CODE} logged on one or several accounts
 * (several need {@code CLX_BULK_UPDATE}), the latest effort kept on the item for the reports, and
 * the remarks and category A / B / C kept up to date, each change logged field by field.
 */
@Service
@Transactional
public class EffortService {

  /** LOV type of the effort codes. */
  public static final String EFFORT_CODES = "CLX_EFFORT_CODE";

  private static final Set<String> CATEGORIES = Set.of("A", "B", "C");

  private final CollectionItems items;
  private final EffortRepository efforts;
  private final LovService lovs;
  private final ChangeRecorder changes;
  private final AuditTrailService audit;
  private final DispositionSupport support;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param items collection items
   * @param efforts efforts
   * @param lovs lists of values
   * @param changes change recorder
   * @param audit audit trail
   * @param support account checks
   * @param clock clock
   */
  public EffortService(
      CollectionItems items,
      EffortRepository efforts,
      LovService lovs,
      ChangeRecorder changes,
      AuditTrailService audit,
      DispositionSupport support,
      Clock clock) {
    this.items = items;
    this.efforts = efforts;
    this.lovs = lovs;
    this.changes = changes;
    this.audit = audit;
    this.support = support;
    this.clock = clock;
  }

  /**
   * Logs an effort on one or several accounts.
   *
   * @param companyId company
   * @param command accounts and what was done
   * @return efforts logged
   */
  public List<Effort> log(Long companyId, EffortCommand command) {
    LocalDate today = support.today();
    List<String> invoices =
        support.accounts(command.invoiceNos(), PrDispositionService.BULK_PERMISSION);
    lovs.requireValid(EFFORT_CODES, command.code(), today);
    Instant now = clock.instant();
    Instant at = command.at() == null ? now : command.at();
    if (at.isAfter(now)) {
      throw new BusinessRuleException("CLX_EFFORT_FUTURE", "An effort cannot be in the future");
    }
    String bulkRef = support.bulkRef(invoices.size(), today);
    Facts facts =
        new Facts(
            command.code(), at, command.channel(), command.contactPerson(), command.remarks());
    List<Effort> logged = new ArrayList<>();
    for (String invoiceNo : invoices) {
      CollectionItem item = support.workable(items.requireEditable(invoiceNo), companyId);
      String before = item.getLastEffortCode();
      Effort effort = efforts.save(new Effort(item.getCompanyId(), item.getId(), facts, bulkRef));
      item.effortMade(at, command.code());
      changes.record(target(item), "lastEffortCode", before, item.getLastEffortCode(), bulkRef);
      audit.record(
          CollectionItems.ENTITY,
          item.getInvoiceNo(),
          AuditAction.UPDATE,
          "Effort " + command.code() + (command.remarks() == null ? "" : ": " + command.remarks()));
      logged.add(effort);
    }
    return logged;
  }

  /**
   * Changes the remarks and tagging category of an account (p.41).
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param remarks remarks, blank to clear
   * @param category A, B or C; blank to clear
   * @return the item
   */
  public CollectionItem updateDetails(
      Long companyId, String invoiceNo, String remarks, String category) {
    String cat = category == null || category.isBlank() ? null : category.strip();
    if (cat != null && !CATEGORIES.contains(cat)) {
      throw new BusinessRuleException("CLX_CATEGORY", "The tagging category is A, B or C");
    }
    CollectionItem item = support.workable(items.requireEditable(invoiceNo), companyId);
    String remarksBefore = item.getRemarks();
    String categoryBefore = item.getCategory();
    String text = remarks == null || remarks.isBlank() ? null : remarks.strip();
    item.updateDetails(text, cat);
    boolean changed = changes.record(target(item), "remarks", remarksBefore, text, null);
    changed |= changes.record(target(item), "category", categoryBefore, cat, null);
    if (changed) {
      audit.record(
          CollectionItems.ENTITY, invoiceNo, AuditAction.UPDATE, "Remarks and category updated");
    }
    return item;
  }

  /**
   * The efforts of an item, latest first.
   *
   * @param itemId item
   * @return efforts
   */
  @Transactional(readOnly = true)
  public List<Effort> ofItem(Long itemId) {
    return efforts.findByItemIdOrderByEffortAtDescIdDesc(itemId);
  }

  private static Target target(CollectionItem item) {
    return new Target(
        item.getCompanyId(), CollectionItems.ENTITY, item.getInvoiceNo(), item.getId());
  }

  /**
   * An effort on one or several accounts.
   *
   * @param invoiceNos accounts
   * @param code LOV {@code CLX_EFFORT_CODE} code
   * @param at when it was made, null for now
   * @param channel channel
   * @param contactPerson person contacted
   * @param remarks remarks
   */
  public record EffortCommand(
      List<String> invoiceNos,
      String code,
      Instant at,
      String channel,
      String contactPerson,
      String remarks) {

    /** Defensive copy. */
    public EffortCommand {
      invoiceNos = invoiceNos == null ? List.of() : List.copyOf(invoiceNos);
    }
  }
}
