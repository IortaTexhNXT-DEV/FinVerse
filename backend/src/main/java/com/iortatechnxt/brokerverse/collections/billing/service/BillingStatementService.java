package com.iortatechnxt.brokerverse.collections.billing.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingDocument;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingDocumentRepository;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatement;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatement.StatementStatus;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatementLine;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatementLine.LineKind;
import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatementRepository;
import com.iortatechnxt.brokerverse.collections.installment.domain.Installment;
import com.iortatechnxt.brokerverse.collections.installment.domain.InstallmentPlan;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.PlanStatus;
import com.iortatechnxt.brokerverse.collections.installment.service.InstallmentPlanService;
import com.iortatechnxt.brokerverse.collections.installment.service.PlanAllocation;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Statements of account per billing cycle (BRCLXN.058/060, CQ18): one per cycle of an installment
 * plan (a policy year of a multi-year account, or an installment), listing the cycle's installment
 * and every earlier one still unpaid with the payments allocated from the ledger, rendered from the
 * template {@code CLX_SOA}. Generating a statement creates no receivable and no commission billing
 * (BRCLXN.060); a cancelled statement frees the cycle to be billed again.
 */
@Service
@Transactional
public class BillingStatementService {

  /** Audit entity type. */
  public static final String ENTITY = "BillingStatement";

  private static final LocalDate EARLIEST = LocalDate.of(1900, 1, 1);
  private static final LocalDate LATEST = LocalDate.of(9999, 12, 31);

