package com.iortatechnxt.brokerverse.collections.promise.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.collections.installment.domain.Installment;
import com.iortatechnxt.brokerverse.collections.installment.domain.InstallmentRepository;
import com.iortatechnxt.brokerverse.collections.installment.service.LedgerBalances;
import com.iortatechnxt.brokerverse.collections.installment.service.LedgerBalances.Payments;
import com.iortatechnxt.brokerverse.collections.promise.domain.PaymentPromise;
import com.iortatechnxt.brokerverse.collections.promise.domain.PaymentPromise.Account;
import com.iortatechnxt.brokerverse.collections.promise.domain.PaymentPromise.Outcome;
import com.iortatechnxt.brokerverse.collections.promise.domain.PaymentPromise.Terms;
import com.iortatechnxt.brokerverse.collections.promise.domain.PaymentPromiseRepository;
import com.iortatechnxt.brokerverse.collections.promise.domain.PromiseStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Promises to pay (BRCLXN.055, CQ16): recorded by the collector on a collection account or one of
 * its installments, replaced by a newer promise, withdrawn, and evaluated against the payments the
 * ledger applied between the day of the promise and its date plus {@code CLX_PROMISE_GRACE_DAYS}. A
 * broken promise notifies the collector and the account officer ({@code CLX_PROMISE_BROKEN}) and is
 * published as {@link PromiseBroken} for the escalation rules.
 */
@Service
@Transactional
public class PromiseService {

  /** Audit entity type. */
  public static final String ENTITY = "PaymentPromise";

  /** Grace days parameter. */
  public static final String GRACE_DAYS = "CLX_PROMISE_GRACE_DAYS";

  private static final String BROKEN_EVENT = "CLX_PROMISE_BROKEN";
  private static final String PROMISE_OF = "Promise of ";
  private static final LocalDate EARLIEST = LocalDate.of(1900, 1, 1);
  private static final LocalDate LATEST = LocalDate.of(9999, 12, 31);

