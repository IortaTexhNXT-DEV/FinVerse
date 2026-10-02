package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.catalog.domain.RatingMethod;
import java.math.BigDecimal;
import java.util.List;

/**
 * Full Appendix A premium breakdown. Amounts have scale 2; {@code netPremium} is the net premium of
 * fire and other lines or the basic premium of motor, after the period adjustment and the minimum
 * premium.
 *
 * @param method formula used
 * @param sumInsured total sum insured
 * @param odTheftCoverage motor OD/Theft coverage (TSI x factor), zero for other lines
 * @param odTheftPremium motor OD/Theft premium, zero for other lines
 * @param biPremium motor excess BI premium
 * @param pdPremium motor PD premium
 * @param annualPremium annual net / basic premium before period and minimum
 * @param periodFactor period factor applied (1 for annual)
 * @param minimumApplied whether the minimum premium replaced the computed premium
 * @param netPremium net (basic) premium
 * @param dst documentary stamp tax (half-peso rounding)
 * @param premiumTax premium tax
 * @param vat VAT on premium
 * @param fst fire service tax
 * @param lgt local government tax
 * @param totalCharges sum of the taxes
 * @param grossPremium net premium plus charges (motor total premium)
 * @param commission broker commission on the net premium
 * @param vatOnCommission VAT on the commission
 * @param items annual premium per item
 */
public record PremiumBreakdown(
    RatingMethod method,
    BigDecimal sumInsured,
    BigDecimal odTheftCoverage,
    BigDecimal odTheftPremium,
    BigDecimal biPremium,
    BigDecimal pdPremium,
    BigDecimal annualPremium,
    BigDecimal periodFactor,
    boolean minimumApplied,
    BigDecimal netPremium,
    BigDecimal dst,
    BigDecimal premiumTax,
    BigDecimal vat,
    BigDecimal fst,
    BigDecimal lgt,
    BigDecimal totalCharges,
    BigDecimal grossPremium,
    BigDecimal commission,
    BigDecimal vatOnCommission,
    List<ItemPremium> items) {

  /** Defensive copy. */
  public PremiumBreakdown {
    items = items == null ? List.of() : List.copyOf(items);
  }

  /**
   * Premium of one item.
   *
   * @param label item label
   * @param sumInsured sum insured
   * @param ratePercent rate in percent
   * @param premium annual premium of the item (motor: OD/Theft + BI + PD)
   */
  public record ItemPremium(
      String label, BigDecimal sumInsured, BigDecimal ratePercent, BigDecimal premium) {}
}
