package com.iortatechnxt.finverse.underwriting.api.dto;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.underwriting.domain.QuotationIteration;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Quotation iteration view with our-share figures.
 *
 * @param iterationNo iteration number
 * @param sumInsured sum insured 100 %
 * @param grossPremium gross 100 %
 * @param discount discount 100 %
 * @param loading loading 100 %
 * @param charges charges
 * @param netPremium net 100 %
 * @param ourNetPremium our net premium
 * @param brokerage commission on our net premium
 * @param remarks remarks
 * @param createdBy maker
 * @param createdAt time
 */
public record IterationResponse(
    int iterationNo,
    BigDecimal sumInsured,
    BigDecimal grossPremium,
    BigDecimal discount,
    BigDecimal loading,
    BigDecimal charges,
    BigDecimal netPremium,
    BigDecimal ourNetPremium,
    BigDecimal brokerage,
    String remarks,
    String createdBy,
    Instant createdAt) {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  /**
   * Maps an iteration.
   *
   * @param it iteration
   * @param sharePct our share %
   * @param commissionRate commission %
   * @return response
   */
  public static IterationResponse from(
      QuotationIteration it, BigDecimal sharePct, BigDecimal commissionRate) {
    BigDecimal ourNet = pct(it.netPremium(), sharePct);
    return new IterationResponse(
        it.getIterationNo(),
        it.getSumInsured(),
        it.getGrossPremium(),
        it.getDiscount(),
        it.getLoading(),
        it.getCharges(),
        it.netPremium(),
        ourNet,
        pct(ourNet, commissionRate),
        it.getRemarks(),
        it.getCreatedBy(),
        it.getCreatedAt());
  }

  private static BigDecimal pct(BigDecimal amount, BigDecimal rate) {
    return Money.round(
        amount.multiply(rate).divide(HUNDRED, Money.RATE_SCALE, RoundingMode.HALF_EVEN));
  }
}
