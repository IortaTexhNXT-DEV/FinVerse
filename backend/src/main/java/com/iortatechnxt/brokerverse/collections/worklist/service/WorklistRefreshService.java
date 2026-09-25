package com.iortatechnxt.brokerverse.collections.worklist.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.service.SalesOrganisationService;
import com.iortatechnxt.brokerverse.collections.common.domain.AgingBrackets;
import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.ItemStatus;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem.Snapshot;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItemRepository;
import com.iortatechnxt.brokerverse.collections.common.domain.FieldChange.Target;
import com.iortatechnxt.brokerverse.collections.common.service.ChangeRecorder;
import com.iortatechnxt.brokerverse.collections.common.service.ClxSettings;
import com.iortatechnxt.brokerverse.collections.common.service.CollectionItems;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.LedgerSearch;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The refresh of the collection worklist from the Operations invoice ledger (BRCLXN.001-015, 022,
 * 046): new invoices above the threshold become items, listed items are updated (keys,
 * classification, Unit Head, figures, aging and bracket, balance per component) and completed,
 * reopened, excluded or moved to the credit view by the rules of {@link ItemSnapshots}. The full
 * refresh then returns ended temporary assignments and assigns the unassigned open items by rule
 * (BRCLXN.052). Each ledger page is refreshed in its own transaction.
 */
@Service
public class WorklistRefreshService {

  private static final int PAGE = 200;
  private static final String STATUS_FIELD = "status";

  private final InvoiceLedgerQueryService ledger;
  private final CollectionItemRepository items;
  private final ClxSettings settings;
  private final SalesOrganisationService sales;
  private final AssignmentDefaults assignments;
  private final ChangeRecorder changes;
  private final AuditTrailService audit;
  private final TransactionTemplate tx;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param ledger invoice ledger
   * @param items items
   * @param settings parameters
   * @param sales sales organisation (Unit Head)
   * @param assignments assignment
   * @param changes change recorder
   * @param audit audit trail
   * @param txManager transactions
   * @param clock clock
   */
  public WorklistRefreshService(
      InvoiceLedgerQueryService ledger,
      CollectionItemRepository items,
      ClxSettings settings,
      SalesOrganisationService sales,
      AssignmentDefaults assignments,
      ChangeRecorder changes,
      AuditTrailService audit,
      PlatformTransactionManager txManager,
      Clock clock) {
    this.ledger = ledger;
    this.items = items;
    this.settings = settings;
    this.sales = sales;
    this.assignments = assignments;
    this.changes = changes;
    this.audit = audit;
    this.tx = new TransactionTemplate(txManager);
    this.clock = clock;
  }

  /**
   * Refreshes every ledger invoice of a company (CLX_DAILY_REFRESH, BRCLXN.013-015), then reverts
   * ended temporary assignments and assigns by rule. Call outside a transaction.
   *
   * @param companyId company
   * @param today business date
   * @return counts per outcome
   */
  public RefreshOutcome refreshAll(Long companyId, LocalDate today) {
    Context ctx = context(today);
    Map<Change, Integer> counts = new EnumMap<>(Change.class);
    int page = 0;
    boolean more = true;
    while (more) {
      PageRequest request = PageRequest.of(page, PAGE, Sort.by("id"));
      Page<OpsInvoice> invoices =
          tx.execute(
              s -> {
                Page<OpsInvoice> p = ledger.searchLoaded(LedgerSearch.all(companyId), request);
                p.forEach(i -> counts.merge(apply(i, ctx), 1, Integer::sum));
                return p;
              });
      more = invoices != null && invoices.hasNext();
      page++;
    }
    Integer reverted = tx.execute(s -> assignments.revertExpired(companyId, today));
    Integer assigned = tx.execute(s -> assignments.assignByRules(companyId, today));
    return new RefreshOutcome(
        counts.getOrDefault(Change.CREATED, 0),
        counts.getOrDefault(Change.UPDATED, 0),
        counts.getOrDefault(Change.COMPLETED, 0) + counts.getOrDefault(Change.EXCLUDED, 0),
        counts.getOrDefault(Change.REOPENED, 0),
        reverted == null ? 0 : reverted,
        assigned == null ? 0 : assigned);
  }

