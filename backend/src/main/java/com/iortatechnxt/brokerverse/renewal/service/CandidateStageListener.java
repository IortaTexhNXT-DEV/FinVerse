package com.iortatechnxt.brokerverse.renewal.service;

import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mirrors the stage of the {@code RNW_CASE} work case on the renewal candidate, in the transaction
 * of the transition (RENEWAL_DESIGN section 7.1), whoever moved it.
 */
@Component
public class CandidateStageListener {

  private final RenewalCandidateRepository candidates;

  /**
   * Creates the listener.
   *
   * @param candidates candidates
   */
  public CandidateStageListener(RenewalCandidateRepository candidates) {
    this.candidates = candidates;
  }

  /**
   * Mirrors a stage change.
   *
   * @param event stage change
   */
  @EventListener
  public void on(WorkCaseTransitioned event) {
    if (!RenewalCodes.WORKFLOW.equals(event.workflowCode())) {
      return;
    }
    candidates
        .findById(Long.valueOf(event.entityId()))
        .ifPresent(c -> c.enter(RenewalStage.valueOf(event.toStage())));
  }
}
