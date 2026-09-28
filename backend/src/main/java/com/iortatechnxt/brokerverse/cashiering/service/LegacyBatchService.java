package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.LegacyBatch;
import com.iortatechnxt.brokerverse.cashiering.domain.LegacyBatchLine;
import com.iortatechnxt.brokerverse.cashiering.domain.LegacyBatchLineRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.LegacyBatchRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The Cashiering batches of the legacy context (DATA_MIGRATION_DESIGN 14.4 D and F):
 *
 * <ul>
 *   <li>income reclassification: old unapplied payments, new or legacy, taken to other income -
 *       unclaimed collections; requested by the Cashiering user, approved by the Cashiering team
 *       lead and then by top management (BRID 5.5);
 *   <li>legacy PR 2307 reversal: the PR 2307 balances of legacy invoices settled against the
 *       insurer (reclassified from the premium receivable first when needed); requested by the
 *       Cashiering user, approved by the team lead (BRID 7.2).
 * </ul>
 *
 * <p>The maker never approves, a return needs its reason, and each line posts in its own
 * transaction after the last approval.
 */
@Service
@Transactional
@SuppressWarnings("PMD.GodClass") // the two batch kinds share the same maker-checker life cycle
public class LegacyBatchService {

  private static final String ENTITY = "CashLegacyBatch";
  private static final Set<String> OPEN_STAGES = Set.of(Unapplied.STAGE_INITIAL, "MONITORING");

  /**
   * The day an unapplied item was received: the legacy acknowledgement receipt date of a migrated
   * item (received in legacy, loaded at cut-over), else the day it was created in BIBS.
   */
  private static final String RECEIVED_ON =
      "coalesce(u.legacy_ar_date, cast(u.created_at at time zone 'Asia/Manila' as date))";

  private static final String CANDIDATES =
      "select u.id, u.reference, u.origin, u.ledger_context, u.legacy_ar_no, u.payor_name,"
          + " u.client_code, u.currency, u.balance, u.stage,"
          + " cast(? as date) - "
          + RECEIVED_ON
          + " as age_days"
          + " from csh_unapplied u where u.company_id = ? and u.stage in ('UNAPPLIED', 'MONITORING')"
          + " and u.balance > 0 and "
          + RECEIVED_ON
          + " <= ?"
          + " and (cast(? as varchar) is null or u.origin = ?)"
          + " and not exists (select 1 from csh_legacy_batch_line l join csh_legacy_batch b"
          + " on b.id = l.batch_id where l.unapplied_id = u.id and l.status <> 'FAILED'"
          + " and b.status <> 'CANCELLED') order by u.created_at, u.id limit 500";

  private final LegacyBatchRepository batches;
  private final LegacyBatchLineRepository lines;
  private final UnappliedService unapplied;
  private final InvoiceLedgerQueryService ledger;
  private final LegacyBatchPoster poster;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final TransactionTemplate tx;
  private final JdbcTemplate jdbc;

  /**
   * Creates the service.
   *
   * @param batches batches
   * @param lines lines
   * @param unapplied unapplied items
   * @param ledger Operations ledger
   * @param poster line poster
   * @param numbers batch numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   * @param txManager transactions (the lines post one by one after the approval)
   * @param jdbc JDBC (candidates)
   */
  @SuppressWarnings("java:S107") // collaborators
  public LegacyBatchService(
      LegacyBatchRepository batches,
      LegacyBatchLineRepository lines,
      UnappliedService unapplied,
      InvoiceLedgerQueryService ledger,
      LegacyBatchPoster poster,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock,
      PlatformTransactionManager txManager,
      JdbcTemplate jdbc) {
    this.batches = batches;
    this.lines = lines;
    this.unapplied = unapplied;
    this.ledger = ledger;
    this.poster = poster;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
    this.tx = new TransactionTemplate(txManager);
    this.jdbc = jdbc;
  }

