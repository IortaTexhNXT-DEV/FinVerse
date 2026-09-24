package com.iortatechnxt.brokerverse.reserves.demo;

import com.iortatechnxt.brokerverse.reserves.domain.DevelopmentPeriod;
import com.iortatechnxt.brokerverse.reserves.domain.IbnrMethod;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveParameterTerms;
import com.iortatechnxt.brokerverse.reserves.domain.TriangleBasis;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Reserve parameters of the demo lines of business: a mix of the rate and chain-ladder IBNR
 * methods, margins, expected loss ratios (HEALTH above break-even to show a premium deficiency) and
 * reinsurance commission rates.
 */
final class DemoReserveParameters {

  private static final String REMARKS = "Demo actuarial basis 2026";
  private static final int ACCIDENT_YEARS = 5;
  private static final int ACCIDENT_QUARTERS = 8;

  /** Parameters by line of business code (the codes of the underwriting demo products). */
  static final Map<String, ReserveParameterTerms> BY_LINE =
      Map.of(
          "FIRE", chainLadder(TriangleBasis.INCURRED, DevelopmentPeriod.YEAR, "7.5", "4", "55"),
          "MOTOR", chainLadder(TriangleBasis.PAID, DevelopmentPeriod.QUARTER, "10", "5", "68"),
          "MARINE", rate("5", "7.5", "3", "60"),
          "ENGG", rate("4", "7.5", "3", "50"),
          "CASUALTY",
              chainLadder(TriangleBasis.INCURRED, DevelopmentPeriod.QUARTER, "10", "5", "65"),
          "PA", rate("3", "5", "2.5", "45"),
          "HEALTH", rate("6", "5", "4", "95"),
          "BONDS", rate("2", "5", "2", "40"));

  private DemoReserveParameters() {}

  private static ReserveParameterTerms rate(
      String ibnrRate, String mfad, String ulae, String lossRatio) {
    return terms(
        IbnrMethod.RATE,
        ibnrRate,
        TriangleBasis.INCURRED,
        DevelopmentPeriod.YEAR,
        mfad,
        ulae,
        lossRatio);
  }

  private static ReserveParameterTerms chainLadder(
      TriangleBasis basis, DevelopmentPeriod period, String mfad, String ulae, String lossRatio) {
    return terms(IbnrMethod.CHAIN_LADDER, "0", basis, period, mfad, ulae, lossRatio);
  }

  private static ReserveParameterTerms terms(
      IbnrMethod method,
      String ibnrRate,
      TriangleBasis basis,
      DevelopmentPeriod period,
      String mfad,
      String ulae,
      String lossRatio) {
    return new ReserveParameterTerms(
        method,
        new BigDecimal(ibnrRate),
        basis,
        period,
        period == DevelopmentPeriod.YEAR ? ACCIDENT_YEARS : ACCIDENT_QUARTERS,
        new BigDecimal(mfad),
        new BigDecimal(ulae),
        new BigDecimal(lossRatio),
        new BigDecimal("27.5"),
        new BigDecimal("22.5"),
        REMARKS);
  }
}
