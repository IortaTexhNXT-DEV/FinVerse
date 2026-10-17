package com.iortatechnxt.brokerverse.collections.escalation.service;

import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.Basis;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationCandidates.Candidate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Whether an open collection account meets an escalation rule on a business date (BRCLXN.049):
 * aging from booking or inception, no open promise by a day, broken promises, overdue installment
 * days, or an outstanding at or above an amount.
 */
public final class RuleMatcher {

  private RuleMatcher() {}

  /**
   * Evaluates one rule on one account.
   *
   * @param basis rule basis
   * @param threshold days, count or amount
   * @param account the open account
   * @param signals promises and installments of the account
   * @param asOf business date
   * @return true when the account is to be escalated
   */
  public static boolean matches(
      Basis basis, BigDecimal threshold, Candidate account, Signals signals, LocalDate asOf) {
    return switch (basis) {
      case AGING_FROM_BOOKING -> reached(days(account.dates().booking(), asOf), threshold);
      case AGING_FROM_INCEPTION -> reached(days(account.dates().inception(), asOf), threshold);
      case NO_COMMITMENT_BY_DAY ->
          !signals.openPromise() && reached(days(account.dates().booking(), asOf), threshold);
      case BROKEN_PROMISES_COUNT -> reached(signals.brokenPromises(), threshold);
      case INSTALLMENT_OVERDUE_DAYS -> reached(signals.overdueDays(), threshold);
      case AMOUNT_OVER -> account.balance().compareTo(threshold) >= 0;
    };
  }

  /**
   * Days from a date to the business date.
   *
   * @param from start
   * @param asOf business date
   * @return days, zero when in the future
   */
  public static long days(LocalDate from, LocalDate asOf) {
    return Math.max(0, ChronoUnit.DAYS.between(from, asOf));
  }

  private static boolean reached(long value, BigDecimal threshold) {
    return BigDecimal.valueOf(value).compareTo(threshold) >= 0;
  }

  /**
   * What Collections knows of an account besides the ledger.
   *
   * @param brokenPromises number of broken promises
   * @param openPromise whether a promise to pay is running
   * @param overdueDays days the oldest overdue installment is past due, zero when none
   */
  public record Signals(long brokenPromises, boolean openPromise, long overdueDays) {

    /** Nothing known. */
    public static final Signals NONE = new Signals(0, false, 0);
  }
}
