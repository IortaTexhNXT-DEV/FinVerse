package com.iortatechnxt.brokerverse.collections.installment.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceStatus;
import com.iortatechnxt.brokerverse.booking.service.BookingQueryService;
import com.iortatechnxt.brokerverse.collections.installment.domain.BillingFrequency;
import com.iortatechnxt.brokerverse.collections.installment.domain.Installment;
import com.iortatechnxt.brokerverse.collections.installment.domain.InstallmentPlan;
import com.iortatechnxt.brokerverse.collections.installment.domain.InstallmentPlan.Header;
import com.iortatechnxt.brokerverse.collections.installment.domain.InstallmentPlanRepository;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.PlanSource;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.PlanStatus;
import com.iortatechnxt.brokerverse.collections.installment.service.InstallmentSchedule.Cycle;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import java.math.BigDecimal;
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
 * Installment plans of collection accounts (BRCLXN.053/054/058, CQ15): built from the policy-year
 * invoices of a multi-year account (booked in the ledger or still scheduled in booking), generated
 * by splitting the outstanding premium of one invoice by a billing frequency, or entered by the
 * collector. The payments are allocated from the ledger by {@link PlanAllocation}. Plans are
 * monitoring only (BRCLXN.060).
 */
@Service
@Transactional
public class InstallmentPlanService {

  /** Audit entity type. */
  public static final String ENTITY = "InstallmentPlan";

  private final InstallmentPlanRepository plans;
  private final LedgerBalances ledger;
  private final BookingQueryService booking;
  private final PlanAllocation allocation;
  private final DocumentNumberService numbers;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param plans plans
   * @param ledger ledger reads
   * @param booking booked and scheduled policy years
   * @param allocation payment allocation
   * @param numbers plan numbers
   * @param lovs billing frequencies
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public InstallmentPlanService(
      InstallmentPlanRepository plans,
      LedgerBalances ledger,
      BookingQueryService booking,
      PlanAllocation allocation,
      DocumentNumberService numbers,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.plans = plans;
    this.ledger = ledger;
    this.booking = booking;
    this.allocation = allocation;
    this.numbers = numbers;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * A plan with its installments.
   *
   * @param id plan
   * @return plan
   */
  @Transactional(readOnly = true)
  public InstallmentPlan get(Long id) {
    return plans
        .findWithInstallmentsById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Plans of a company.
   *
   * @param companyId company
   * @param statuses statuses (all when empty)
   * @param q plan, account, invoice, client or assured
   * @param pageable page
   * @return plans
   */
  @Transactional(readOnly = true)
  public Page<InstallmentPlan> search(
      Long companyId, Collection<PlanStatus> statuses, String q, Pageable pageable) {
    Collection<PlanStatus> wanted =
        statuses == null || statuses.isEmpty() ? List.of(PlanStatus.values()) : statuses;
    String like = "%" + (q == null ? "" : q.strip().toLowerCase(Locale.ROOT)) + "%";
    return plans.search(companyId, wanted, like, pageable);
  }

  /**
   * Plans of an account, newest first, with their installments.
   *
   * @param arn account
   * @return plans
   */
  @Transactional(readOnly = true)
  public List<InstallmentPlan> forAccount(String arn) {
    List<InstallmentPlan> list = plans.findByArnOrderByIdDesc(arn);
    list.forEach(InstallmentPlan::loadInstallments);
    return list;
  }

  /**
   * A plan over the policy years of a multi-year account (BRCLXN.058): each policy-year invoice,
   * booked or still scheduled, is split into the billing cycles of the frequency within its
   * coverage year. The first policy year must be booked.
   *
   * @param companyId company
   * @param arn account
   * @param frequency billing frequency (LOV CLX_BILLING_FREQUENCY)
   * @param remarks remarks
   * @return the plan, allocated
   */
  public InstallmentPlan fromPolicyYears(
      Long companyId, String arn, String frequency, String remarks) {
    BillingFrequency cycle = frequency(frequency);
    List<BookedInvoice> years =
        booking.schedule(arn.strip()).stream()
            .filter(i -> i.getKind() == InvoiceKind.BOOKING)
            .filter(i -> i.getStatus() != InvoiceStatus.CANCELLED)
            .toList();
    BookedInvoice first =
        years.stream()
            .filter(BookedInvoice::isBooked)
            .findFirst()
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "CLX_PLAN_NOT_BOOKED", "Account " + arn + " has no booked policy year"));
    OpsInvoice lead = ledger.requireReceivable(companyId, first.getInvoiceNo());
    requireNoLivePlan(companyId, lead.getArn(), years);
    List<Installment.Terms> terms = new ArrayList<>();
    for (BookedInvoice year : years) {
      BigDecimal amount =
          year.isBooked()
              ? ledger.requireReceivable(companyId, year.getInvoiceNo()).getGrossPremium()
              : year.getPremium().total();
      addYear(terms, year, amount, cycle);
    }
    return save(
        header(lead, null, frequency, PlanSource.POLICY_YEARS, remarks),
        terms,
        "Policy-year plan of " + lead.getArn());
  }

