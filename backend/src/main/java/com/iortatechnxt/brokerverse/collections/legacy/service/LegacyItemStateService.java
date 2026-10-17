package com.iortatechnxt.brokerverse.collections.legacy.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItemRepository;
import com.iortatechnxt.brokerverse.collections.legacy.domain.LegacyState;
import com.iortatechnxt.brokerverse.collections.legacy.domain.LegacyStateRepository;
import com.iortatechnxt.brokerverse.collections.worklist.service.AssignmentService;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistRefreshService;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The open collection follow-up carried from legacy (Data Migration object F03; the designed
 * CLX_LEGACY_ITEMS of the Collections design): the rows are kept per legacy invoice and shown on
 * the account, and the item of the invoice is brought in step - listed if it qualifies, assigned to
 * the collector who followed it in legacy (without a notification), the latest legacy disposition
 * in its remarks and the promise flag set while a legacy promise is still ahead. Rolled back per
 * batch.
 */
@Service
@Transactional
public class LegacyItemStateService {

  /** Promise flag of an item with a promise carried from legacy. */
  public static final String LEGACY_PROMISE = "LEGACY";

  private static final String ASSIGNMENT = "ASSIGNMENT";
  private static final String DISPOSITION = "DISPOSITION";
  private static final String PROMISE = "PROMISE";
  private static final int MAX_REMARKS = 1000;

  private final LegacyStateRepository states;
  private final CollectionItemRepository items;
  private final WorklistRefreshService refresh;
  private final AssignmentService assignments;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param states legacy follow-up rows
   * @param items worklist items
   * @param refresh worklist refresh (lists the item of the invoice)
   * @param assignments assignments
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public LegacyItemStateService(
      LegacyStateRepository states,
      CollectionItemRepository items,
      WorklistRefreshService refresh,
      AssignmentService assignments,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.states = states;
    this.items = items;
    this.refresh = refresh;
    this.assignments = assignments;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Records the follow-up of a legacy invoice and brings its item in step.
   *
   * @param key invoice, legacy invoice, source system and batch
   * @param rows follow-up rows
   * @return the rows saved
   */
  public List<LegacyState> record(LegacyState.Key key, List<LegacyState.Row> rows) {
    Instant now = clock.instant();
    List<LegacyState> saved = new ArrayList<>();
    for (LegacyState.Row row : rows) {
      saved.add(states.save(new LegacyState(key, row, currentUser.username(), now)));
    }
    Optional<CollectionItem> item =
        items.findByCompanyIdAndInvoiceNo(key.companyId(), key.invoiceNo());
    if (item.isEmpty()) {
      refresh.refreshInvoice(key.invoiceNo());
      item = items.findByCompanyIdAndInvoiceNo(key.companyId(), key.invoiceNo());
    }
    item.ifPresent(i -> apply(i, rows, key.migrationBatch()));
    audit.record(
        "CollectionItem",
        key.invoiceNo(),
        AuditAction.UPDATE,
        rows.size() + " follow-up row(s) carried from legacy (batch " + key.migrationBatch() + ")");
    return saved;
  }

  private void apply(CollectionItem item, List<LegacyState.Row> rows, String batch) {
    rows.stream()
        .filter(r -> ASSIGNMENT.equals(r.recordType()) && r.collector() != null)
        .max(Comparator.comparingInt(LegacyState.Row::seqNo))
        .ifPresent(
            r ->
                assignments.assignCarried(
                    item, r.collector(), "Collector in legacy (batch " + batch + ")"));
    rows.stream()
        .filter(r -> DISPOSITION.equals(r.recordType()))
        .max(Comparator.comparingInt(LegacyState.Row::seqNo))
        .ifPresent(r -> item.updateDetails(remarks(r), item.getCategory()));
    LocalDate today = BusinessClock.today(clock);
    boolean promiseAhead =
        rows.stream()
            .anyMatch(
                r ->
                    PROMISE.equals(r.recordType())
                        && r.promiseDate() != null
                        && !r.promiseDate().isBefore(today));
    if (promiseAhead) {
      item.flagPromise(LEGACY_PROMISE);
    }
  }

  private static String remarks(LegacyState.Row r) {
    String text =
        "Legacy disposition "
            + Objects.toString(r.dispositionCode(), "")
            + (r.dispositionDate() == null ? "" : " on " + r.dispositionDate())
            + (r.remarks() == null ? "" : ": " + r.remarks());
    return text.length() > MAX_REMARKS ? text.substring(0, MAX_REMARKS) : text;
  }

  /**
   * The live follow-up of an invoice (account page).
   *
   * @param companyId company
   * @param invoiceNo ledger invoice
   * @return rows
   */
  @Transactional(readOnly = true)
  public List<LegacyState> of(Long companyId, String invoiceNo) {
    return states.findByCompanyIdAndInvoiceNoAndRolledBackAtIsNullOrderByIdAsc(
        companyId, invoiceNo);
  }

  /**
   * Undoes the follow-up of a rolled-back batch (the rows are kept, marked rolled back).
   *
   * @param batchNo batch
   * @return rows rolled back
   */
  public int rollback(String batchNo) {
    Instant now = clock.instant();
    int n = 0;
    for (LegacyState s : states.findByMigrationBatch(batchNo)) {
      if (s.getRolledBackAt() == null) {
        s.rollBack(now);
        n++;
      }
    }
    return n;
  }
}
