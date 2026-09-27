package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCaseRepository;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import java.time.Clock;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mirrors the stage of workflow {@code SCR_CASE} on the screening case (SNSRP-401; Developer
 * pattern "mirror the stage with an {@code @EventListener} on {@code WorkCaseTransitioned}"): the
 * stage and its entry time, open or closed, the assignee cleared (the case service assigns the next
 * owner) and the SLA of the new stage entry (SNSRP-108).
 */
@Component
public class CaseStageMirror {

  private final ScreeningCaseRepository cases;
  private final CaseSla sla;
  private final Clock clock;

  /**
   * Creates the mirror.
   *
   * @param cases cases
   * @param sla stage SLA
   * @param clock clock
   */
  public CaseStageMirror(ScreeningCaseRepository cases, CaseSla sla, Clock clock) {
    this.cases = cases;
    this.sla = sla;
    this.clock = clock;
  }

  /**
   * Mirrors a transition of a screening case.
   *
   * @param event the transition
   */
  @EventListener
  public void on(WorkCaseTransitioned event) {
    if (!CaseCodes.ENTITY.equals(event.entityType())) {
      return;
    }
    cases
        .findById(Long.valueOf(event.entityId()))
        .ifPresent(
            c -> {
              c.enter(CaseStage.valueOf(event.toStage()), clock.instant());
              c.assignTo(null);
              sla.apply(c);
            });
  }
}
