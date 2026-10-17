package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.config.service.ActiveConfig;
import com.iortatechnxt.brokerverse.screening.config.service.SlaMatrix;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowViewService;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The SLA of a case stage (SNSRP-108, 405; FR-SS-015, 044): on each stage entry the most specific
 * row of the SLA matrix the case started with gives the due time, the reminder lead and the
 * escalation role; the due time overrides the workflow default ({@code
 * WorkflowService.overrideDue}). Without a row the workflow's default SLA stays, with a reminder
 * lead of {@value #DEFAULT_LEAD_HOURS} hours and escalation to the Compliance Officers.
 * Working-hour calendars count calendar hours until BDOI supplies the calendar (SQ08).
 */
@Component
@Transactional
public class CaseSla {

  /** Reminder lead when no SLA row applies. */
  static final int DEFAULT_LEAD_HOURS = 4;

  /** Escalation role when no SLA row applies. */
  static final String DEFAULT_ESCALATION_ROLE = "COMPLIANCE_OFFICER";

  private final ActiveConfig config;
  private final WorkflowService workflow;
  private final WorkflowViewService workViews;

  /**
   * Creates the SLA service.
   *
   * @param config active configuration
   * @param workflow workflow (due-time override)
   * @param workViews workflow reads
   */
  public CaseSla(ActiveConfig config, WorkflowService workflow, WorkflowViewService workViews) {
    this.config = config;
    this.workflow = workflow;
    this.workViews = workViews;
  }

  /**
   * Sets the due time, lead and escalation role of the stage the case just entered.
   *
   * @param c the case (stage and entry time already mirrored)
   */
  public void apply(ScreeningCase c) {
    if (c.getStage() == CaseStage.CLOSED || c.getStage() == CaseStage.NEW) {
      c.applySla(null, null, null);
    } else {
      rule(c).ifPresentOrElse(r -> applyRule(c, r), () -> applyDefault(c));
    }
  }

  private void applyRule(ScreeningCase c, SlaMatrix.Rule r) {
    Instant due = c.getStageEnteredAt().plus(Duration.ofHours(r.slaHours()));
    c.applySla(due, r.reminderLeadHours(), r.escalateToRole());
    if (c.getWorkCaseId() != null) {
      workflow.overrideDue(
          c.getWorkCaseId(),
          due,
          "SLA matrix: " + r.slaHours() + " hours for " + c.getStage() + " (row " + r.id() + ")");
    }
  }

  private void applyDefault(ScreeningCase c) {
    Instant defaultDue =
        c.getWorkCaseId() == null ? null : workViews.get(c.getWorkCaseId()).getDueAt();
    c.applySla(defaultDue, DEFAULT_LEAD_HOURS, DEFAULT_ESCALATION_ROLE);
  }

  private Optional<SlaMatrix.Rule> rule(ScreeningCase c) {
    if (c.getSlaVersionId() == null) {
      return Optional.empty();
    }
    return config
        .slaMatrix(c.getSlaVersionId())
        .ruleFor(c.getStage().name(), c.getCaseType(), c.getRiskCategory());
  }

  /**
   * The SLA state shown on lists and the case (FR-SS-044): none, on time, due soon (inside the
   * reminder lead) or breached.
   *
   * @param c the case
   * @param now the time
   * @return the state
   */
  public static SlaState state(ScreeningCase c, Instant now) {
    if (!c.isOpen() || c.getDueAt() == null) {
      return SlaState.NONE;
    }
    if (c.isBreached() || !now.isBefore(c.getDueAt())) {
      return SlaState.BREACHED;
    }
    int lead = c.getReminderLeadHours() == null ? 0 : c.getReminderLeadHours();
    return now.isBefore(c.getDueAt().minus(Duration.ofHours(lead)))
        ? SlaState.ON_TIME
        : SlaState.DUE_SOON;
  }

  /** SLA state of a case stage. */
  public enum SlaState {
    /** No SLA (closed or no due time). */
    NONE,
    /** Before the reminder lead. */
    ON_TIME,
    /** Inside the reminder lead. */
    DUE_SOON,
    /** Past the due time. */
    BREACHED
  }
}
