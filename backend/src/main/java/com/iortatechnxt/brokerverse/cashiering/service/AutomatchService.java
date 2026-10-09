package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ApplicationSource;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.UnappliedOrigin;
import com.iortatechnxt.brokerverse.cashiering.domain.Payment;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedRepository;
import com.iortatechnxt.brokerverse.cashiering.service.ApplicationService.ApplyOptions;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentMatcher.Kind;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentMatcher.Match;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Automatic matching of unapplied payments (CSHID.008 item 3, {@code PAYMENT_AUTOMATCH}): payments
 * that found no booked invoice at acceptance are matched again with their references; when the
 * invoice is booked now, the unapplied balance is applied and a fully used item is closed.
 * Unapplied payments carried from legacy are matched with their legacy references, against legacy
 * and new invoices alike.
 */
@Service
@Transactional
public class AutomatchService {

  private final UnappliedRepository items;
  private final PaymentRepository payments;
  private final PaymentMatcher matcher;
  private final PaymentIntakeService intake;
  private final UnappliedService unapplied;
  private final ItemTransactions transactions;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param items unapplied items
   * @param payments payments
   * @param matcher matching
   * @param intake application oldest first
   * @param unapplied unapplied workbench
   * @param transactions one transaction per item
   * @param audit audit trail
   */
  public AutomatchService(
      UnappliedRepository items,
      PaymentRepository payments,
      PaymentMatcher matcher,
      PaymentIntakeService intake,
      UnappliedService unapplied,
      ItemTransactions transactions,
      AuditTrailService audit) {
    this.items = items;
    this.payments = payments;
    this.matcher = matcher;
    this.intake = intake;
    this.unapplied = unapplied;
    this.transactions = transactions;
    this.audit = audit;
  }

  /**
   * Re-matches every unmatched payment still in the Unapplied tab.
   *
   * @param businessDate business date
   * @return items applied
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public int run(LocalDate businessDate) {
    List<Long> ids =
        items
            .findByStageAndOriginInOrderByIdAsc(
                Unapplied.STAGE_INITIAL,
                EnumSet.of(
                    UnappliedOrigin.NO_MATCH, UnappliedOrigin.PREBOOKED, UnappliedOrigin.MIGRATED))
            .stream()
            .map(Unapplied::getId)
            .toList();
    int applied = 0;
    for (Long id : ids) {
      if (transactions.run("AUTO:" + id, () -> rematch(unapplied.get(id), businessDate))) {
        applied++;
      }
    }
    return applied;
  }

  private boolean rematch(Unapplied item, LocalDate date) {
    return applyItem(item, referencesOf(item), date);
  }

  /**
   * Applies an unapplied payment to the booked account of references given (Direct Credit file,
   * FRS.CSH.05.01.09 priority 2).
   *
   * @param item unapplied payment of the payment identified
   * @param refs references of the Direct Credit row
   * @param date value date of the application
   * @return true when money was applied
   */
  public boolean identify(Unapplied item, List<String> refs, LocalDate date) {
    return applyItem(item, refs, date);
  }

  private boolean applyItem(Unapplied item, List<String> refs, LocalDate date) {
    if (refs.isEmpty() || item.getBalance().signum() <= 0) {
      return false;
    }
    Match match = matcher.match(item.getCompanyId(), refs);
    if (match.kind() != Kind.BOOKED) {
      return false;
    }
    List<Application> made =
        intake.applyOldestFirst(
            match.invoices(),
            item.getBalance(),
            new Application.Origin(
                item.getReceiptId(),
                item.getId(),
                ApplicationSource.AUTOMATCH,
                item.getReference()),
            new ApplyOptions(date, false, null));
    BigDecimal applied =
        made.stream().map(Application::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (applied.signum() <= 0) {
      return false;
    }
    item.consume(applied);
    if (item.getBalance().signum() == 0) {
      unapplied.close(item, "auto_apply", "Applied to " + match.reference());
    }
    audit.record(
        UnappliedService.ENTITY,
        item.getReference(),
        AuditAction.UPDATE,
        "Automatch applied " + applied + " to " + match.reference());
    return true;
  }

  /**
   * The references of an item: those of its payment, or for an unapplied payment carried from
   * legacy its invoice, cover, PN and bank references (DATA_MIGRATION_DESIGN 14.4 B).
   */
  private List<String> referencesOf(Unapplied item) {
    if (item.getPaymentId() == null) {
      return item.getLegacy().references();
    }
    Optional<Payment> payment = payments.findById(item.getPaymentId());
    return payment.map(AutomatchService::references).orElse(List.of());
  }

  private static List<String> references(Payment payment) {
    List<String> refs = new ArrayList<>();
    if (payment.getReference() != null) {
      refs.add(payment.getReference());
    }
    if (payment.getOtherRefs() != null) {
      refs.addAll(Arrays.asList(payment.getOtherRefs().split(",")));
    }
    if (payment.getMatchedRef() != null && !refs.contains(payment.getMatchedRef())) {
      refs.add(payment.getMatchedRef());
    }
    return refs;
  }
}
