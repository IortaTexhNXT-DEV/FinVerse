package com.iortatechnxt.brokerverse.finreport.service;

import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Basis;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Comparative;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Measure;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Side;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleQueries.Increase;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleQueries.RowFigures;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleQueries.Window;
import com.iortatechnxt.brokerverse.subledger.service.AgeingSlots;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Pure calculations of the account schedule engine (FRBS 3.2.0): the dates of a run, the figures of
 * a row on the schedule's side and the FIFO ageing of a balance.
 */
public final class ScheduleMath {

  /** Lower bound of an open-ended comparative balance. */
  static final LocalDate BEGINNING = LocalDate.of(1900, 1, 1);

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final int PERCENT_SCALE = 2;
  private static final int MONTHS_IN_YEAR = 12;

  private ScheduleMath() {}

  /**
   * The dates of a run: the period, the fiscal year start and the comparative window (the balance
   * at the shifted as-of date, or the movement of the shifted period).
   *
   * @param from first day of the period
   * @param asOf as-of date
   * @param yearStartMonth first month of the fiscal year
   * @param basis balance or movement
   * @param comparative comparative period
   * @return window
   */
  public static Window window(
      LocalDate from, LocalDate asOf, int yearStartMonth, Basis basis, Comparative comparative) {
    LocalDate yearStart = LocalDate.of(asOf.getYear(), yearStartMonth, 1);
    if (yearStart.isAfter(asOf)) {
      yearStart = yearStart.minusYears(1);
    }
    int months = comparative == Comparative.PREVIOUS_YEAR ? MONTHS_IN_YEAR : 1;
    LocalDate cmpTo = shift(asOf, months);
    LocalDate cmpFrom = basis == Basis.BALANCE ? BEGINNING : shift(from, months);
    if (comparative == Comparative.NONE) {
      cmpFrom = BEGINNING;
      cmpTo = BEGINNING;
    }
    return new Window(from, asOf, yearStart, cmpFrom, cmpTo);
  }

  /** Moves a date back by months; a month end stays a month end. */
  private static LocalDate shift(LocalDate date, int months) {
    LocalDate shifted = date.minusMonths(months);
    boolean monthEnd = date.getDayOfMonth() == date.lengthOfMonth();
    return monthEnd ? YearMonth.from(shifted).atEndOfMonth() : shifted;
  }

  /**
   * The figures of a row on the schedule's side (debit schedules show debit balances positive).
   *
   * @param f signed figures (debit minus credit)
   * @param side side shown positive
   * @param basis balance or movement
   * @return every measure
   */
  public static Map<Measure, BigDecimal> measures(RowFigures f, Side side, Basis basis) {
    BigDecimal sign = side == Side.DEBIT ? BigDecimal.ONE : BigDecimal.ONE.negate();
    Map<Measure, BigDecimal> m = new EnumMap<>(Measure.class);
    m.put(Measure.OPENING, f.opening().multiply(sign));
    m.put(Measure.DEBITS, f.debits());
    m.put(Measure.CREDITS, f.credits());
    BigDecimal movement = f.debits().subtract(f.credits()).multiply(sign);
    m.put(Measure.MOVEMENT, movement);
    BigDecimal closing = (basis == Basis.BALANCE ? f.closing() : f.yearToDate()).multiply(sign);
    m.put(Measure.CLOSING, closing);
    BigDecimal main = basis == Basis.BALANCE ? closing : movement;
    BigDecimal comparative = f.comparative().multiply(sign);
    m.put(Measure.COMPARATIVE, comparative);
    BigDecimal variance = main.subtract(comparative);
    m.put(Measure.VARIANCE, variance);
    m.put(
        Measure.VARIANCE_PCT,
        comparative.signum() == 0
            ? null
            : variance
                .multiply(HUNDRED)
                .divide(comparative.abs(), PERCENT_SCALE, RoundingMode.HALF_UP));
    return m;
  }

  /**
   * Ages a balance first-in first-out: what is still open is made of the most recent increases, so
   * the balance is spread over them from the newest backwards; a balance older than every increase
   * seen goes to the oldest bucket, a zero or negative balance to the first one.
   *
   * @param balance balance on the schedule's side
   * @param newestFirst increases of the row, newest first
   * @param asOf as-of date
   * @param slots ageing buckets
   * @return amount per bucket
   */
  public static List<BigDecimal> fifo(
      BigDecimal balance, List<Increase> newestFirst, LocalDate asOf, AgeingSlots slots) {
    List<BigDecimal> buckets = new ArrayList<>(Collections.nCopies(slots.size(), BigDecimal.ZERO));
    if (balance.signum() <= 0) {
      buckets.set(0, balance);
      return buckets;
    }
    BigDecimal remaining = balance;
    for (Increase inc : newestFirst) {
      if (remaining.signum() <= 0) {
        break;
      }
      BigDecimal taken = remaining.min(inc.amount());
      int index = slots.index(ChronoUnit.DAYS.between(inc.date(), asOf));
      buckets.set(index, buckets.get(index).add(taken));
      remaining = remaining.subtract(taken);
    }
    if (remaining.signum() > 0) {
      int last = slots.size() - 1;
      buckets.set(last, buckets.get(last).add(remaining));
    }
    return buckets;
  }
}
