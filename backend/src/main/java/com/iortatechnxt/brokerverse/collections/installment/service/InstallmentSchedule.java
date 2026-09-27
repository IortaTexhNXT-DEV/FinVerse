package com.iortatechnxt.brokerverse.collections.installment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure calculations of the installment plans (BRCLXN.053/054/058): billing cycles of a period,
 * equal installments with the rounding difference on the last one, and the allocation of what was
 * settled to the installments, oldest due first.
 */
public final class InstallmentSchedule {

  private static final int SCALE = 2;

  private InstallmentSchedule() {}

  /**
   * The billing cycles of a coverage period: one every {@code months} months from the start, the
   * last one ending the day before the end of the period.
   *
   * @param from first day of the period
   * @param to end of the period (exclusive, e.g. the expiry date)
   * @param months cycle length
   * @return cycles, at least one
   */
  public static List<Cycle> cycles(LocalDate from, LocalDate to, int months) {
    List<Cycle> cycles = new ArrayList<>();
    LocalDate start = from;
    int k = 1;
    while (cycles.isEmpty() || start.isBefore(to)) {
      LocalDate next = from.plusMonths((long) months * k);
      LocalDate end = next.isBefore(to) ? next : to;
      cycles.add(new Cycle(start, end.isAfter(start) ? end.minusDays(1) : start));
      start = next;
      k++;
    }
    return cycles;
  }

  /**
   * A number of consecutive cycles from a first due date.
   *
   * @param firstDue first due date
   * @param count number of cycles
   * @param months cycle length
   * @return cycles
   */
  public static List<Cycle> consecutive(LocalDate firstDue, int count, int months) {
    List<Cycle> cycles = new ArrayList<>();
    for (int k = 0; k < count; k++) {
      LocalDate start = firstDue.plusMonths((long) months * k);
      LocalDate next = firstDue.plusMonths((long) months * (k + 1));
      cycles.add(new Cycle(start, next.minusDays(1)));
    }
    return cycles;
  }

  /**
   * Splits an amount in equal installments; the rounding difference goes on the last one.
   *
   * @param total amount
   * @param count installments (at least one)
   * @return amounts at scale 2 summing to the total
   */
  public static List<BigDecimal> split(BigDecimal total, int count) {
    BigDecimal whole = total.setScale(SCALE, RoundingMode.HALF_UP);
    BigDecimal each = whole.divide(BigDecimal.valueOf(count), SCALE, RoundingMode.DOWN);
    List<BigDecimal> amounts = new ArrayList<>();
    for (int k = 1; k < count; k++) {
      amounts.add(each);
    }
    amounts.add(whole.subtract(each.multiply(BigDecimal.valueOf(count - 1L))));
    return amounts;
  }

  /**
   * Allocates a settled amount to installments in order (oldest due first): each takes up to its
   * amount, the rest goes to the next.
   *
   * @param amounts installment amounts in due-date order
   * @param settled amount settled on their invoice
   * @return amount allocated to each installment
   */
  public static List<BigDecimal> allocate(List<BigDecimal> amounts, BigDecimal settled) {
    List<BigDecimal> allocated = new ArrayList<>();
    BigDecimal rest = settled.max(BigDecimal.ZERO);
    for (BigDecimal amount : amounts) {
      BigDecimal take = rest.min(amount);
      allocated.add(take.setScale(SCALE, RoundingMode.HALF_UP));
      rest = rest.subtract(take);
    }
    return allocated;
  }

  /**
   * One billing cycle.
   *
   * @param from first day (the due date of its installment)
   * @param to last day
   */
  public record Cycle(LocalDate from, LocalDate to) {}
}