  private static void addYear(
      List<Installment.Terms> terms, BookedInvoice year, BigDecimal amount, BillingFrequency f) {
    List<Cycle> cycles =
        InstallmentSchedule.cycles(year.getInceptionDate(), year.getExpiryDate(), f.months());
    List<BigDecimal> amounts = InstallmentSchedule.split(amount, cycles.size());
    for (int k = 0; k < cycles.size(); k++) {
      Cycle c = cycles.get(k);
      terms.add(
          new Installment.Terms(
              terms.size() + 1,
              year.getPolicyYear(),
              year.isBooked() ? year.getInvoiceNo() : null,
              c.from(),
              c.from(),
              c.to(),
              amounts.get(k)));
    }
  }

  /**
   * A plan splitting the outstanding premium of one invoice in equal installments (BRCLXN.053).
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param frequency billing frequency
   * @param firstDue first due date
   * @param count number of installments
   * @param remarks remarks
   * @return the plan, allocated
   */
  public InstallmentPlan generate(
      Long companyId,
      String invoiceNo,
      String frequency,
      LocalDate firstDue,
      int count,
      String remarks) {
    BillingFrequency cycle = frequency(frequency);
    OpsInvoice invoice = openInvoice(companyId, invoiceNo);
    List<Cycle> cycles = InstallmentSchedule.consecutive(firstDue, count, cycle.months());
    List<BigDecimal> amounts = InstallmentSchedule.split(invoice.premiumBalance(), count);
    List<Installment.Terms> terms = new ArrayList<>();
    for (int k = 0; k < count; k++) {
      Cycle c = cycles.get(k);
      terms.add(
          new Installment.Terms(
              k + 1,
              invoice.getPolicyYear(),
              invoice.getInvoiceNo(),
              c.from(),
              c.from(),
              c.to(),
              amounts.get(k)));
    }
    return save(
        header(invoice, invoice.getInvoiceNo(), frequency, PlanSource.GENERATED, remarks),
        terms,
        "Generated plan of " + invoice.getInvoiceNo() + ", " + count + " installments");
  }

  /**
   * A plan with installments entered by the collector; they must add up to the outstanding premium
   * of the invoice and fall due in increasing order.
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param frequency billing frequency shown on the statements
   * @param entries due dates and amounts
   * @param remarks remarks
   * @return the plan, allocated
   */
  public InstallmentPlan manual(
      Long companyId,
      String invoiceNo,
      String frequency,
      List<ManualEntry> entries,
      String remarks) {
    frequency(frequency);
    OpsInvoice invoice = openInvoice(companyId, invoiceNo);
    ManualEntry.requireValid(entries, invoice.premiumBalance());
    List<Installment.Terms> terms = new ArrayList<>();
    for (int k = 0; k < entries.size(); k++) {
      ManualEntry e = entries.get(k);
      LocalDate end =
          k + 1 < entries.size()
              ? entries.get(k + 1).dueDate().minusDays(1)
              : lastDay(invoice, e.dueDate());
      terms.add(
          new Installment.Terms(
              k + 1,
              invoice.getPolicyYear(),
              invoice.getInvoiceNo(),
              e.dueDate(),
              e.dueDate(),
              end,
              e.amount()));
    }
    return save(
        header(invoice, invoice.getInvoiceNo(), frequency, PlanSource.MANUAL, remarks),
        terms,
        "Manual plan of " + invoice.getInvoiceNo() + ", " + entries.size() + " installments");
  }

  private static LocalDate lastDay(OpsInvoice invoice, LocalDate due) {
    LocalDate end = invoice.getClassification().expiryDate().minusDays(1);
    return end.isAfter(due) ? end : due;
  }

  /**
   * Cancels a live plan.
   *
   * @param id plan
   * @param reason why
   * @return the plan
   */
  public InstallmentPlan cancel(Long id, String reason) {
    InstallmentPlan plan = get(id);
    plan.cancel(reason);
    audit.record(ENTITY, plan.getPlanNo(), AuditAction.UPDATE, "Cancelled: " + reason);
    return plan;
  }

  /**
   * Allocates the ledger payments to the installments of a plan as of today.
   *
   * @param id plan
   * @return the plan
   */
  public InstallmentPlan refresh(Long id) {
    InstallmentPlan plan = get(id);
    allocation.allocate(plan, LocalDate.now(clock));
    return plan;
  }

