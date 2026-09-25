package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied.UnappliedSpec;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
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
    Unapplied item =
        items.save(
            new Unapplied(
                companyId, branchId, numbers.next("UNP-" + LocalDate.now(clock).getYear()), spec));
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

  private static String title(Unapplied item) {
    String who = item.getPayorName() != null ? item.getPayorName() : item.getClientCode();
    return (who == null ? "Unapplied payment" : who)
        + " "
        + item.getCurrency()
        + " "
        + item.getAmount().toPlainString();
  }
}
