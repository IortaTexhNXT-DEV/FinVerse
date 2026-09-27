package com.iortatechnxt.brokerverse.acsl.service;

import com.iortatechnxt.brokerverse.acsl.domain.AcslCaseRepository;
import com.iortatechnxt.brokerverse.acsl.domain.CaseStage;
import com.iortatechnxt.brokerverse.acsl.domain.Correction;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionRepository;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionStage;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mirrors the stage of the ACSL work cases on the case or correction, inside the transition's
 * transaction, and carries out the generic actions of the workflow panel: a correction returned
 * from review or approval keeps the comment and tells its preparer (ACSL 2.12.0-2.12.2).
 */
@Component
public class AcslStageListener {

  private final AcslCaseRepository cases;
  private final CorrectionRepository corrections;
  private final AcslNotifier notifier;

  /**
   * Creates the listener.
   *
   * @param cases cases
   * @param corrections corrections
   * @param notifier notifications
   */
  public AcslStageListener(
      AcslCaseRepository cases, CorrectionRepository corrections, AcslNotifier notifier) {
    this.cases = cases;
    this.corrections = corrections;
    this.notifier = notifier;
  }

  /**
   * Follows a stage change of an ACSL work case.
   *
   * @param event stage change
   */
  @EventListener
  public void on(WorkCaseTransitioned event) {
    if (Acsl.CASE_ENTITY.equals(event.entityType())) {
      cases
          .findById(Long.valueOf(event.entityId()))
          .ifPresent(c -> c.moveTo(CaseStage.valueOf(event.toStage())));
    } else if (Acsl.CORRECTION_ENTITY.equals(event.entityType())) {
      corrections.findById(Long.valueOf(event.entityId())).ifPresent(c -> follow(c, event));
    }
  }

  private void follow(Correction correction, WorkCaseTransitioned event) {
    correction.moveTo(CorrectionStage.valueOf(event.toStage()));
    if ("return".equals(event.action())) {
      correction.returned(event.comment());
      notifier.user(
          correction.getSubmittedBy(),
          correction.getCorrectionNo() + " returned",
          event.reasonCode() + (event.comment() == null ? "" : ": " + event.comment()),
          Acsl.correctionLink(correction.getId()),
          Acsl.CORRECTION_ENTITY,
          correction.getId());
    }
  }
}