  /**
   * Allocates the ledger payments to the installments of a plan as of a business date (daily
   * refresh).
   *
   * @param id plan
   * @param asOf business date
   */
  public void refresh(Long id, LocalDate asOf) {
    InstallmentPlan plan = get(id);
    if (plan.isActive()) {
      allocation.allocate(plan, asOf);
    }
  }

  /**
   * Ids of the live plans.
   *
   * @return ids
   */
  @Transactional(readOnly = true)
  public List<Long> activePlanIds() {
    return plans.idsByStatus(PlanStatus.ACTIVE);
  }

  private OpsInvoice openInvoice(Long companyId, String invoiceNo) {
    OpsInvoice invoice = ledger.requireReceivable(companyId, invoiceNo);
    if (ledger.collected(invoice.premiumBalance())) {
      throw new BusinessRuleException(
          "CLX_PLAN_NOTHING_DUE",
          "Invoice " + invoice.getInvoiceNo() + " has no outstanding premium to schedule");
    }
    plans
        .findFirstByCompanyIdAndInvoiceNoAndStatus(
            companyId, invoice.getInvoiceNo(), PlanStatus.ACTIVE)
        .or(
            () ->
                plans.findFirstByCompanyIdAndArnAndSourceAndStatus(
                    companyId, invoice.getArn(), PlanSource.POLICY_YEARS, PlanStatus.ACTIVE))
        .ifPresent(InstallmentPlanService::refuseDuplicate);
    return invoice;
  }

  private void requireNoLivePlan(Long companyId, String arn, List<BookedInvoice> years) {
    plans
        .findFirstByCompanyIdAndArnAndSourceAndStatus(
            companyId, arn, PlanSource.POLICY_YEARS, PlanStatus.ACTIVE)
        .ifPresent(InstallmentPlanService::refuseDuplicate);
    years.stream()
        .filter(BookedInvoice::isBooked)
        .forEach(
            y ->
                plans
                    .findFirstByCompanyIdAndInvoiceNoAndStatus(
                        companyId, y.getInvoiceNo(), PlanStatus.ACTIVE)
                    .ifPresent(InstallmentPlanService::refuseDuplicate));
  }

  private static void refuseDuplicate(InstallmentPlan existing) {
    throw new BusinessRuleException(
        "CLX_PLAN_EXISTS",
        "Plan " + existing.getPlanNo() + " already schedules this account; cancel it first");
  }

  private BillingFrequency frequency(String code) {
    lovs.requireValid(BillingFrequency.LOV, code, LocalDate.now(clock));
    return BillingFrequency.of(code);
  }

  private Header header(
      OpsInvoice invoice, String invoiceNo, String frequency, PlanSource source, String remarks) {
    return new Header(
        invoice.getCompanyId(),
        numbers.next("IPL-" + LocalDate.now(clock).getYear()),
        invoice.getArn(),
        invoiceNo,
        invoice.getClientCode(),
        invoice.getAssuredName(),
        invoice.getCurrency(),
        frequency,
        source,
        remarks);
  }

  private InstallmentPlan save(Header header, List<Installment.Terms> terms, String summary) {
    InstallmentPlan plan = plans.save(InstallmentPlan.create(header, terms));
    allocation.allocate(plan, LocalDate.now(clock));
    audit.record(
        ENTITY,
        plan.getPlanNo(),
        AuditAction.CREATE,
        summary + " (" + plan.getFrequency() + ", total " + plan.getTotal() + ")");
    return plan;
  }

  /**
   * One installment entered by the collector.
   *
   * @param dueDate due date
   * @param amount amount
   */
  public record ManualEntry(LocalDate dueDate, BigDecimal amount) {

    /**
     * Checks the entries: at least one, positive amounts, increasing due dates, adding up to the
     * outstanding premium.
     *
     * @param entries entries
     * @param outstanding outstanding premium of the invoice
     */
    static void requireValid(List<ManualEntry> entries, BigDecimal outstanding) {
      if (entries.isEmpty()) {
        throw new BusinessRuleException("CLX_PLAN_EMPTY", "Enter at least one installment");
      }
      BigDecimal sum = BigDecimal.ZERO;
      LocalDate previous = null;
      for (ManualEntry e : entries) {
        e.requireAfter(previous);
        sum = sum.add(e.amount());
        previous = e.dueDate();
      }
      if (sum.compareTo(outstanding) != 0) {
        throw new BusinessRuleException(
            "CLX_PLAN_TOTAL_MISMATCH",
            "The installments add up to " + sum + " but the outstanding premium is " + outstanding);
      }
    }

    private void requireAfter(LocalDate previous) {
      if (amount.signum() <= 0 || previous != null && !dueDate.isAfter(previous)) {
        throw new BusinessRuleException(
            "CLX_PLAN_ENTRY_INVALID",
            "Installment amounts must be positive and due dates increasing");
      }
    }
  }
}
