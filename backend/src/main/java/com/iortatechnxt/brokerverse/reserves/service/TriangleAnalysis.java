package com.iortatechnxt.brokerverse.reserves.service;

import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.insurance.ClaimMovement;
import com.iortatechnxt.brokerverse.reserves.domain.DevelopmentPeriod;
import com.iortatechnxt.brokerverse.reserves.domain.TriangleBasis;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Chain-ladder IBNR of one line of business: the paid and incurred triangles, the projection of the
 * selected one and IBNR = ultimate − incurred per accident period. The line's IBNR is the sum over
 * accident periods, floored at zero (a negative total would release reported reserves).
 *
 * @param businessLine line of business
 * @param basis triangle projected
 * @param period development period
 * @param paid paid triangle
 * @param incurred incurred triangle
 * @param projection chain-ladder projection of the selected triangle
 * @param incurredToDate latest incurred per accident period
 * @param ibnrByAccident ultimate − incurred per accident period
 * @param ibnr IBNR of the line (not negative)
 */
public record TriangleAnalysis(
    String businessLine,
    TriangleBasis basis,
    DevelopmentPeriod period,
    Triangle paid,
    Triangle incurred,
    ChainLadder.Projection projection,
    List<BigDecimal> incurredToDate,
    List<BigDecimal> ibnrByAccident,
    BigDecimal ibnr) {

  /** Canonical constructor copying the lists. */
  public TriangleAnalysis {
    incurredToDate = List.copyOf(incurredToDate);
    ibnrByAccident = List.copyOf(ibnrByAccident);
  }

  /**
   * Analyses the movements of one line of business.
   *
   * @param businessLine line of business
   * @param movements posted claim movements of the line
   * @param basis paid or incurred projection
   * @param period development period
   * @param accidentPeriods number of accident periods
   * @param valuationDate valuation date
   * @return analysis
   */
  public static TriangleAnalysis of(
      String businessLine,
      Collection<ClaimMovement> movements,
      TriangleBasis basis,
      DevelopmentPeriod period,
      int accidentPeriods,
      LocalDate valuationDate) {
    ClaimsTriangleBuilder.Triangles triangles =
        ClaimsTriangleBuilder.build(movements, period, valuationDate, accidentPeriods);
    Triangle selected = basis == TriangleBasis.PAID ? triangles.paid() : triangles.incurred();
    ChainLadder.Projection projection = ChainLadder.project(selected);
    List<BigDecimal> incurred = new ArrayList<>();
    List<BigDecimal> ibnr = new ArrayList<>();
    BigDecimal total = BigDecimal.ZERO;
    for (int i = 0; i < selected.rows().size(); i++) {
      BigDecimal reported = Money.round(triangles.incurred().latest(i));
      BigDecimal byAccident = projection.ultimates().get(i).subtract(reported);
      incurred.add(reported);
      ibnr.add(byAccident);
      total = total.add(byAccident);
    }
    return new TriangleAnalysis(
        businessLine,
        basis,
        period,
        triangles.paid(),
        triangles.incurred(),
        projection,
        incurred,
        ibnr,
        total.signum() < 0 ? Money.zero() : Money.round(total));
  }
}
