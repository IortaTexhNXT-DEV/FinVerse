package com.iortatechnxt.brokerverse.collections.escalation.service;

import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.Basis;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationRule;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationRuleRepository;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationCandidates.Candidate;
import com.iortatechnxt.brokerverse.collections.escalation.service.RuleMatcher.Signals;
import com.iortatechnxt.brokerverse.collections.installment.domain.Installment;
import com.iortatechnxt.brokerverse.collections.installment.domain.InstallmentRepository;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.InstallmentStatus;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.PlanStatus;
import com.iortatechnxt.brokerverse.collections.installment.service.LedgerBalances;
import com.iortatechnxt.brokerverse.collections.promise.domain.PaymentPromiseRepository;
import com.iortatechnxt.brokerverse.collections.promise.domain.PromiseStatus;
import com.iortatechnxt.brokerverse.collections.promise.service.PromiseBroken;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies the escalation rules (BRCLXN.049): the open accounts that meet a rule on a business date,
 * with what Collections knows of them (promises, installments). Used by the job {@code
 * CLX_ESCALATION}, and at once when a promise is broken (BRCLXN.055: "a broken promise escalates to
 * the team lead" through the BROKEN_PROMISES_COUNT rules).
 */
@Component
public class EscalationEngine {

  private final EscalationRuleRepository rules;
  private final EscalationCandidates candidates;
  private final PaymentPromiseRepository promises;
  private final InstallmentRepository installments;
  private final LedgerBalances ledger;
  private final EscalationService escalations;
  private final Clock clock;

  /**
   * Creates the engine.
   *
   * @param rules escalation rules
   * @param candidates open accounts
   * @param promises promises
   * @param installments installments
   * @param ledger ledger reads (threshold)
   * @param escalations escalation cases
   * @param clock clock
   */
  public EscalationEngine(
      EscalationRuleRepository rules,
      EscalationCandidates candidates,
      PaymentPromiseRepository promises,
      InstallmentRepository installments,
      LedgerBalances ledger,
      EscalationService escalations,
      Clock clock) {
    this.rules = rules;
    this.candidates = candidates;
    this.promises = promises;
    this.installments = installments;
    this.ledger = ledger;
    this.escalations = escalations;
    this.clock = clock;
  }

  /**
   * The authorized rules in force on a date.
   *
   * @param asOf business date
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<EscalationRule> rulesOn(LocalDate asOf) {
    return rules.findByRecordStatusOrderByCompanyIdAscCodeAsc(RecordStatus.ACTIVE).stream()
        .filter(r -> r.appliesOn(asOf))
        .toList();
  }

  /**
   * The open accounts a rule escalates on a date.
   *
   * @param rule rule
   * @param asOf business date
   * @return accounts
   */
  @Transactional(readOnly = true)
  public List<Candidate> matches(EscalationRule rule, LocalDate asOf) {
    List<Candidate> open = candidates.open(rule.getCompanyId(), rule.filters(), ledger.threshold());
    SignalBook book = signals(rule.getCompanyId(), rule.getBasis(), asOf);
    return open.stream()
        .filter(
            c ->
                RuleMatcher.matches(
                    rule.getBasis(), rule.getThreshold(), c, book.of(c.invoiceNo()), asOf))
        .toList();
  }

  /**
   * Escalates an account whose promise was just broken, by the company's BROKEN_PROMISES_COUNT
   * rules in force, inside the evaluating transaction.
   *
   * @param event broken promise
   */
  @EventListener
  public void on(PromiseBroken event) {
    LocalDate today = LocalDate.now(clock);
    rulesOn(today).stream()
        .filter(r -> r.getCompanyId().equals(event.companyId()))
        .filter(r -> r.getBasis() == Basis.BROKEN_PROMISES_COUNT)
        .forEach(
            r ->
                candidates
                    .one(event.companyId(), event.invoiceNo(), r.filters(), ledger.threshold())
                    .stream()
                    .filter(
                        c ->
                            RuleMatcher.matches(
                                r.getBasis(),
                                r.getThreshold(),
                                c,
                                signals(r.getCompanyId(), r.getBasis(), today).of(c.invoiceNo()),
                                today))
                    .forEach(c -> escalations.raiseForRule(r, c, today)));
  }

  private SignalBook signals(Long companyId, Basis basis, LocalDate asOf) {
    return switch (basis) {
      case BROKEN_PROMISES_COUNT -> {
        Map<String, Long> counts = new HashMap<>();
        promises
            .countByInvoice(companyId, PromiseStatus.BROKEN)
            .forEach(row -> counts.put((String) row[0], ((Number) row[1]).longValue()));
        yield new SignalBook(counts, Set.of(), Map.of());
      }
      case NO_COMMITMENT_BY_DAY ->
          new SignalBook(
              Map.of(),
              new HashSet<>(promises.invoicesWith(companyId, PromiseStatus.OPEN)),
              Map.of());
      case INSTALLMENT_OVERDUE_DAYS -> {
        Map<String, Long> days = new HashMap<>();
        for (Installment i :
            installments.inStatus(companyId, PlanStatus.ACTIVE, InstallmentStatus.OVERDUE)) {
          days.merge(i.getInvoiceNo(), RuleMatcher.days(i.getDueDate(), asOf), Math::max);
        }
        yield new SignalBook(Map.of(), Set.of(), days);
      }
      default -> new SignalBook(Map.of(), Set.of(), Map.of());
    };
  }

  /**
   * The signals of every account of a company for one basis.
   *
   * @param broken broken promises per invoice
   * @param open invoices with an open promise
   * @param overdue days overdue per invoice
   */
  private record SignalBook(Map<String, Long> broken, Set<String> open, Map<String, Long> overdue) {

    Signals of(String invoiceNo) {
      return new Signals(
          broken.getOrDefault(invoiceNo, 0L),
          open.contains(invoiceNo),
          overdue.getOrDefault(invoiceNo, 0L));
    }
  }
}
