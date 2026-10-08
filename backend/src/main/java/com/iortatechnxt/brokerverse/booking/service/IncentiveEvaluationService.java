package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoiceRepository;
import com.iortatechnxt.brokerverse.booking.domain.IncentiveEvaluation;
import com.iortatechnxt.brokerverse.booking.domain.IncentiveEvaluation.Outcome;
import com.iortatechnxt.brokerverse.booking.domain.IncentiveEvaluationRepository;
import com.iortatechnxt.brokerverse.booking.domain.IncentiveStatus;
import com.iortatechnxt.brokerverse.booking.domain.IncentiveTrigger;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceIncentive;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.service.BookingRuleService.RuleFacts;
import com.iortatechnxt.brokerverse.booking.service.port.IncentiveAcceptanceGate;
import com.iortatechnxt.brokerverse.booking.service.port.IncentivePaymentStatus;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Incentive eligibility of a booked transaction (New Business BRNB.107, FR-NB-118; Renewal
 * BRRN.041, FR-RN-087). The indicator reads Pending until the transaction is booked and fully paid
 * (and, for a renewal, its acceptance is confirmed through the Account Officer); then the active
 * incentive criteria are evaluated on the attributes of the mother policy with the financial
 * endorsements posted on it, and the indicator becomes Eligible or Not eligible. A financial
 * endorsement re-evaluates it and a cancellation invalidates it. Every evaluation is kept with its
 * trigger, criteria, endorsements and result; the indicator is never set by hand. The endorsement
 * invoices of the family carry the indicator of their mother policy.
 */
@Service
@Transactional
public class IncentiveEvaluationService {

  private static final String SYSTEM = "SYSTEM";
  private static final String ENTITY = "BookedInvoice";

