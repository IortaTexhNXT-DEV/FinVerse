package com.iortatechnxt.brokerverse.eb.service;

import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStageChanged;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mirrors the {@code EB_CYCLE} work case stage on the cycle (design 7.1) for every transition,
 * including the generic ones run from the workflow panel ({@code close_lost}, {@code not_renewed}),
 * and publishes {@link EbCycleStageChanged}.
 */
@Component
public class EbCycleMirror {

  private final EbCycleRepository cycles;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  /**
   * Creates the listener.
   *
   * @param cycles cycles
   * @param events event publisher
   * @param clock clock
   */
  public EbCycleMirror(EbCycleRepository cycles, ApplicationEventPublisher events, Clock clock) {
    this.cycles = cycles;
    this.events = events;
    this.clock = clock;
  }

  /**
   * Mirrors a stage change of a cycle's work case.
   *
   * @param event transition
   */
  @EventListener
  public void on(WorkCaseTransitioned event) {
    if (!EbCodes.ENTITY_CYCLE.equals(event.entityType())) {
      return;
    }
    EbCycle cycle =
        cycles
            .findById(Long.valueOf(event.entityId()))
            .orElseThrow(() -> new IllegalStateException("No EB cycle " + event.entityId()));
    EbCycleStage from = cycle.getStage();
    EbCycleStage to = EbCycleStage.valueOf(event.toStage());
    cycle.mirror(to, clock.instant());
    events.publishEvent(
        new EbCycleStageChanged(
            cycle.getId(),
            cycle.getCycleNo(),
            cycle.getProgrammeId(),
            from,
            to,
            event.action(),
            event.reasonCode()));
  }
}
