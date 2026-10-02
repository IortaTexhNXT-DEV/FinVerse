package com.iortatechnxt.brokerverse.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/**
 * Premium breakdown of an account as computed by the Appendix A calculator (per policy year).
 *
 * @param ratingBasis period basis (ANNUAL, PRO_RATA, SHORT_PERIOD)
 * @param netPremium net premium (motor: basic premium)
 * @param dst documentary stamp tax
 * @param premiumTax premium tax
 * @param vat VAT on premium
 * @param fst fire service tax
 * @param lgt local government tax
 * @param totalCharges total taxes
 * @param grossPremium gross (total) premium
 * @param commissionRate commission rate in percent
 * @param commission commission
 * @param vatOnCommission VAT on commission
 * @param minimumApplied whether the minimum premium applied
 */
@Embeddable
public record AccountPremium(
    @Column(name = "rating_basis", length = 20) String ratingBasis,
    @Column(name = "net_premium", precision = 19, scale = 2) BigDecimal netPremium,
    @Column(name = "dst", precision = 19, scale = 2) BigDecimal dst,
    @Column(name = "premium_tax", precision = 19, scale = 2) BigDecimal premiumTax,
    @Column(name = "vat", precision = 19, scale = 2) BigDecimal vat,
    @Column(name = "fst", precision = 19, scale = 2) BigDecimal fst,
    @Column(name = "lgt", precision = 19, scale = 2) BigDecimal lgt,
    @Column(name = "total_charges", precision = 19, scale = 2) BigDecimal totalCharges,
    @Column(name = "gross_premium", precision = 19, scale = 2) BigDecimal grossPremium,
    @Column(name = "commission_rate", precision = 19, scale = 8) BigDecimal commissionRate,
    @Column(name = "commission", precision = 19, scale = 2) BigDecimal commission,
    @Column(name = "vat_on_commission", precision = 19, scale = 2) BigDecimal vatOnCommission,
    @Column(name = "minimum_applied", nullable = false) boolean minimumApplied) {

  /** Not rated yet. */
  public static final AccountPremium NONE =
      new AccountPremium(
          null, null, null, null, null, null, null, null, null, null, null, null, false);

  /**
   * Whether a premium has been computed.
   *
   * @return true when rated
   */
  public boolean isRated() {
    return grossPremium != null;
  }
}
