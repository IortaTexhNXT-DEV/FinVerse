package com.iortatechnxt.finverse.reserves.api.dto;

import com.iortatechnxt.finverse.reserves.service.TriangleAnalysis;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Chain-ladder development triangles of one line of business.
 *
 * @param businessLine line of business
 * @param basis projected triangle (PAID / INCURRED)
 * @param period development period (YEAR / QUARTER)
 * @param paid cumulative paid triangle rows
 * @param incurred cumulative incurred triangle rows
 * @param factors age-to-age factors of the projected triangle
 * @param rows per accident period: latest, cumulative factor, ultimate, incurred, IBNR
 * @param ibnr IBNR of the line (not negative)
 */
public record TriangleResponse(
    String businessLine,
    String basis,
    String period,
    List<List<BigDecimal>> paid,
    List<List<BigDecimal>> incurred,
    List<BigDecimal> factors,
    List<AccidentRow> rows,
    BigDecimal ibnr) {

  /**
   * Maps an analysis.
   *
   * @param a analysis
   * @return response
   */
  public static TriangleResponse from(TriangleAnalysis a) {
    List<AccidentRow> rows = new ArrayList<>();
    List<String> labels = a.paid().accidentLabels();
    boolean paidBasis = "PAID".equals(a.basis().name());
    for (int i = 0; i < labels.size(); i++) {
      rows.add(
          new AccidentRow(
              labels.get(i),
              paidBasis ? a.paid().latest(i) : a.incurred().latest(i),
              a.projection().cumulativeFactors().get(i),
              a.projection().ultimates().get(i),
              a.incurredToDate().get(i),
              a.ibnrByAccident().get(i)));
    }
    return new TriangleResponse(
        a.businessLine(),
        a.basis().name(),
        a.period().name(),
        a.paid().rows(),
        a.incurred().rows(),
        a.projection().factors(),
        rows,
        a.ibnr());
  }

  /**
   * Projection of one accident period.
   *
   * @param accidentPeriod accident period label
   * @param latest latest cumulative amount of the projected triangle
   * @param cumulativeFactor factor to ultimate
   * @param ultimate projected ultimate
   * @param incurred incurred to date (paid + outstanding)
   * @param ibnr ultimate − incurred
   */
  public record AccidentRow(
      String accidentPeriod,
      BigDecimal latest,
      BigDecimal cumulativeFactor,
      BigDecimal ultimate,
      BigDecimal incurred,
      BigDecimal ibnr) {}
}
