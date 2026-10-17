package com.iortatechnxt.brokerverse.workflow.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertCheck;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCase;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/** Daily alert check: work items past their stage SLA ({@code WORK_SLA_BREACH}). */
@Component
public class WorkSlaAlertCheck implements AlertCheck {

  /** Exception code. */
  public static final String CODE = "WORK_SLA_BREACH";

  private final WorkQueueService queues;

  /**
   * Creates the check.
   *
   * @param queues work queues
   */
  public WorkSlaAlertCheck(WorkQueueService queues) {
    this.queues = queues;
  }

  @Override
  public List<AlertSignal> evaluate(LocalDate asOf) {
    return queues.overdue().stream().map(WorkSlaAlertCheck::signal).toList();
  }

  private static AlertSignal signal(WorkCase c) {
    return new AlertSignal(
        CODE,
        new AlertFacts(
            c.getCompanyId(),
            null,
            c.getEntityType(),
            c.getReference(),
            c.getReference() + " (" + c.getTitle() + ") is overdue in stage " + c.getStageCode(),
            null,
            CODE + ":" + c.getId() + ":" + c.getStageCode() + ":" + c.getStageEnteredAt()));
  }
}
