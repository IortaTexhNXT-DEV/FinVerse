package com.iortatechnxt.brokerverse.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/**
 * The rate scheme of a package version (BRPM.007): what new business is priced on while the version
 * is current. On release it is projected on the product's commercial columns so that existing
 * readers keep working (PRODUCT_MAINTENANCE_DESIGN section 4.2).
 *
 * @param defaultRate premium rate in percent, null when every insurer has its own rate
 * @param minimumPremium minimum premium
 * @param defaultCommissionRate commission rate in percent
 * @param maxSumInsured package TSI limit (TSU routing), null for none
 * @param ratingBasisNote computation basis agreed with the insurers (PQ12)
 * @param manualRateAllowed whether an item rate other than the scheme rate is accepted without an
 *     approved exception (PQ10; true only for the versions backfilled at go-live)
 */
@Embeddable
public record SchemeTerms(
    @Column(name = "default_rate", precision = 19, scale = 8) BigDecimal defaultRate,
    @Column(name = "minimum_premium", nullable = false, precision = 19, scale = 2)
        BigDecimal minimumPremium,
    @Column(name = "default_commission_rate", nullable = false, precision = 19, scale = 8)
        BigDecimal defaultCommissionRate,
    @Column(name = "max_sum_insured", precision = 19, scale = 2) BigDecimal maxSumInsured,
    @Column(name = "rating_basis_note", length = 1000) String ratingBasisNote,
    @Column(name = "manual_rate_allowed", nullable = false) boolean manualRateAllowed) {

  /** Missing amounts are zero. */
  public SchemeTerms {
    minimumPremium = minimumPremium == null ? BigDecimal.ZERO : minimumPremium;
    defaultCommissionRate = defaultCommissionRate == null ? BigDecimal.ZERO : defaultCommissionRate;
  }
}
