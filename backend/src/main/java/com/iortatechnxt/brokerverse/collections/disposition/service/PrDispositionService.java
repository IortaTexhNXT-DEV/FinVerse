package com.iortatechnxt.brokerverse.collections.disposition.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.DispositionSource;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.common.domain.FieldChange.Target;
import com.iortatechnxt.brokerverse.collections.common.service.ChangeRecorder;
import com.iortatechnxt.brokerverse.collections.common.service.CollectionItems;
import com.iortatechnxt.brokerverse.collections.common.service.LovAttributes;
import com.iortatechnxt.brokerverse.collections.common.service.LovAttributes.DispositionRule;
import com.iortatechnxt.brokerverse.collections.disposition.domain.PrDisposition;
import com.iortatechnxt.brokerverse.collections.disposition.domain.PrDisposition.Details;
import com.iortatechnxt.brokerverse.collections.disposition.domain.PrDisposition.Tag;
import com.iortatechnxt.brokerverse.collections.disposition.domain.PrDispositionRepository;
import com.iortatechnxt.brokerverse.collections.disposition.service.HandOffs.Context;
import com.iortatechnxt.brokerverse.collections.feed.domain.OutboxItem;
import com.iortatechnxt.brokerverse.collections.feed.service.InboxService.InboxDispositions;
import com.iortatechnxt.brokerverse.collections.feed.service.OutboxService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PR collector dispositions (BRCLXN.016-023, 051; p.40-42): a disposition of the active LOV {@code
 * CLX_PR_DISPOSITION} on one or several accounts (several need {@code CLX_BULK_UPDATE} and share a
 * bulk reference), restricted by the value's {@code allowed_roles}, with the hand-off to Operations
 * of its {@code ops_action} through the outbox. Dispositions are append-only: the new one becomes
 * current, the previous one is superseded and a hand-off it queued but that was not taken yet is
 * withdrawn. Category and tagging owner follow the value's attributes.
 */
@Service
@Transactional
public class PrDispositionService implements InboxDispositions {

  static final String BULK_PERMISSION = "CLX_BULK_UPDATE";
  private static final String HEAD_OFFICE = "HO";

  private final CollectionItems items;
  private final PrDispositionRepository dispositions;
  private final LovAttributes attributes;
  private final LovService lovs;
  private final OutboxService outbox;
  private final UserDirectory users;
  private final OrganizationService organization;
  private final ChangeRecorder changes;
  private final AuditTrailService audit;
  private final DispositionSupport support;

  /**
   * Creates the service.
   *
   * @param items collection items
   * @param dispositions dispositions
   * @param attributes LOV attributes
   * @param lovs lists of values
   * @param outbox outbox
   * @param users user directory (roles)
   * @param organization branches
   * @param changes change recorder
   * @param audit audit trail
   * @param support user, clock, JSON and bulk references
   */
  public PrDispositionService(
      CollectionItems items,
      PrDispositionRepository dispositions,
      LovAttributes attributes,
      LovService lovs,
      OutboxService outbox,
      UserDirectory users,
      OrganizationService organization,
      ChangeRecorder changes,
      AuditTrailService audit,
      DispositionSupport support) {
    this.items = items;
    this.dispositions = dispositions;
    this.attributes = attributes;
    this.lovs = lovs;
    this.outbox = outbox;
    this.users = users;
    this.organization = organization;
    this.changes = changes;
    this.audit = audit;
    this.support = support;
  }

  /**
   * Records a disposition on one or several accounts (BRCLXN.016-023, 051).
   *
   * @param companyId company
   * @param command accounts, code, remarks, effective date and hand-off details
   * @return the dispositions recorded
   */
  public List<PrDisposition> record(Long companyId, DispositionCommand command) {
    LocalDate today = support.today();
    List<String> invoices = support.accounts(command.invoiceNos(), BULK_PERMISSION);
    lovs.requireValid(LovAttributes.PR_DISPOSITION, command.code(), today);
    DispositionRule rule = attributes.prRule(command.code());
    requireAllowed(rule);
    HandOffs.validate(rule.opsAction(), command.details(), today);
    String bulkRef = support.bulkRef(invoices.size(), today);
    DispositionSource source = bulkRef == null ? DispositionSource.USER : DispositionSource.BULK;
    LocalDate effective = command.effectiveOn() == null ? today : command.effectiveOn();
    List<PrDisposition> recorded = new ArrayList<>();
    for (String invoiceNo : invoices) {
      CollectionItem item = support.workable(items.requireEditable(invoiceNo), companyId);
      recorded.add(
          apply(
              item,
              rule,
              new Details(
                  command.remarks(), effective, support.json(command.details()), source, bulkRef),
              command.details()));
    }
    return recorded;
  }