  private final BillingStatementRepository statements;
  private final BillingDocumentRepository documents;
  private final InstallmentPlanService plans;
  private final PlanAllocation allocation;
  private final SoaDocument renderer;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param statements statements
   * @param documents rendered statements
   * @param plans installment plans
   * @param allocation payment allocation
   * @param renderer PDF renderer
   * @param numbers SOA numbers
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public BillingStatementService(
      BillingStatementRepository statements,
      BillingDocumentRepository documents,
      InstallmentPlanService plans,
      PlanAllocation allocation,
      SoaDocument renderer,
      DocumentNumberService numbers,
      AuditTrailService audit,
      Clock clock) {
    this.statements = statements;
    this.documents = documents;
    this.plans = plans;
    this.allocation = allocation;
    this.renderer = renderer;
    this.numbers = numbers;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * A statement with its lines.
   *
   * @param id statement
   * @return statement
   */
  @Transactional(readOnly = true)
  public BillingStatement get(Long id) {
    return statements
        .findWithLinesById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Statements of a company.
   *
   * @param companyId company
   * @param statuses statuses (all when empty)
   * @param from first due date (open when null)
   * @param to last due date (open when null)
   * @param q SOA, account, client or assured
   * @param pageable page
   * @return statements
   */
  @Transactional(readOnly = true)
  public Page<BillingStatement> search(
      Long companyId,
      Collection<StatementStatus> statuses,
      LocalDate from,
      LocalDate to,
      String q,
      Pageable pageable) {
    return statements.search(
        companyId,
        statuses == null || statuses.isEmpty() ? List.of(StatementStatus.values()) : statuses,
        from == null ? EARLIEST : from,
        to == null ? LATEST : to,
        "%" + (q == null ? "" : q.strip().toLowerCase(Locale.ROOT)) + "%",
        pageable);
  }

  /**
   * Statements of a plan, by cycle.
   *
   * @param planId plan
   * @return statements
   */
  @Transactional(readOnly = true)
  public List<BillingStatement> forPlan(Long planId) {
    return statements.findByPlanIdOrderByCycleSeqAscIdAsc(planId);
  }

  /**
   * The statement of one billing cycle of a plan (BRCLXN.058): the payments are allocated first,
   * then the cycle's installment and the earlier unpaid ones are billed.
   *
   * @param planId plan
   * @param cycleSeq billing cycle (installment sequence)
   * @return the statement, rendered
   */
  public BillingStatement generate(Long planId, int cycleSeq) {
    InstallmentPlan plan = plans.get(planId);
    if (plan.getStatus() == PlanStatus.CANCELLED) {
      throw new BusinessRuleException(
          "CLX_PLAN_NOT_ACTIVE", "Plan " + plan.getPlanNo() + " is cancelled");
    }
    Installment current = plan.installment(cycleSeq);
    statements
        .findFirstByPlanIdAndCycleSeqAndStatusNot(planId, cycleSeq, StatementStatus.CANCELLED)
        .ifPresent(
            s -> {
              throw new BusinessRuleException(
                  "CLX_SOA_EXISTS",
                  "Cycle "
                      + cycleSeq
                      + " of "
                      + plan.getPlanNo()
                      + " is billed by "
                      + s.getSoaNo());
            });
    LocalDate today = LocalDate.now(clock);
    if (plan.isActive()) {
      allocation.allocate(plan, today);
    }
    BillingStatement soa =
        statements.save(
            BillingStatement.create(
                new BillingStatement.Header(
                    plan.getCompanyId(),
                    numbers.next("SOA-" + today.getYear()),
                    plan.getId(),
                    cycleSeq,
                    plan.getArn(),
                    plan.getClientCode(),
                    plan.getAssuredName(),
                    plan.getCurrency(),
                    plan.getFrequency(),
                    current.getCycleFrom(),
                    current.getCycleTo(),
                    current.getDueDate()),
                lines(plan, current)));
    byte[] pdf = renderer.render(soa);
    documents.save(
        new BillingDocument(soa.getId(), soa.getSoaNo() + ".pdf", "application/pdf", pdf));
    audit.record(
        ENTITY,
        soa.getSoaNo(),
        AuditAction.CREATE,
        "SOA of "
            + plan.getPlanNo()
            + " cycle "
            + cycleSeq
            + " ("
            + soa.getCycleFrom()
            + " to "
            + soa.getCycleTo()
            + "), balance "
            + soa.getBalance());
    return soa;
  }

  private static List<BillingStatementLine.Facts> lines(InstallmentPlan plan, Installment current) {
    List<BillingStatementLine.Facts> lines = new ArrayList<>();
    for (Installment i : plan.getInstallments()) {
      boolean arrears = i.getSeq() < current.getSeq() && i.balance().signum() > 0;
      if (arrears || i.getSeq() == current.getSeq()) {
        lines.add(
            new BillingStatementLine.Facts(
                arrears ? LineKind.ARREARS : LineKind.CURRENT,
                i.getInvoiceNo(),
                i.getPolicyYear(),
                i.getSeq(),
                i.getCycleFrom(),
                i.getCycleTo(),
                i.getDueDate(),
                i.getAmount(),
                i.getPaidAmount()));
      }
    }
    return lines;
  }

  /**
   * The statements of every billing cycle of a company's live plans falling due in a period and not
   * billed yet (the billing run of BRCLXN.058).
   *
   * @param companyId company
   * @param from first due date
   * @param to last due date
   * @return statements generated
   */
  public List<BillingStatement> generateDue(Long companyId, LocalDate from, LocalDate to) {
    if (to.isBefore(from)) {
      throw new BusinessRuleException("CLX_SOA_PERIOD", "The period ends before it starts");
    }
    List<BillingStatement> generated = new ArrayList<>();
    for (Long planId : plans.activePlanIds()) {
      InstallmentPlan plan = plans.get(planId);
      if (!plan.getCompanyId().equals(companyId)) {
        continue;
      }
      for (Installment i : plan.getInstallments()) {
        boolean inPeriod = !i.getDueDate().isBefore(from) && !i.getDueDate().isAfter(to);
        if (inPeriod
            && statements
                .findFirstByPlanIdAndCycleSeqAndStatusNot(
                    plan.getId(), i.getSeq(), StatementStatus.CANCELLED)
                .isEmpty()) {
          generated.add(generate(plan.getId(), i.getSeq()));
        }
      }
    }
    return generated;
  }

  /**
   * Cancels a statement so the cycle can be billed again.
   *
   * @param id statement
   * @param reason why
   * @return the statement
   */
  public BillingStatement cancel(Long id, String reason) {
    BillingStatement soa = get(id);
    soa.cancel(reason);
    audit.record(ENTITY, soa.getSoaNo(), AuditAction.UPDATE, "Cancelled: " + reason);
    return soa;
  }

  /**
   * The rendered statement.
   *
   * @param id statement
   * @return document
   */
  @Transactional(readOnly = true)
  public BillingDocument document(Long id) {
    return documents
        .findByStatementId(id)
        .orElseThrow(() -> new ResourceNotFoundException("Statement document", id));
  }
}