  private final BookedInvoiceRepository invoices;
  private final IncentiveEvaluationRepository evaluations;
  private final AccountRepository accounts;
  private final BookingRuleService rules;
  private final ObjectProvider<IncentivePaymentStatus> payments;
  private final ObjectProvider<IncentiveAcceptanceGate> acceptances;
  private final ApplicationEventPublisher events;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param invoices booked invoices
   * @param evaluations evaluation history
   * @param accounts accounts (attributes of the mother policy)
   * @param rules incentive criteria
   * @param payments payment status of the invoices (Operations)
   * @param acceptances acceptance of the renewals (Renewal)
   * @param events in-process events (the ledger copy of the indicator)
   * @param audit audit trail
   * @param currentUser user of the request, SYSTEM for events and jobs
   * @param clock clock
   */
  @SuppressWarnings({"java:S107", "PMD.ExcessiveParameterList"}) // constructor injection
  public IncentiveEvaluationService(
      BookedInvoiceRepository invoices,
      IncentiveEvaluationRepository evaluations,
      AccountRepository accounts,
      BookingRuleService rules,
      ObjectProvider<IncentivePaymentStatus> payments,
      ObjectProvider<IncentiveAcceptanceGate> acceptances,
      ApplicationEventPublisher events,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.invoices = invoices;
    this.evaluations = evaluations;
    this.accounts = accounts;
    this.rules = rules;
    this.payments = payments;
    this.acceptances = acceptances;
    this.events = events;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Evaluates the incentive indicator of the transaction of an invoice.
   *
   * @param invoiceNo an invoice of the family (the root is evaluated)
   * @param trigger what made it be evaluated
   * @return the indicator decided, empty when the invoice is not a booked BIBS transaction
   */
  public Optional<IncentiveStatus> evaluate(String invoiceNo, IncentiveTrigger trigger) {
    Optional<BookedInvoice> found =
        invoices.findByInvoiceNo(invoiceNo).map(i -> root(i)).filter(BookedInvoice::isBooked);
    if (found.isEmpty()) {
      return Optional.empty();
    }
    BookedInvoice root = found.get();
    List<BookedInvoice> family = invoices.findByRootInvoiceNoOrderByIdAsc(root.getInvoiceNo());
    List<String> endorsements =
        family.stream()
            .filter(i -> i.isBooked() && isEndorsement(i.getKind()))
            .map(BookedInvoice::getInvoiceNo)
            .toList();
    Decision decision = decide(root, trigger, family);
    Instant now = clock.instant();
    InvoiceIncentive decided = new InvoiceIncentive(decision.status(), now, decision.reason());
    for (BookedInvoice i : family) {
      if (i.isBooked()) {
        i.incentiveDecided(decided, decision.criteria());
      }
    }
    String by = currentUser.optionalUsername().orElse(SYSTEM);
    evaluations.save(
        new IncentiveEvaluation(
            root.getCompanyId(),
            root.getInvoiceNo(),
            trigger,
            new Outcome(
                decision.status(),
                joined(decision.criteria()),
                joined(endorsements),
                decision.reason()),
            now,
            by));
    audit.record(
        ENTITY,
        root.getInvoiceNo(),
        AuditAction.UPDATE,
        "Incentive " + decision.status().label() + ": " + decision.reason());
    accounts
        .findByArn(root.getArn())
        .ifPresent(
            a -> a.getLifecycle().incentiveDecided(decision.status() == IncentiveStatus.ELIGIBLE));
    events.publishEvent(
        new IncentiveDecided(
            root.getCompanyId(),
            family.stream()
                .filter(BookedInvoice::isBooked)
                .map(BookedInvoice::getInvoiceNo)
                .toList(),
            decision.status() == IncentiveStatus.ELIGIBLE));
    return Optional.of(decision.status());
  }

  /**
   * The evaluations of a transaction, oldest first.
   *
   * @param invoiceNo an invoice of the family
   * @return evaluations of its root
   */
  @Transactional(readOnly = true)
  public List<IncentiveEvaluation> history(String invoiceNo) {
    return invoices
        .findByInvoiceNo(invoiceNo)
        .map(i -> evaluations.findByInvoiceNoOrderByIdAsc(root(i).getInvoiceNo()))
        .orElse(List.of());
  }

  private Decision decide(
      BookedInvoice root, IncentiveTrigger trigger, List<BookedInvoice> family) {
    boolean cancelled =
        trigger == IncentiveTrigger.CANCELLATION
            || family.stream()
                .anyMatch(i -> i.isBooked() && i.getKind() == InvoiceKind.CANCELLATION);
    if (cancelled) {
      return new Decision(IncentiveStatus.NOT_ELIGIBLE, List.of(), "The booking was cancelled");
    }
    boolean paid =
        payments
            .getIfAvailable(() -> (companyId, invoiceNo) -> false)
            .fullyPaid(root.getCompanyId(), root.getInvoiceNo());
    if (!paid) {
      return new Decision(IncentiveStatus.PENDING, List.of(), "The invoice is not fully paid");
    }
    boolean accepted =
        acceptances.getIfAvailable(() -> arn -> true).acceptanceConfirmed(root.getArn());
    if (!accepted) {
      return new Decision(
          IncentiveStatus.PENDING, List.of(), "The acceptance of the client is not confirmed");
    }
    List<String> criteria = criteria(root);
    return criteria.isEmpty()
        ? new Decision(
            IncentiveStatus.NOT_ELIGIBLE, List.of(), "No active incentive criterion matches")
        : new Decision(
            IncentiveStatus.ELIGIBLE, criteria, "Matches " + String.join(", ", criteria));
  }

  /** The criteria matched by the mother policy as it stands after its endorsements. */
  private List<String> criteria(BookedInvoice root) {
    Optional<Account> account = accounts.findByArn(root.getArn());
    if (account.isEmpty()) {
      return root.getFlags().incentiveCriteriaCodes();
    }
    Account a = account.get();
    return rules.incentiveCriteria(
        root.getCompanyId(),
        new RuleFacts(
            a.getProductCode(),
            a.getCoverTypeCode(),
            a.getMarketSegment(),
            a.getSourceChannel(),
            a.getInsurerCode()),
        root.getBookingDate());
  }

  private BookedInvoice root(BookedInvoice invoice) {
    String rootNo = invoice.getRootInvoiceNo();
    if (rootNo == null || rootNo.equals(invoice.getInvoiceNo())) {
      return invoice;
    }
    return invoices.findByInvoiceNo(rootNo).orElse(invoice);
  }

  private static boolean isEndorsement(InvoiceKind kind) {
    return kind == InvoiceKind.ENDORSEMENT_PLUS || kind == InvoiceKind.ENDORSEMENT_MINUS;
  }

  private static String joined(List<String> values) {
    return values.isEmpty() ? null : String.join(",", values);
  }

  /** The indicator decided, the criteria matched and the reason. */
  private record Decision(IncentiveStatus status, List<String> criteria, String reason) {}

  /**
   * The incentive indicator of the invoices of a transaction changed (the ledger copies it).
   *
   * @param companyId company
   * @param invoiceNos invoices of the family
   * @param eligible indicator Eligible
   */
  public record IncentiveDecided(Long companyId, List<String> invoiceNos, boolean eligible) {}
}
