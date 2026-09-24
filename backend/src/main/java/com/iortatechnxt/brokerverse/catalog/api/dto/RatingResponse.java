package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import com.iortatechnxt.brokerverse.catalog.service.PremiumBreakdown;
import com.iortatechnxt.brokerverse.catalog.service.PremiumRequest.RatingRates;
import com.iortatechnxt.brokerverse.catalog.service.RatingService.Rating;
import java.math.BigDecimal;

/**
 * Premium calculator result: the Appendix A breakdown and the rates it used.
 *
 * @param breakdown breakdown
 * @param rates rates used, in percent
 * @param basis period basis applied
 * @param shortPeriodPercent short-period percentage applied
 */
public record RatingResponse(
    PremiumBreakdown breakdown,
    RatingRates rates,
    PeriodBasis basis,
    BigDecimal shortPeriodPercent) {

  /**
   * Maps a rating.
   *
   * @param r rating
   * @return response
   */
  public static RatingResponse from(Rating r) {
    return new RatingResponse(r.breakdown(), r.rates(), r.basis(), r.shortPeriodPercent());
  }
}
