package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.remittance.domain.HoldRequestRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatchRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.HoldStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.SpecialStage;
import com.iortatechnxt.brokerverse.remittance.domain.SpecialRemittanceRepository;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mirrors the stage of the remittance workflows on their records (RMTID.019/036): batches
 * (OPS_REMITTANCE), holds (OPS_HOLD) and special remittance requests (OPS_SPECIAL_REMIT), for every
 * transition, including the generic ones run from the workflow panel (hold, release, send back).
 */
@Component
public class RemittanceStageListener {

  private final RemittanceBatchRepository batches;
  private final HoldRequestRepository holds;
  private final SpecialRemittanceRepository specials;

  /**
   * Creates the listener.
   *
   * @param batches batches
   * @param holds hold requests
   * @param specials special remittance requests
   */
  public RemittanceStageListener(
      RemittanceBatchRepository batches,
      HoldRequestRepository holds,
      SpecialRemittanceRepository specials) {
    this.batches = batches;
    this.holds = holds;
    this.specials = specials;
  }

  /**
   * Mirrors a stage change.
   *
   * @param event transition
   */
  @EventListener
  public void on(WorkCaseTransitioned event) {
    Long id =
        switch (event.entityType()) {
          case BatchService.ENTITY, HoldService.ENTITY, SpecialRemittanceService.ENTITY ->
              Long.valueOf(event.entityId());
          default -> null;
        };
    if (id == null) {
      return;
    }
    switch (event.entityType()) {
      case BatchService.ENTITY ->
          batches.findById(id).ifPresent(b -> b.markStage(BatchStage.valueOf(event.toStage())));
      case HoldService.ENTITY ->
          holds.findById(id).ifPresent(h -> h.markStage(HoldStage.valueOf(event.toStage())));
      default ->
          specials.findById(id).ifPresent(s -> s.markStage(SpecialStage.valueOf(event.toStage())));
    }
  }
}
