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
                EnumSet.of(UnappliedOrigin.NO_MATCH, UnappliedOrigin.PREBOOKED))
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
    Optional<Payment> payment =
        item.getPaymentId() == null ? Optional.empty() : payments.findById(item.getPaymentId());
    if (payment.isEmpty() || item.getBalance().signum() <= 0) {
      return false;
    }
    Match match = matcher.match(item.getCompanyId(), references(payment.get()));
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

  private static List<String> references(Payment payment) {
    List<String> refs = new ArrayList<>();
    if (payment.getReference() != null) {
      refs.add(payment.getReference());
    }
    if (payment.getOtherRefs() != null) {
      refs.addAll(Arrays.asList(payment.getOtherRefs().split(",")));
    }
    return refs;
  }
}
