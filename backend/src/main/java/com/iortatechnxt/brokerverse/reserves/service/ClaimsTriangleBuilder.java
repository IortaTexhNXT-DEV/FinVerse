package com.iortatechnxt.brokerverse.reserves.service;

import com.iortatechnxt.brokerverse.insurance.ClaimMovement;
import com.iortatechnxt.brokerverse.insurance.ClaimMovementType;
import com.iortatechnxt.brokerverse.reserves.domain.DevelopmentPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Builds the paid and incurred development triangles of one line of business from posted claim
 * movements (base currency, company share, gross of reinsurance).
 *
 * <ul>
 *   <li>Accident period = period of the loss date; development age = period of the movement date −
 *       accident period.
 *   <li>Paid = payments − recoveries. Incurred = payments − recoveries + reserve changes (the
 *       outstanding reserve is the running sum of its signed changes, so incurred = paid +
 *       outstanding).
 *   <li>Only the last {@code accidentPeriods} accident periods up to the valuation date are kept;
 *       movements dated after the valuation date are ignored.
 * </ul>
 */
public final class ClaimsTriangleBuilder {

  private ClaimsTriangleBuilder() {}

  /**
   * First movement date needed for a triangle.
   *
   * @param period development period
   * @param valuationDate valuation date
   * @param accidentPeriods number of accident periods
   * @return first day of the oldest accident period
   */
  public static LocalDate firstDate(
      DevelopmentPeriod period, LocalDate valuationDate, int accidentPeriods) {
    return period.start(period.index(valuationDate) - accidentPeriods + 1);
  }

  /**
   * Builds both triangles.
   *
   * @param movements movements of one line of business
   * @param period development period
   * @param valuationDate valuation date
   * @param accidentPeriods number of accident periods
   * @return paid and incurred triangles
   */
  public static Triangles build(
      Collection<ClaimMovement> movements,
      DevelopmentPeriod period,
      LocalDate valuationDate,
      int accidentPeriods) {
    int current = period.index(valuationDate);
    int first = current - accidentPeriods + 1;
    BigDecimal[][] paid = new BigDecimal[accidentPeriods][accidentPeriods];
    BigDecimal[][] incurred = new BigDecimal[accidentPeriods][accidentPeriods];
    for (int i = 0; i < accidentPeriods; i++) {
      for (int k = 0; k < accidentPeriods; k++) {
        paid[i][k] = BigDecimal.ZERO;
        incurred[i][k] = BigDecimal.ZERO;
      }
    }
    for (ClaimMovement m : movements) {
      int accident = period.index(m.lossDate());
      int at = period.index(m.movementDate());
      if (accident < first || accident > current || m.movementDate().isAfter(valuationDate)) {
        continue;
      }
      int row = accident - first;
      int age = Math.max(0, at - accident);
      BigDecimal signed = signed(m);
      incurred[row][age] = incurred[row][age].add(signed);
      if (m.type() != ClaimMovementType.RESERVE_CHANGE) {
        paid[row][age] = paid[row][age].add(signed);
      }
    }
    List<String> labels = new ArrayList<>();
    for (int i = 0; i < accidentPeriods; i++) {
      labels.add(period.label(first + i));
    }
    return new Triangles(
        new Triangle(labels, cumulate(paid)), new Triangle(labels, cumulate(incurred)));
  }

  private static BigDecimal signed(ClaimMovement m) {
    BigDecimal amount = m.baseAmount() == null ? BigDecimal.ZERO : m.baseAmount();
    return m.type() == ClaimMovementType.RECOVERY ? amount.negate() : amount;
  }

  /** Cumulates each row over the ages observed so far (row i observes n − i ages). */
  private static List<List<BigDecimal>> cumulate(BigDecimal[]... incremental) {
    int n = incremental.length;
    List<List<BigDecimal>> rows = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      List<BigDecimal> row = new ArrayList<>();
      BigDecimal running = BigDecimal.ZERO;
      for (int k = 0; k < n - i; k++) {
        running = running.add(incremental[i][k]);
        row.add(running);
      }
      rows.add(row);
    }
    return rows;
  }

  /**
   * Paid and incurred triangles of the same accident periods.
   *
   * @param paid paid triangle
   * @param incurred incurred triangle
   */
  public record Triangles(Triangle paid, Triangle incurred) {}
}
