package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.UnappliedOrigin;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied.UnappliedSpec;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedLegacy;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerContext;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates unapplied items (CSHID.024/025): the excess or unmatched part of a payment, and the items
 * other Operations modules hand over through {@code UnappliedSink}. Each item opens a case of the
 * workflow {@code OPS_DISPOSITION} in the Unapplied tab. Creation is idempotent on (source module,
 * source reference).
 */
@Service
@Transactional
public class UnappliedService {

  /** Entity type of the work case. */
  public static final String ENTITY = "Unapplied";

  /** Workflow of the dispositions. */
  public static final String WORKFLOW = "OPS_DISPOSITION";

  /** Source module of the items carried from legacy. */
  public static final String MIGRATION = "MIGRATION";

  private final UnappliedRepository items;
  private final DocumentNumberService numbers;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param items unapplied items
   * @param numbers document numbers
   * @param workflow workflow
   * @param audit audit trail
   * @param clock clock
   */
  public UnappliedService(
      UnappliedRepository items,
      DocumentNumberService numbers,
      WorkflowService workflow,
      AuditTrailService audit,
      Clock clock) {
    this.items = items;
    this.numbers = numbers;
    this.workflow = workflow;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Creates an item (or returns the one of the same source).
   *
   * @param companyId company
   * @param branchId branch
   * @param spec origin, links, party, money and source
   * @return the item
   */
  public Unapplied create(Long companyId, Long branchId, UnappliedSpec spec) {
    return create(companyId, branchId, spec, UnappliedLegacy.NONE);
  }

  private Unapplied create(
      Long companyId, Long branchId, UnappliedSpec spec, UnappliedLegacy legacy) {
    Optional<Unapplied> earlier =
        items.findByCompanyIdAndSourceModuleAndSourceRef(
            companyId, spec.sourceModule(), spec.sourceRef());
    if (earlier.isPresent()) {
      return earlier.get();
    }
    if (spec.amount() == null || spec.amount().signum() <= 0) {
      throw new BusinessRuleException(
          "UNAPPLIED_AMOUNT", "An unapplied item needs an amount above zero");
    }
    Unapplied created =
        new Unapplied(
            companyId, branchId, numbers.next("UNP-" + BusinessClock.today(clock).getYear()), spec);
    created.markMigrated(legacy);
    Unapplied item = items.save(created);
    workflow.start(
        new StartCase(
            companyId,
            WORKFLOW,
            new CaseRecord(
                ENTITY,
                String.valueOf(item.getId()),
                item.getReference(),
                title(item),
                "/cashiering/unapplied/" + item.getId(),
                item.getSalesUnit()),
            null));
    audit.record(
        ENTITY,
        item.getReference(),
        AuditAction.CREATE,
        item.getOrigin() + " " + item.getCurrency() + " " + item.getAmount());
    return item;
  }

  /**
   * One item.
   *
   * @param id id
   * @return item
   */
  @Transactional(readOnly = true)
  public Unapplied get(Long id) {
    return items.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Uses the whole remaining balance of an item in the Unapplied tab and closes it (automatch,
   * overages, receipt cancelled).
   *
   * @param item item in the Unapplied tab
   * @param action closing transition
   * @param comment comment
   * @return balance used
   */
  public BigDecimal close(Unapplied item, String action, String comment) {
    BigDecimal used = item.getBalance();
    if (used.signum() > 0) {
      item.consume(used);
    }
    workflow.systemTransition(
        ENTITY, String.valueOf(item.getId()), action, TransitionNote.comment(comment));
    audit.record(ENTITY, item.getReference(), AuditAction.CLOSE, action + ": " + comment);
    return used;
  }

  /**
   * Creates an unapplied payment carried from legacy at cut-over (Data Migration, F02): origin
   * MIGRATED, ledger context LEGACY, source module MIGRATION with the legacy reference; no BIBS
   * receipt is issued (the legacy acknowledgement receipt is kept). Idempotent on the legacy
   * reference.
   *
   * @param companyId company
   * @param branchId branch
   * @param spec party, money, disposition hint and remarks (origin and source are set here)
   * @param facts legacy facts
   * @return the item
   */
  public Unapplied createMigrated(
      Long companyId, Long branchId, UnappliedSpec spec, UnappliedLegacy facts) {
    return create(
        companyId,
        branchId,
        new UnappliedSpec(
            UnappliedOrigin.MIGRATED,
            null,
            null,
            spec.invoiceNo(),
            spec.clientCode(),
            spec.payorName(),
            spec.salesUnit(),
            spec.currency(),
            spec.amount(),
            spec.dispositionHint(),
            MIGRATION,
            "MIG:UPP:" + facts.sourceSystem() + ":" + facts.legacyRef(),
            spec.remarks()),
        new UnappliedLegacy(
            facts.sourceSystem(),
            facts.legacyRef(),
            facts.migrationBatch(),
            LedgerContext.LEGACY,
            facts.legacyArNo(),
            facts.legacyArDate(),
            facts.matchRefs()));
  }

  /**
   * Sets the balance of a migrated item at cut-over (the amount received in legacy may be larger
   * than what is still unapplied).
   *
   * @param item migrated item
   * @param balance open balance at cut-over
   */
  public void openAt(Unapplied item, BigDecimal balance) {
    BigDecimal used = item.getAmount().subtract(balance);
    if (used.signum() > 0) {
      item.consume(used);
    }
  }

  /**
   * Adjusts the open balance of a migrated item after go-live (year-end true-up, section 17.7).
   *
   * @param item migrated item
   * @param change signed change of the balance
   */
  public void adjustMigratedOpening(Unapplied item, BigDecimal change) {
    requireMigrated(item);
    if (change.signum() < 0) {
      item.consume(change.negate());
    } else if (change.signum() > 0) {
      item.restore(change);
    }
    audit.record(ENTITY, item.getReference(), AuditAction.UPDATE, "Opening adjusted by " + change);
  }

  /**
   * Closes a migrated item of a rolled-back migration batch. Refused when it was worked in BIBS.
   *
   * @param item migrated item
   * @param batchNo rolled-back batch
   */
  public void rollbackMigrated(Unapplied item, String batchNo) {
    requireMigrated(item);
    if (!Unapplied.STAGE_INITIAL.equals(item.getStage())) {
      throw new BusinessRuleException(
          "MIG_UPP_WORKED",
          "Unapplied payment " + item.getReference() + " is being worked and cannot be undone");
    }
    close(item, "migration_rollback", "Migration batch " + batchNo + " rolled back");
  }

  private static void requireMigrated(Unapplied item) {
    if (item.getOrigin() != UnappliedOrigin.MIGRATED) {
      throw new BusinessRuleException(
          "MIG_UPP_NOT_MIGRATED", item.getReference() + " is not a migrated unapplied payment");
    }
  }

  private static String title(Unapplied item) {
    String who = item.getPayorName() != null ? item.getPayorName() : item.getClientCode();
    return (who == null ? "Unapplied payment" : who)
        + " "
        + item.getCurrency()
        + " "
        + item.getAmount().toPlainString();
  }
}
