package com.iortatechnxt.brokerverse.eb.tracked.service;

import com.iortatechnxt.brokerverse.eb.service.EbWorkingDays;
import java.time.LocalDate;
import java.util.function.Predicate;

/**
 * When a pending item is followed up (BRID-030; FR-EB-057 R1, R2): the n-th follow-up is due {@code
 * n x EB_FOLLOWUP_DAYS} working days after the due date; after {@code EB_FOLLOWUP_MAX} follow-ups
 * the next step escalates to the AO instead.
 *
 * @param intervalDays working days between follow-ups
 * @param maxFollowUps follow-ups before escalation
 */
public record FollowUpSchedule(int intervalDays, int maxFollowUps) {

  /** What is due for an item on a date. */
  public enum Step {
    /** Nothing yet. */
    NONE,
    /** Send the next follow-up. */
    FOLLOW_UP,
    /** Escalate (after the last follow-up). */
    ESCALATE
  }

  /**
   * The step due on a date.
   *
   * @param dueDate due date of the item
   * @param followUpsSent follow-ups already sent
   * @param escalated whether the item is already escalated
   * @param today business date
   * @param working working-day calendar
   * @return the step
   */
  public Step stepOn(
      LocalDate dueDate,
      int followUpsSent,
      boolean escalated,
      LocalDate today,
      Predicate<LocalDate> working) {
    if (!today.isAfter(dueDate) || escalated) {
      return Step.NONE;
    }
    LocalDate next = nextDate(dueDate, followUpsSent, working);
    if (today.isBefore(next)) {
      return Step.NONE;
    }
    return followUpsSent < maxFollowUps ? Step.FOLLOW_UP : Step.ESCALATE;
  }

  /**
   * The date of the next step.
   *
   * @param dueDate due date of the item
   * @param followUpsSent follow-ups already sent
   * @param working working-day calendar
   * @return the date the next follow-up (or the escalation) is due
   */
  public LocalDate nextDate(LocalDate dueDate, int followUpsSent, Predicate<LocalDate> working) {
    return EbWorkingDays.plus(dueDate, Math.max(1, intervalDays) * (followUpsSent + 1), working);
  }
}