  private final PaymentPromiseRepository promises;
  private final InstallmentRepository installments;
  private final LedgerBalances ledger;
  private final SystemParameterService parameters;
  private final NotificationService notifications;
  private final ApplicationEventPublisher events;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param promises promises
   * @param installments installments (promise per installment)
   * @param ledger ledger reads
   * @param parameters grace days
   * @param notifications broken-promise notifications
   * @param events event publisher
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public PromiseService(
      PaymentPromiseRepository promises,
      InstallmentRepository installments,
      LedgerBalances ledger,
      SystemParameterService parameters,
      NotificationService notifications,
      ApplicationEventPublisher events,
      AuditTrailService audit,
      Clock clock) {
    this.promises = promises;
    this.installments = installments;
    this.ledger = ledger;
    this.parameters = parameters;
    this.notifications = notifications;
    this.events = events;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * A promise.
   *
   * @param id promise
   * @return promise
   */
  @Transactional(readOnly = true)
  public PaymentPromise get(Long id) {
    return promises.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Promises of a company.
   *
   * @param companyId company
   * @param statuses statuses (all when empty)
   * @param from first promised date (open when null)
   * @param to last promised date (open when null)
   * @param q invoice, account, client or assured
   * @param pageable page
   * @return promises
   */
  @Transactional(readOnly = true)
  public Page<PaymentPromise> search(
      Long companyId,
      Collection<PromiseStatus> statuses,
      LocalDate from,
      LocalDate to,
      String q,
      Pageable pageable) {
    return promises.search(
        companyId,
        statuses == null || statuses.isEmpty() ? List.of(PromiseStatus.values()) : statuses,
        from == null ? EARLIEST : from,
        to == null ? LATEST : to,
        "%" + (q == null ? "" : q.strip().toLowerCase(Locale.ROOT)) + "%",
        pageable);
  }

  /**
   * Promises of a collection account, newest first.
   *
   * @param invoiceNo invoice
   * @return promises
   */
  @Transactional(readOnly = true)
  public List<PaymentPromise> forInvoice(String invoiceNo) {
    return promises.findByInvoiceNoOrderByIdDesc(invoiceNo);
  }

  /**
   * Records a promise (BRCLXN.055). An open promise of the same account whose date has passed is
   * evaluated first; one still running is replaced (cancelled as superseded).
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param input dates, amount, installment and remarks
   * @param bulkRef bulk reference, may be null
   * @return the promise
   */
  public PaymentPromise record(
      Long companyId, String invoiceNo, PromiseInput input, String bulkRef) {
    OpsInvoice invoice = ledger.requireReceivable(companyId, invoiceNo);
    BigDecimal outstanding = invoice.premiumBalance();
    if (ledger.collected(outstanding)) {
      throw new BusinessRuleException(
          "CLX_PROMISE_NOTHING_DUE",
          "Invoice " + invoice.getInvoiceNo() + " has nothing to collect");
    }
    LocalDate today = LocalDate.now(clock);
    Terms terms = terms(input, outstanding, today);
    requireInstallmentOf(input.installmentId(), invoice.getInvoiceNo());
    closeRunning(invoice.getInvoiceNo(), today);
    PaymentPromise promise =
        promises.save(
            new PaymentPromise(
                new Account(
                    companyId,
                    invoice.getInvoiceNo(),
                    invoice.getArn(),
                    invoice.getClientCode(),
                    invoice.getAssuredName(),
                    invoice.getCurrency()),
                terms,
                input.remarks(),
                bulkRef));
    audit.record(
        ENTITY,
        promise.getInvoiceNo(),
        AuditAction.CREATE,
        "Promise to pay "
            + promise.getCurrency()
            + " "
            + promise.getPromisedAmount()
            + " by "
            + promise.getPromisedDate()
            + (bulkRef == null ? "" : " (bulk " + bulkRef + ")"));
    return promise;
  }

  private static Terms terms(PromiseInput input, BigDecimal outstanding, LocalDate today) {
    LocalDate promisedOn = input.promisedOn() == null ? today : input.promisedOn();
    if (input.promisedDate() == null || promisedOn.isAfter(today)) {
      throw new BusinessRuleException(
          "CLX_PROMISE_DATES",
          "Enter the promised date; the day of the promise cannot be in the future");
    }
    BigDecimal amount = input.amount() == null ? outstanding : input.amount();
    if (amount.compareTo(outstanding) > 0) {
      throw new BusinessRuleException(
          "CLX_PROMISE_OVER_BALANCE",
          "The promised amount " + amount + " is above the outstanding " + outstanding);
    }
    return new Terms(promisedOn, input.promisedDate(), amount, input.installmentId());
  }

  private void requireInstallmentOf(Long installmentId, String invoiceNo) {
    if (installmentId == null) {
      return;
    }
    Installment installment =
        installments
            .findById(installmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Installment", installmentId));
    if (!invoiceNo.equals(installment.getInvoiceNo())) {
      throw new BusinessRuleException(
          "CLX_PROMISE_INSTALLMENT", "The installment does not bill invoice " + invoiceNo);
    }
  }

  private void closeRunning(String invoiceNo, LocalDate today) {
    int grace = graceDays();
    for (PaymentPromise open : promises.findByInvoiceNoAndStatus(invoiceNo, PromiseStatus.OPEN)) {
      if (open.deadline(grace).isBefore(today)) {
        evaluate(open, today);
      } else {
        open.cancel("Superseded by a new promise", clock.instant());
        audit.record(ENTITY, invoiceNo, AuditAction.UPDATE, "Open promise superseded");
      }
    }
  }

  /**
   * Withdraws an open promise.
   *
   * @param id promise
   * @param note why
   * @return the promise
   */
  public PaymentPromise cancel(Long id, String note) {
    PaymentPromise promise = get(id);
    promise.cancel(note, clock.instant());
    audit.record(ENTITY, promise.getInvoiceNo(), AuditAction.UPDATE, "Promise withdrawn: " + note);
    return promise;
  }

  /**
   * Evaluates an open promise whose date plus the grace days is before the business date.
   *
   * @param id promise
   * @param asOf business date
   * @return the promise
   */
  public PaymentPromise evaluate(Long id, LocalDate asOf) {
    PaymentPromise promise = get(id);
    if (promise.getStatus() == PromiseStatus.OPEN && promise.deadline(graceDays()).isBefore(asOf)) {
      evaluate(promise, asOf);
    }
    return promise;
  }

  private void evaluate(PaymentPromise promise, LocalDate asOf) {
    LocalDate deadline = promise.deadline(graceDays());
    Payments paid =
        ledger.paymentsBetween(
            promise.getInvoiceNo(),
            promise.getPromisedOn(),
            deadline.isBefore(asOf) ? deadline : asOf);
    boolean nothingLeft =
        ledger.outstanding(promise.getInvoiceNo()).map(ledger::collected).orElse(true);
    Outcome outcome =
        PromiseEvaluator.evaluate(
            promise.getPromisedAmount(), paid.total(), paid.lastDate(), nothingLeft);
    promise.evaluate(outcome, clock.instant());
    audit.record(
        ENTITY,
        promise.getInvoiceNo(),
        AuditAction.UPDATE,
        PROMISE_OF
            + promise.getPromisedDate()
            + " "
            + outcome.status()
            + ", paid "
            + outcome.paid());
    if (outcome.status() == PromiseStatus.BROKEN) {
      notifyBroken(promise);
      events.publishEvent(
          new PromiseBroken(promise.getCompanyId(), promise.getId(), promise.getInvoiceNo()));
    }
  }

  private void notifyBroken(PaymentPromise promise) {
    Notice notice =
        new Notice(
            PROMISE_OF + promise.getAssuredName() + " broken",
            promise.getInvoiceNo()
                + ": "
                + promise.getCurrency()
                + " "
                + promise.getPromisedAmount()
                + " promised by "
                + promise.getPromisedDate()
                + " was not paid",
            "/collections/promises?q=" + promise.getInvoiceNo(),
            ENTITY,
            String.valueOf(promise.getId()));
    Set<String> users = new LinkedHashSet<>();
    users.add(promise.getCreatedBy());
    ledger
        .find(promise.getInvoiceNo())
        .map(i -> i.getClassification().aoUsername())
        .filter(Objects::nonNull)
        .ifPresent(users::add);
    users.forEach(u -> notifications.notifyUser(u, notice, BROKEN_EVENT));
  }

  /**
   * Ids of the open promises due for evaluation on a business date: their date plus the grace days
   * is before it.
   *
   * @param asOf business date
   * @return ids
   */
  @Transactional(readOnly = true)
  public List<Long> dueForEvaluation(LocalDate asOf) {
    return promises.idsDueBy(PromiseStatus.OPEN, asOf.minusDays(graceDays() + 1L));
  }

  private int graceDays() {
    return parameters.intValue(GRACE_DAYS, 0);
  }

  /**
   * A promise entered on screen, in a bulk update or from a bulk action.
   *
   * @param promisedOn day the client made the promise; today when null
   * @param promisedDate day the payment is promised for
   * @param amount amount; the whole outstanding when null
   * @param installmentId installment, may be null
   * @param remarks remarks
   */
  public record PromiseInput(
      LocalDate promisedOn,
      LocalDate promisedDate,
      BigDecimal amount,
      Long installmentId,
      String remarks) {}
}
