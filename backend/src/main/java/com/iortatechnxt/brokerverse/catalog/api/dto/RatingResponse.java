package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import com.iortatechnxt.brokerverse.catalog.service.PremiumBreakdown;
import com.iortatechnxt.brokerverse.catalog.service.PremiumRequest.RatingRates;
import com.iortatechnxt.brokerverse.catalog.service.RatingService.Rating;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Premium calculator result: the Appendix A breakdown and the rates it used.
 *
 * @param breakdown breakdown
 * @param rates rates used, in percent
 * @param basis period basis applied
 * @param shortPeriodPercent short-period percentage applied
 * @param schemeVersion package version that priced it, null for a product without versions
 * @param schemeRate scheme rate of that version
 * @param schemeEffectiveFrom effective date of that version
 * @param nonCurrent the version is not the one in force today (history, BRPM.007)
 * @param schemeDeviation an item rate differs from the scheme rate (needs an exception on
 *     submission)
 */
public record RatingResponse(
    PremiumBreakdown breakdown,
    RatingRates rates,
    PeriodBasis basis,
    BigDecimal shortPeriodPercent,
    Integer schemeVersion,
    BigDecimal schemeRate,
    LocalDate schemeEffectiveFrom,
    boolean nonCurrent,
    boolean schemeDeviation) {

  /**
   * Maps a rating.
   *
   * @param r rating
   * @param effectiveFrom effective date of the version used, null when none
   * @param currentVersion version in force today, null when none
   * @return response
   */
  public static RatingResponse from(Rating r, LocalDate effectiveFrom, Integer currentVersion) {
    return new RatingResponse(
        r.breakdown(),
        r.rates(),
        r.basis(),
        r.shortPeriodPercent(),
        r.schemeVersion(),
        r.schemeRate(),
        effectiveFrom,
        r.schemeVersion() != null && !r.schemeVersion().equals(currentVersion),
        r.schemeDeviation());
  }
}
