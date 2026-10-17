package com.iortatechnxt.brokerverse.alert.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.domain.ExceptionCode;
import com.iortatechnxt.brokerverse.approval.service.ApprovalInboxService;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * PENDING_APPROVAL_AGEING: items of the universal approval inbox (every module's sources) waiting
 * longer than the threshold days.
 */
@Component
public class PendingApprovalAgeingCheck implements AlertCheck {

  /** Exception code. */
  public static final String CODE = "PENDING_APPROVAL_AGEING";

  private static final int DEFAULT_DAYS = 2;

  private final ApprovalInboxService inbox;
  private final AlertService alerts;
  private final Clock clock;

  /**
   * Creates the check.
   *
   * @param inbox approval inbox
   * @param alerts alert service (threshold)
   * @param clock clock
   */
  public PendingApprovalAgeingCheck(ApprovalInboxService inbox, AlertService alerts, Clock clock) {
    this.inbox = inbox;
    this.alerts = alerts;
    this.clock = clock;
  }

  @Override
  public List<AlertSignal> evaluate(LocalDate asOf) {
    Optional<ExceptionCode> code = alerts.activeCode(CODE);
    if (code.isEmpty()) {
      return List.of();
    }
    int days = Optional.ofNullable(code.get().getThresholdDays()).orElse(DEFAULT_DAYS);
    Instant limit = clock.instant().minus(Duration.ofDays(days));
    return inbox.pendingAll().stream()
        .filter(i -> i.submittedAt() != null && i.submittedAt().isBefore(limit))
        .map(this::signal)
        .toList();
  }

  private AlertSignal signal(PendingApproval item) {
    long waiting = Duration.between(item.submittedAt(), clock.instant()).toDays();
    return new AlertSignal(
        CODE,
        new AlertFacts(
            item.companyId(),
            null,
            item.type(),
            item.reference(),
            item.type()
                + " "
                + item.reference()
                + " submitted by "
                + item.submittedBy()
                + " has waited "
                + waiting
                + " day(s) for approval",
            item.amount(),
            CODE + ":" + item.module() + ":" + item.type() + ":" + item.reference()));
  }
}