  /**
   * Refreshes the item of one invoice between runs (balance listener, BRCLXN.015); invoices not
   * listed wait for the daily refresh.
   *
   * @param invoiceNo invoice
   * @return true when an item was refreshed
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean refreshListed(String invoiceNo) {
    return items.findFirstByInvoiceNo(invoiceNo).isPresent() && refresh(invoiceNo).isPresent();
  }

  /**
   * Refreshes one invoice by the rules of the daily refresh, listing it when it qualifies and
   * assigning it by rule (a refresh on demand of one account).
   *
   * @param invoiceNo invoice
   * @return the item, empty when the invoice is not listed
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Optional<CollectionItem> refreshInvoice(String invoiceNo) {
    Optional<CollectionItem> item = refresh(invoiceNo);
    item.ifPresent(i -> assignments.assignByRules(i, LocalDate.now(clock)));
    return item;
  }

  private Optional<CollectionItem> refresh(String invoiceNo) {
    Optional<OpsInvoice> invoice = ledger.find(invoiceNo);
    if (invoice.isEmpty()) {
      return Optional.empty();
    }
    OpsInvoice inv = invoice.get();
    inv.loadCollections();
    apply(inv, context(LocalDate.now(clock)));
    return items.findByCompanyIdAndInvoiceNo(inv.getCompanyId(), invoiceNo);
  }

  private Context context(LocalDate today) {
    return new Context(
        settings.threshold(), settings.brackets(), today, clock.instant(), new HashMap<>());
  }

  private Change apply(OpsInvoice invoice, Context ctx) {
    BigDecimal total = invoice.premiumBalance().add(ItemSnapshots.pr2307(invoice));
    ItemStatus status = ItemSnapshots.statusOf(invoice, total, ctx.threshold());
    Optional<CollectionItem> existing =
        items.findByCompanyIdAndInvoiceNo(invoice.getCompanyId(), invoice.getInvoiceNo());
    if (existing.isEmpty() && !ItemSnapshots.listable(status)) {
      return Change.SKIPPED;
    }
    Snapshot snapshot = snapshot(invoice, ctx);
    if (existing.isEmpty()) {
      CollectionItem created =
          items.save(
              new CollectionItem(
                  invoice.getCompanyId(),
                  invoice.getInvoiceNo(),
                  snapshot,
                  status,
                  ctx.now(),
                  ctx.today()));
      audit.record(
          CollectionItems.ENTITY,
          created.getInvoiceNo(),
          AuditAction.CREATE,
          "Listed " + status + " with " + total.toPlainString() + " to collect");
      return Change.CREATED;
    }
    CollectionItem item = existing.get();
    ItemStatus previous = item.refresh(snapshot, status, ctx.now(), ctx.today());
    if (previous == status) {
      return Change.UPDATED;
    }
    changes.record(
        new Target(item.getCompanyId(), CollectionItems.ENTITY, item.getInvoiceNo(), item.getId()),
        STATUS_FIELD,
        previous,
        status,
        null);
    audit.record(
        CollectionItems.ENTITY,
        item.getInvoiceNo(),
        AuditAction.UPDATE,
        "Status " + previous + " to " + status + " (" + total.toPlainString() + " to collect)");
    return switch (status) {
      case OPEN -> Change.REOPENED;
      case COMPLETED -> Change.COMPLETED;
      default -> Change.EXCLUDED;
    };
  }

  private Snapshot snapshot(OpsInvoice invoice, Context ctx) {
    String unit = invoice.getClassification().salesUnit();
    String head =
        unit == null
            ? null
            : ctx.heads()
                .computeIfAbsent(unit, u -> sales.unitHead(invoice.getCompanyId(), u).orElse(""));
    int age =
        settings.ageInDays(
            invoice.getClassification().bookingDate(),
            invoice.getClassification().inceptionDate(),
            ctx.today());
    return ItemSnapshots.of(
        invoice, head == null || head.isEmpty() ? null : head, age, ctx.brackets());
  }

  private enum Change {
    CREATED,
    UPDATED,
    COMPLETED,
    REOPENED,
    EXCLUDED,
    SKIPPED
  }

  private record Context(
      BigDecimal threshold,
      AgingBrackets brackets,
      LocalDate today,
      Instant now,
      Map<String, String> heads) {}

  /**
   * Counts of a refresh (job run log, the "synchronization view" of p.45-46).
   *
   * @param created invoices listed
   * @param updated items refreshed without a status change
   * @param closed items completed or excluded
   * @param reopened items reopened
   * @param reverted temporary assignments ended
   * @param assigned items assigned by rule
   */
  public record RefreshOutcome(
      int created, int updated, int closed, int reopened, int reverted, int assigned) {

    /**
     * The run message.
     *
     * @return message
     */
    public String message() {
      return created
          + " listed, "
          + updated
          + " refreshed, "
          + closed
          + " closed, "
          + reopened
          + " reopened, "
          + reverted
          + " temporary assignment(s) ended, "
          + assigned
          + " assigned by rule";
    }
  }
}
