package com.iortatechnxt.brokerverse.collections.escalation.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertCheck;
import com.iortatechnxt.brokerverse.collections.escalation.domain.Escalation;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code CLX_ESCALATION_OVERDUE} (BRCLXN.049): an open escalation stayed in its stage longer than
 * the SLA of its rule (or of the stage for a manual escalation). Evaluated by the daily alert job;
 * one alert per escalation and stage.
 */
@Component
public class EscalationOverdueCheck implements AlertCheck {

  /** Exception code. */
  public static final String CODE = "CLX_ESCALATION_OVERDUE";

  private final EscalationRepository escalations;
  private final Clock clock;

  /**
   * Creates the check.
   *
   * @param escalations escalations
   * @param clock clock
   */
  public EscalationOverdueCheck(EscalationRepository escalations, Clock clock) {
    this.escalations = escalations;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public List<AlertSignal> evaluate(LocalDate asOf) {
    Instant now = clock.instant();
    return escalations.findByStatusIn(EscalationService.OPEN).stream()
        .filter(e -> e.isOverdue(now))
        .map(EscalationOverdueCheck::signal)
        .toList();
  }

  private static AlertSignal signal(Escalation e) {
    return new AlertSignal(
        CODE,
        new AlertFacts(
            e.getCompanyId(),
            null,
            EscalationService.ENTITY,
            e.getEscalationNo(),
            e.getEscalationNo()
                + " ("
                + e.getAssuredName()
                + ") is past its "
                + e.getSlaHours()
                + "-hour SLA in stage "
                + e.getStatus(),
            e.getTotalBalance(),
            CODE + ":" + e.getId() + ":" + e.getStatus() + ":" + e.getStageSince()));
  }
}
