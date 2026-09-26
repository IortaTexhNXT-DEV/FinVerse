package com.iortatechnxt.brokerverse.eb.cycle.service;

import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleOutcome;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStageChanged;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeStatus;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Records how a cycle closed without placement (design 7.1; generic actions {@code close_lost} and
 * {@code not_renewed} of the workflow panel, reason list EB_LOST_REASON): the outcome LOST or
 * NOT_RENEWED with its reason on the cycle, and the programme becomes LOST (a new business or a
 * lost renewal) or LAPSED (a renewal not renewed).
 */
@Component
public class CycleOutcomeListener {

  private final EbCycleRepository cycles;
  private final EbRecords records;

  /**
   * Creates the listener.
   *
   * @param cycles cycles
   * @param records programme look-up
   */
  public CycleOutcomeListener(EbCycleRepository cycles, EbRecords records) {
    this.cycles = cycles;
    this.records = records;
  }

  /**
   * Records the outcome of a cycle closed as lost or not renewed.
   *
   * @param event stage change
   */
  @EventListener
  public void on(EbCycleStageChanged event) {
    if (event.to() != EbCycleStage.CLOSED_LOST && event.to() != EbCycleStage.NOT_RENEWED) {
      return;
    }
    EbCycle cycle = cycles.findById(event.cycleId()).orElseThrow();
    boolean lost = event.to() == EbCycleStage.CLOSED_LOST;
    cycle.recordOutcome(
        lost ? EbCycleOutcome.LOST : EbCycleOutcome.NOT_RENEWED, event.reasonCode(), null);
    EbProgramme programme = records.programmeOf(cycle);
    boolean renewal = cycle.getBusinessType() == BusinessType.RENEWAL;
    if (renewal && !lost) {
      programme.markStatus(EbProgrammeStatus.LAPSED);
    } else if (renewal || programme.getStatus() == EbProgrammeStatus.PROSPECT) {
      programme.markStatus(EbProgrammeStatus.LOST);
    }
  }
}
