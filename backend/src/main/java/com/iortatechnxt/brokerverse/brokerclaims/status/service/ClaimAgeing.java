package com.iortatechnxt.brokerverse.brokerclaims.status.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Claim dates and ages (BRCLM.025/027, FR-CL-053; CLAIMS_BROKING_DESIGN 8.1): business dates are
 * Philippine dates; ages are calendar days computed on read. Age overall runs from the reported
 * date (day zero) to the as-of date, or to the closure date of a closed claim; age this stage runs
 * from the day the current status was set. Temporarily closed claims keep ageing (CLQ06). The
 * buckets come from parameter {@code BCL_AGEING_BUCKETS} (upper bounds 30, 60, 90, 180 give 0-30,
 * 31-60, 61-90, 91-180 and 181+).
 */
public final class ClaimAgeing {

  /** Time zone of the business dates. */
  public static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  private ClaimAgeing() {}

  /**
   * Today in the Philippines.
   *
   * @param clock clock
   * @return business date
   */
  public static LocalDate today(Clock clock) {
    return LocalDate.now(clock.withZone(MANILA));
  }

  /**
   * The Philippine date of an instant.
   *
   * @param at instant
   * @return date, null when {@code at} is null
   */
  public static LocalDate dateOf(Instant at) {
    return at == null ? null : at.atZone(MANILA).toLocalDate();
  }

  /**
   * Calendar days between two dates, never negative.
   *
   * @param from first day (day zero)
   * @param to last day
   * @return days
   */
  public static int days(LocalDate from, LocalDate to) {
    return (int) Math.max(0, ChronoUnit.DAYS.between(from, to));
  }

  /**
   * Age overall of a claim (BRCLM.025).
   *
   * @param reportedDate reported date
   * @param closedOn closure date of a permanently closed claim, else null
   * @param asOf as-of date
   * @return days from the reported date to the closure or as-of date, whichever is first
   */
  public static int ageOverall(LocalDate reportedDate, LocalDate closedOn, LocalDate asOf) {
    LocalDate end = closedOn != null && closedOn.isBefore(asOf) ? closedOn : asOf;
    return days(reportedDate, end);
  }

  /**
   * Age this stage (BRCLM.027).
   *
   * @param statusSince time the current status was set, null before the first status
   * @param asOf as-of date
   * @return days in the current status (0 when there is none)
   */
  public static int ageThisStage(Instant statusSince, LocalDate asOf) {
    return statusSince == null ? 0 : days(dateOf(statusSince), asOf);
  }

  /**
   * The ageing bucket of an age.
   *
   * @param age days
   * @param bounds increasing upper bounds, e.g. 30, 60, 90, 180
   * @return label such as {@code 0-30}, {@code 31-60} or {@code 181+}
   */
  public static String bucket(int age, List<Integer> bounds) {
    int low = 0;
    for (int bound : bounds) {
      if (age <= bound) {
        return low + "-" + bound;
      }
      low = bound + 1;
    }
    return low + "+";
  }

  /**
   * Every bucket label in order.
   *
   * @param bounds increasing upper bounds
   * @return labels, the open-ended one last
   */
  public static List<String> buckets(List<Integer> bounds) {
    List<String> labels = new ArrayList<>();
    int low = 0;
    for (int bound : bounds) {
      labels.add(low + "-" + bound);
      low = bound + 1;
    }
    labels.add(low + "+");
    return List.copyOf(labels);
  }
}
