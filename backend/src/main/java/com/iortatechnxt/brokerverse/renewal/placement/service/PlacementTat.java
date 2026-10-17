package com.iortatechnxt.brokerverse.renewal.placement.service;

import com.iortatechnxt.brokerverse.renewal.domain.RenewalPlacement;
import com.iortatechnxt.brokerverse.renewal.service.RenewalWorkingDays;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.function.Predicate;

/**
 * The placement turnaround time (FRRN.29.05): counted in business days (no Saturdays, Sundays or
 * holidays) from the placement submission date, a submission after the cut-off hour counting from
 * the next business day; up to the insurer's response, or while the placement is tagged With Issue
 * up to its resolution date; within SLA while it does not exceed the target (3 business days for a
 * packaged account, 10 for a non-package account by default).
 */
public final class PlacementTat {

  /** SLA status: within the target. */
  public static final String WITHIN = "Within SLA";

  /** SLA status: beyond the target. */
  public static final String BEYOND = "Beyond SLA";

  private static final int MAX_DAYS = 366;

  private PlacementTat() {}

  /**
   * The first day of the turnaround time.
   *
   * @param sent submission time in the business zone
   * @param cutoffHour hour after which the next business day counts
   * @param working business days
   * @return the submission date, or the next business day
   */
  public static LocalDate start(ZonedDateTime sent, int cutoffHour, Predicate<LocalDate> working) {
    LocalDate day = sent.toLocalDate();
    if (sent.getHour() >= cutoffHour || !working.test(day)) {
      day = next(day, working);
    }
    return day;
  }

  /**
   * The turnaround time of a placement.
   *
   * @param p placement
   * @param today today
   * @param working business days
   * @return days and SLA status, null before the placement is sent
   */
  public static Measure measure(RenewalPlacement p, LocalDate today, Predicate<LocalDate> working) {
    if (p.getTatStart() == null || p.getSlaDays() == null) {
      return null;
    }
    LocalDate end = end(p, today);
    int days = RenewalWorkingDays.between(p.getTatStart(), end, working);
    return new Measure(days, p.getSlaDays(), days <= p.getSlaDays() ? WITHIN : BEYOND, end);
  }

  private static LocalDate end(RenewalPlacement p, LocalDate today) {
    if (p.isWithIssue()) {
      return p.getResolutionDate() == null ? today : p.getResolutionDate();
    }
    return p.getResponseDate() == null ? today : p.getResponseDate();
  }

  private static LocalDate next(LocalDate day, Predicate<LocalDate> working) {
    LocalDate d = day.plusDays(1);
    for (int i = 0; i < MAX_DAYS && !working.test(d); i++) {
      d = d.plusDays(1);
    }
    return d;
  }

  /**
   * A turnaround time.
   *
   * @param days business days elapsed
   * @param target target in business days
   * @param status Within SLA or Beyond SLA
   * @param end last day counted
   */
  public record Measure(int days, int target, String status, LocalDate end) {}
}
