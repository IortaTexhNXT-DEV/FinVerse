package com.iortatechnxt.brokerverse.frbs.service;

import com.iortatechnxt.brokerverse.frbs.domain.FrbsEnums.RunStage;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeItem;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeItemRepository;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLine;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLineRepository;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRunRepository;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mirrors the stage of a run's work case on the run, inside the transition's transaction (FRBS
 * 2.10; design 7.3), and carries out the generic cancellation of the workflow panel: the lines are
 * cancelled and the invoices freed for another run.
 */
@Component
public class ServiceFeeStageListener {

  private final ServiceFeeRunRepository runs;
  private final ServiceFeeLineRepository lines;
  private final ServiceFeeItemRepository items;

  /**
   * Creates the listener.
   *
   * @param runs runs
   * @param lines lines
   * @param items invoices
   */
  public ServiceFeeStageListener(
      ServiceFeeRunRepository runs,
      ServiceFeeLineRepository lines,
      ServiceFeeItemRepository items) {
    this.runs = runs;
    this.lines = lines;
    this.items = items;
  }

  /**
   * Follows a stage change of a run's work case.
   *
   * @param event stage change
   */
  @EventListener
  public void on(WorkCaseTransitioned event) {
    if (!ServiceFees.ENTITY.equals(event.entityType())) {
      return;
    }
    runs.findById(Long.valueOf(event.entityId()))
        .ifPresent(
            run -> {
              RunStage stage = RunStage.valueOf(event.toStage());
              run.moveTo(stage);
              if (stage == RunStage.CANCELLED) {
                lines.findByRunIdOrderByLineNoAsc(run.getId()).forEach(ServiceFeeLine::cancel);
                items.findByRunIdOrderByInvoiceNoAsc(run.getId()).forEach(ServiceFeeItem::release);
              }
            });
  }
}