  /**
   * Batches of a kind.
   *
   * @param companyId company
   * @param kind kind
   * @return batches, newest first
   */
  @Transactional(readOnly = true)
  public List<LegacyBatch> list(Long companyId, LegacyBatch.Kind kind) {
    return batches.findByCompanyIdAndKindOrderByIdDesc(companyId, kind);
  }

  /**
   * A batch.
   *
   * @param batchNo number
   * @return the batch
   */
  @Transactional(readOnly = true)
  public LegacyBatch get(String batchNo) {
    return batches
        .findByBatchNo(batchNo)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, batchNo));
  }

  /**
   * Lines of a batch.
   *
   * @param batch batch
   * @return lines
   */
  @Transactional(readOnly = true)
  public List<LegacyBatchLine> lines(LegacyBatch batch) {
    return lines.findByBatchIdOrderByLineNoAsc(batch.getId());
  }

  /**
   * The unapplied items that may be taken to income: open in the Unapplied tab, with a balance,
   * received at least a number of days ago (a migrated item from its legacy receipt date) and not
   * on another batch.
   *
   * @param companyId company
   * @param minAgeDays minimum age in days
   * @param origin MIGRATED, or null for every origin
   * @return candidates, oldest first
   */
  @Transactional(readOnly = true)
  public List<Candidate> candidates(Long companyId, int minAgeDays, String origin) {
    LocalDate before = BusinessClock.today(clock).minusDays(minAgeDays);
    return jdbc.query(
        CANDIDATES,
        (rs, i) ->
            new Candidate(
                rs.getLong("id"),
                rs.getString("reference"),
                rs.getString("origin"),
                rs.getString("ledger_context"),
                rs.getString("legacy_ar_no"),
                rs.getString("payor_name"),
                rs.getString("client_code"),
                rs.getString("currency"),
                rs.getBigDecimal("balance"),
                rs.getInt("age_days"),
                rs.getString("stage")),
        BusinessClock.today(clock),
        companyId,
        java.sql.Date.valueOf(before),
        origin,
        origin);
  }

  /**
   * An unapplied item that may be taken to income.
   *
   * @param id item
   * @param reference reference
   * @param origin origin
   * @param ledgerContext NEW or LEGACY
   * @param legacyArNo legacy acknowledgement receipt
   * @param payorName payor
   * @param clientCode client
   * @param currency currency
   * @param balance balance
   * @param ageDays age in days
   * @param stage stage
   */
  public record Candidate(
      Long id,
      String reference,
      String origin,
      String ledgerContext,
      String legacyArNo,
      String payorName,
      String clientCode,
      String currency,
      BigDecimal balance,
      int ageDays,
      String stage) {}

  /**
   * Opens a batch.
   *
   * @param companyId company
   * @param kind kind
   * @param reason reason
   * @param currency currency of its lines
   * @return the batch
   */
  public LegacyBatch create(Long companyId, LegacyBatch.Kind kind, String reason, String currency) {
    requireText(reason, "Give the reason of the batch");
    String prefix = kind == LegacyBatch.Kind.INCOME_RECLASS ? "UIR-" : "P2R-";
    LegacyBatch batch =
        batches.save(
            new LegacyBatch(
                companyId,
                numbers.next(prefix + BusinessClock.today(clock).getYear()),
                kind,
                reason.strip(),
                currency));
    audit.record(ENTITY, batch.getBatchNo(), AuditAction.CREATE, kind + ": " + reason);
    return batch;
  }

  /**
   * Adds an unapplied item to an income reclassification.
   *
   * @param batchNo batch
   * @param unappliedId item
   * @param reason reason
   * @return the line
   */
  public LegacyBatchLine addUnapplied(String batchNo, Long unappliedId, String reason) {
    LegacyBatch batch = draftOf(batchNo, LegacyBatch.Kind.INCOME_RECLASS);
    Unapplied item = unapplied.get(unappliedId);
    if (!item.getCompanyId().equals(batch.getCompanyId())
        || !item.getCurrency().equals(batch.getCurrency())) {
      throw new BusinessRuleException(
          "CASH_BATCH_ITEM", item.getReference() + " is not of the batch's company and currency");
    }
    if (!OPEN_STAGES.contains(item.getStage()) || item.getBalance().signum() <= 0) {
      throw new BusinessRuleException(
          "CASH_BATCH_ITEM", item.getReference() + " has no open balance in the Unapplied tab");
    }
    if (lines.taken(unappliedId)) {
      throw new BusinessRuleException(
          "CASH_BATCH_ITEM", item.getReference() + " is already on a reclassification batch");
    }
    int age =
        (int)
            ChronoUnit.DAYS.between(
                LocalDate.ofInstant(item.getCreatedAt(), BusinessClock.zone()),
                BusinessClock.today(clock));
    LegacyBatchLine line =
        lines.save(
            new LegacyBatchLine(
                batch.getId(),
                batch.getLineCount() + 1,
                new LegacyBatchLine.Subject(
                    item.getId(),
                    null,
                    item.getReference(),
                    item.getLegacy().ledgerContext().name(),
                    age),
                item.getBalance(),
                reason));
    batch.lineAdded(item.getBalance());
    return line;
  }

  /**
   * Adds a legacy invoice to a PR 2307 reversal.
   *
   * @param batchNo batch
   * @param invoiceNo legacy invoice
   * @param amount amount to reverse
   * @param reason reason
   * @return the line
   */
  public LegacyBatchLine addInvoice(
      String batchNo, String invoiceNo, BigDecimal amount, String reason) {
    LegacyBatch batch = draftOf(batchNo, LegacyBatch.Kind.PR2307_REVERSAL);
    OpsInvoice invoice =
        ledger
            .find(invoiceNo)
            .filter(i -> i.getCompanyId().equals(batch.getCompanyId()))
            .orElseThrow(() -> new ResourceNotFoundException("Operations invoice", invoiceNo));
    if (!invoice.getLegacy().isLegacy()) {
      throw new BusinessRuleException(
          "CASH_BATCH_NOT_LEGACY", invoiceNo + " is not a legacy invoice");
    }
    invoice.loadCollections();
    BigDecimal available =
        invoice.component(LedgerComponent.PR2307).getBalance().max(BigDecimal.ZERO);
    for (LedgerComponent c : LedgerComponent.applicationHierarchy()) {
      available = available.add(invoice.component(c).getBalance().max(BigDecimal.ZERO));
    }
    BigDecimal dtip = invoice.component(LedgerComponent.DTIP).getBalance();
    if (amount == null
        || amount.signum() <= 0
        || amount.compareTo(available) > 0
        || amount.compareTo(dtip) > 0) {
      throw new BusinessRuleException(
          "CASH_BATCH_AMOUNT",
          invoiceNo
              + ": the amount must be positive and within the open PR 2307 and due to insurer");
    }
    LegacyBatchLine line =
        lines.save(
            new LegacyBatchLine(
                batch.getId(),
                batch.getLineCount() + 1,
                new LegacyBatchLine.Subject(
                    null, invoiceNo, invoiceNo, invoice.getLegacy().ledgerContext().name(), null),
                amount,
                reason));
    batch.lineAdded(amount);
    return line;
  }

  /**
   * Removes a line of a draft.
   *
   * @param batchNo batch
   * @param lineId line
   */
  public void removeLine(String batchNo, Long lineId) {
    LegacyBatch batch = get(batchNo);
    LegacyBatchLine line =
        lines
            .findById(lineId)
            .filter(l -> l.getBatchId().equals(batch.getId()))
            .orElseThrow(() -> new ResourceNotFoundException("Batch line", lineId));
    batch.lineRemoved(line.getAmount());
    lines.delete(line);
  }

  /**
   * Submits a draft for approval.
   *
   * @param batchNo batch
   * @return the batch
   */
  public LegacyBatch submit(String batchNo) {
    LegacyBatch batch = get(batchNo);
    batch.submit(currentUser.username(), clock.instant());
    audit.record(ENTITY, batchNo, AuditAction.SUBMIT, batch.getLineCount() + " line(s)");
    return batch;
  }

  /**
   * Approves the batch at its current level; the last approval posts the lines.
   *
   * @param batchNo batch
   * @param comment comment
   * @return the batch
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public LegacyBatch approve(String batchNo, String comment) {
    LegacyBatch batch = tx.execute(s -> approveStep(batchNo, comment));
    if (batch != null && batch.getFinalApprovedBy() != null) {
      return execute(batch);
    }
    return batch;
  }

  private LegacyBatch approveStep(String batchNo, String comment) {
    LegacyBatch batch = get(batchNo);
    String user = currentUser.username();
    if (CurrentUser.sameUser(user, batch.getSubmittedBy())
        || CurrentUser.sameUser(user, batch.getCreatedBy())) {
      throw new BusinessRuleException(
          "CASH_BATCH_MAKER_CHECKER", "The requester of a batch does not approve it");
    }
    if (batch.getStatus() == LegacyBatch.Status.FOR_TOP_MANAGEMENT) {
      if (CurrentUser.sameUser(user, batch.getFirstApprovedBy())) {
        throw new BusinessRuleException(
            "CASH_BATCH_MAKER_CHECKER", "The two approvals are given by two people");
      }
      batch.approveFinal(user, clock.instant());
    } else {
      batch.approveFirst(user, clock.instant());
    }
    audit.record(ENTITY, batchNo, AuditAction.AUTHORIZE, blank(comment) ? "Approved" : comment);
    return batch;
  }

  private LegacyBatch execute(LegacyBatch batch) {
    int posted = 0;
    int failed = 0;
    List<LegacyBatchLine> pending = tx.execute(s -> lines(batch));
    for (LegacyBatchLine line : pending == null ? List.<LegacyBatchLine>of() : pending) {
      try {
        poster.post(batch, line.getId());
        posted++;
      } catch (RuntimeException e) {
        poster.fail(line.getId(), e.getMessage());
        failed++;
      }
    }
    int done = posted;
    int refused = failed;
    return tx.execute(s -> executed(batch.getBatchNo(), done, refused));
  }

  private LegacyBatch executed(String batchNo, int posted, int failed) {
    LegacyBatch batch = get(batchNo);
    batch.executed(posted, failed, clock.instant());
    audit.record(
        ENTITY, batchNo, AuditAction.UPDATE, posted + " line(s) posted, " + failed + " refused");
    return batch;
  }

  /**
   * Returns a batch to its requester.
   *
   * @param batchNo batch
   * @param reason reason
   * @return the batch
   */
  public LegacyBatch returnBatch(String batchNo, String reason) {
    requireText(reason, "Give the reason of the return");
    LegacyBatch batch = get(batchNo);
    batch.returnToDraft(reason.strip());
    audit.record(ENTITY, batchNo, AuditAction.REJECT, reason);
    return batch;
  }

  /**
   * Cancels a draft.
   *
   * @param batchNo batch
   * @return the batch
   */
  public LegacyBatch cancel(String batchNo) {
    LegacyBatch batch = get(batchNo);
    batch.cancel();
    audit.record(ENTITY, batchNo, AuditAction.DEACTIVATE, "Cancelled");
    return batch;
  }

  private LegacyBatch draftOf(String batchNo, LegacyBatch.Kind kind) {
    LegacyBatch batch = get(batchNo);
    if (batch.getKind() != kind) {
      throw new BusinessRuleException("CASH_BATCH_KIND", batchNo + " is a " + batch.getKind());
    }
    batch.requireStatus(LegacyBatch.Status.DRAFT);
    return batch;
  }

  private static boolean blank(String text) {
    return text == null || text.isBlank();
  }

  private static void requireText(String text, String message) {
    if (blank(text)) {
      throw new BusinessRuleException("CASH_REASON_REQUIRED", message);
    }
  }
}