  /**
   * Records a disposition set by the system (e.g. "DP returned by insurer" from the inbox,
   * CMRID.009): no role restriction, no hand-off details.
   *
   * @param item the item, locked by the caller
   * @param code disposition code
   * @param remarks remarks
   * @return the disposition
   */
  public PrDisposition recordFromInbox(CollectionItem item, String code, String remarks) {
    return apply(
        item,
        attributes.prRule(code),
        new Details(remarks, support.today(), null, DispositionSource.INBOX, null),
        Map.of());
  }

  @Override
  public void record(CollectionItem item, String code, String remarks) {
    recordFromInbox(item, code, remarks);
  }

  private PrDisposition apply(
      CollectionItem item, DispositionRule rule, Details details, Map<String, String> handOff) {
    PrDisposition saved =
        dispositions.save(
            new PrDisposition(
                item.getCompanyId(),
                item.getId(),
                new Tag(rule.code(), rule.category(), rule.taggingOwner(), rule.opsAction()),
                details));
    supersedeCurrent(item, saved.getId());
    String codeBefore = item.getDispositionCode();
    String categoryBefore = item.getCategory();
    Object ownerBefore = item.getTaggingOwner();
    item.disposed(saved.getId(), rule.code(), rule.category(), rule.taggingOwner());
    Target target =
        new Target(item.getCompanyId(), CollectionItems.ENTITY, item.getInvoiceNo(), item.getId());
    changes.record(target, "dispositionCode", codeBefore, rule.code(), details.bulkRef());
    changes.record(target, "category", categoryBefore, item.getCategory(), details.bulkRef());
    changes.record(target, "taggingOwner", ownerBefore, item.getTaggingOwner(), details.bulkRef());
    HandOffs.of(
            item,
            rule.opsAction(),
            item.getTaggingOwner(),
            new Context(
                saved.getId(),
                details.remarks(),
                handOff,
                support.username(),
                branchCode(item.getClassification().branchId())))
        .ifPresent(
            q -> {
              OutboxItem queued =
                  outbox.queue(
                      item.getCompanyId(), q.key(), item.getInvoiceNo(), q.fields(), saved.getId());
              saved.queued(queued.getId());
            });
    audit.record(
        CollectionItems.ENTITY,
        item.getInvoiceNo(),
        AuditAction.UPDATE,
        "Disposition " + rule.code() + (details.remarks() == null ? "" : ": " + details.remarks()));
    return saved;
  }

  private void supersedeCurrent(CollectionItem item, Long newId) {
    Long current = item.getCurrentDispositionId();
    if (current == null) {
      return;
    }
    dispositions
        .findById(current)
        .ifPresent(
            previous -> {
              previous.supersede(newId);
              if (previous.getOutboxId() != null) {
                outbox.cancel(previous.getOutboxId());
              }
            });
  }

  private void requireAllowed(DispositionRule rule) {
    if (rule.allowedRoles().isEmpty()) {
      return;
    }
    Set<String> roles = users.roleCodes(support.username());
    if (Collections.disjoint(roles, rule.allowedRoles())) {
      throw new BusinessRuleException(
          "CLX_DISPOSITION_NOT_ALLOWED",
          "Disposition "
              + rule.code()
              + " is reserved to "
              + String.join(", ", rule.allowedRoles()));
    }
  }

  private String branchCode(Long branchId) {
    if (branchId == null) {
      return HEAD_OFFICE;
    }
    Branch branch = organization.getBranch(branchId);
    return branch.isHeadOffice() ? HEAD_OFFICE : branch.getCode();
  }

  /**
   * The dispositions of an item, newest first.
   *
   * @param itemId item
   * @return dispositions
   */
  @Transactional(readOnly = true)
  public List<PrDisposition> ofItem(Long itemId) {
    return dispositions.findByItemIdOrderByIdDesc(itemId);
  }

  /**
   * A disposition on one or several accounts.
   *
   * @param invoiceNos accounts (invoice numbers)
   * @param code LOV {@code CLX_PR_DISPOSITION} code
   * @param remarks remarks
   * @param effectiveOn date the disposition applies from, null for today
   * @param details hand-off details (pick-up date and address, certificate number ...)
   */
  public record DispositionCommand(
      List<String> invoiceNos,
      String code,
      String remarks,
      LocalDate effectiveOn,
      Map<String, String> details) {

    /** Defensive copies. */
    public DispositionCommand {
      invoiceNos = invoiceNos == null ? List.of() : List.copyOf(invoiceNos);
      details = details == null ? Map.of() : Map.copyOf(details);
    }
  }
}
