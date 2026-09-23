package com.iortatechnxt.finverse.underwriting.domain;

import com.iortatechnxt.finverse.common.util.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Premium figures of one financial document (policy issue or endorsement), computed by {@link
 * #calculate(PremiumInput)}:
 *
 * <ol>
 *   <li>gross (100 %), discount and loading from their rates; net = gross + loading − discount;
 *   <li>our share of every amount = amount × share %;
 *   <li>billed premium = our net, plus the coinsurers' share when the company leads the coinsurance
 *       and collects 100 % from the client;
 *   <li>taxes on our net premium (DST, VAT, LGT, FST, premium tax) and the policy fee;
 *   <li>total due from the client = billed premium + taxes + policy fee;
 *   <li>commission = our net × commission %, withholding tax on it, net commission payable.
 * </ol>
 *
 * Every amount is rounded to 2 decimals; signs follow the gross premium (negative for refunds).
 */
@Embeddable
public class PremiumBreakdown {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final int RATE_SCALE = 8;

  @Column(name = "sum_insured", nullable = false, precision = 19, scale = 2)
  private BigDecimal sumInsured = Money.zero();

  @Column(name = "our_sum_insured", nullable = false, precision = 19, scale = 2)
  private BigDecimal ourSumInsured = Money.zero();

  @Column(name = "gross_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal grossPremium = Money.zero();

  @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal discountAmount = Money.zero();

  @Column(name = "loading_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal loadingAmount = Money.zero();

  @Column(name = "net_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal netPremium = Money.zero();

  @Column(name = "our_gross_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal ourGrossPremium = Money.zero();

  @Column(name = "our_discount", nullable = false, precision = 19, scale = 2)
  private BigDecimal ourDiscount = Money.zero();

  @Column(name = "our_loading", nullable = false, precision = 19, scale = 2)
  private BigDecimal ourLoading = Money.zero();

  @Column(name = "our_net_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal ourNetPremium = Money.zero();

  @Column(name = "coinsurer_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal coinsurerPremium = Money.zero();

  @Column(name = "billed_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal billedPremium = Money.zero();

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal dst = Money.zero();

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal vat = Money.zero();

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal lgt = Money.zero();

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal fst = Money.zero();

  @Column(name = "premium_tax", nullable = false, precision = 19, scale = 2)
  private BigDecimal premiumTax = Money.zero();

  @Column(name = "policy_fee", nullable = false, precision = 19, scale = 2)
  private BigDecimal policyFee = Money.zero();

  @Column(name = "total_due", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalDue = Money.zero();

  @Column(name = "commission_rate", nullable = false, precision = 19, scale = 8)
  private BigDecimal commissionRate = BigDecimal.ZERO;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal commission = Money.zero();

  @Column(name = "withholding_rate", nullable = false, precision = 19, scale = 8)
  private BigDecimal withholdingRate = BigDecimal.ZERO;

  @Column(name = "withholding_tax", nullable = false, precision = 19, scale = 2)
  private BigDecimal withholdingTax = Money.zero();

  @Column(name = "net_commission", nullable = false, precision = 19, scale = 2)
  private BigDecimal netCommission = Money.zero();

  /** Creates an all-zero breakdown (non-financial documents, JPA). */
  public PremiumBreakdown() {
    // all amounts start at zero
  }

  /**
   * Computes the premium figures.
   *
   * @param in inputs
   * @return breakdown
   */
  public static PremiumBreakdown calculate(PremiumInput in) {
    PremiumBreakdown b = new PremiumBreakdown();
    BigDecimal share = Money.nz(in.sharePct());
    b.sumInsured = Money.round(in.sumInsured());
    b.ourSumInsured = pct(b.sumInsured, share);
    b.grossPremium = Money.round(in.grossPremium());
    b.discountAmount = pct(b.grossPremium, in.discountRate());
    b.loadingAmount = pct(b.grossPremium, in.loadingRate());
    b.netPremium = b.grossPremium.add(b.loadingAmount).subtract(b.discountAmount);
    b.ourGrossPremium = pct(b.grossPremium, share);
    b.ourDiscount = pct(b.discountAmount, share);
    b.ourLoading = pct(b.loadingAmount, share);
    b.ourNetPremium = b.ourGrossPremium.add(b.ourLoading).subtract(b.ourDiscount);
    b.coinsurerPremium =
        in.coinsuranceLeader() ? b.netPremium.subtract(b.ourNetPremium) : Money.zero();
    b.billedPremium = b.ourNetPremium.add(b.coinsurerPremium);
    TaxRates taxes = in.taxes() == null ? TaxRates.none() : in.taxes();
    b.dst = pct(b.ourNetPremium, taxes.dst());
    b.vat = pct(b.ourNetPremium, taxes.vat());
    b.lgt = pct(b.ourNetPremium, taxes.lgt());
    b.fst = pct(b.ourNetPremium, taxes.fst());
    b.premiumTax = pct(b.ourNetPremium, taxes.premiumTax());
    b.policyFee = Money.round(in.policyFee());
    b.totalDue = b.billedPremium.add(b.taxesAndCharges());
    b.commissionRate = rate(in.commissionRate());
    b.commission = pct(b.ourNetPremium, b.commissionRate);
    b.withholdingRate = rate(in.withholdingRate());
    b.withholdingTax = pct(b.commission, b.withholdingRate);
    b.netCommission = b.commission.subtract(b.withholdingTax);
    return b;
  }

  /**
   * Taxes and charges added to the premium on the debit note.
   *
   * @return DST + VAT + LGT + FST + premium tax + policy fee
   */
  public BigDecimal taxesAndCharges() {
    return dst.add(vat).add(lgt).add(fst).add(premiumTax).add(policyFee);
  }

  /**
   * Whether the document moves money (non-zero amount due or commission).
   *
   * @return true when financial
   */
  public boolean isFinancial() {
    return totalDue.signum() != 0 || commission.signum() != 0;
  }

  private static BigDecimal pct(BigDecimal amount, BigDecimal ratePct) {
    return Money.round(
        amount.multiply(Money.nz(ratePct)).divide(HUNDRED, RATE_SCALE, RoundingMode.HALF_EVEN));
  }

  private static BigDecimal rate(BigDecimal r) {
    return Money.nz(r).setScale(RATE_SCALE, RoundingMode.HALF_EVEN);
  }

  public BigDecimal getSumInsured() {
    return sumInsured;
  }

  public BigDecimal getOurSumInsured() {
    return ourSumInsured;
  }

  public BigDecimal getGrossPremium() {
    return grossPremium;
  }

  public BigDecimal getDiscountAmount() {
    return discountAmount;
  }

  public BigDecimal getLoadingAmount() {
    return loadingAmount;
  }

  public BigDecimal getNetPremium() {
    return netPremium;
  }

  public BigDecimal getOurGrossPremium() {
    return ourGrossPremium;
  }

  public BigDecimal getOurDiscount() {
    return ourDiscount;
  }

  public BigDecimal getOurLoading() {
    return ourLoading;
  }

  public BigDecimal getOurNetPremium() {
    return ourNetPremium;
  }

  public BigDecimal getCoinsurerPremium() {
    return coinsurerPremium;
  }

  public BigDecimal getBilledPremium() {
    return billedPremium;
  }

  public BigDecimal getDst() {
    return dst;
  }

  public BigDecimal getVat() {
    return vat;
  }

  public BigDecimal getLgt() {
    return lgt;
  }

  public BigDecimal getFst() {
    return fst;
  }

  public BigDecimal getPremiumTax() {
    return premiumTax;
  }

  public BigDecimal getPolicyFee() {
    return policyFee;
  }

  public BigDecimal getTotalDue() {
    return totalDue;
  }

  public BigDecimal getCommissionRate() {
    return commissionRate;
  }

  public BigDecimal getCommission() {
    return commission;
  }

  public BigDecimal getWithholdingRate() {
    return withholdingRate;
  }

  public BigDecimal getWithholdingTax() {
    return withholdingTax;
  }

  public BigDecimal getNetCommission() {
    return netCommission;
  }
}
