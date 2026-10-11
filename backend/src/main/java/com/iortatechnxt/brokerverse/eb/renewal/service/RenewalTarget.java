package com.iortatechnxt.brokerverse.eb.renewal.service;

import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * What a renewal advice announces (BRID-001): the earliest expiry of the programme's active lines
 * on or after a date (and, for the job, not later than the lead time), and the policy year of the
 * renewal term that starts then.
 *
 * @param expiry expiry date of the lines
 * @param policyYear policy year of the renewal (the expiry's year)
 */
public record RenewalTarget(LocalDate expiry, int policyYear) {

  /**
   * The next expiry of a programme.
   *
   * @param programme programme
   * @param from first date considered
   * @param until last date considered, null for no limit
   * @return the target, empty when no active line ends in the window
   */
  public static Optional<RenewalTarget> of(EbProgramme programme, LocalDate from, LocalDate until) {
    return programme.getLines().stream()
        .filter(EbProgrammeLine::isActive)
        .map(EbProgrammeLine::getPeriodTo)
        .filter(Objects::nonNull)
        .filter(d -> !d.isBefore(from) && (until == null || !d.isAfter(until)))
        .min(LocalDate::compareTo)
        .map(d -> new RenewalTarget(d, d.getYear()));
  }

  /**
   * The reminders due on a date: how many of the reminder days before expiry have been reached.
   *
   * @param expiry expiry announced
   * @param reminderDays days before expiry of the reminders
   * @param today business date
   * @return number of reminders that should have been sent by today
   */
  public static int remindersDue(
      LocalDate expiry, java.util.List<Integer> reminderDays, LocalDate today) {
    return (int) reminderDays.stream().filter(d -> !today.isBefore(expiry.minusDays(d))).count();
  }
}
