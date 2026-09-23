package com.iortatechnxt.finverse.underwriting.api.dto;

import com.iortatechnxt.finverse.underwriting.domain.PremiumBreakdown;
import java.math.BigDecimal;

/**
 * Premium figures of a policy, endorsement or preview.
 *
 * @param sumInsured sum insured 100 %
 * @param ourSumInsured our share of the sum insured
 * @param grossPremium gross premium 100 %
 * @param discountAmount discount 100 %
 * @param loadingAmount loading 100 %
 * @param netPremium net premium 100 %
 * @param ourGrossPremium our gross premium
 * @param ourDiscount our discount
 * @param ourLoading our loading
 * @param ourNetPremium our net premium
 * @param coinsurerPremium coinsurers' share billed by the company as leader
 * @param billedPremium premium billed to the client
 * @param dst documentary stamp tax
 * @param vat value added tax
 * @param lgt local government tax
 * @param fst fire service tax
 * @param premiumTax premium tax
 * @param policyFee policy fee
 * @param taxesAndCharges total taxes and charges
 * @param totalDue amount due from the client
 * @param commissionRate commission %
 * @param commission commission
 * @param withholdingRate withholding %
 * @param withholdingTax withholding tax on commission
 * @param netCommission commission payable
 */
public record PremiumResponse(
    BigDecimal sumInsured,
    BigDecimal ourSumInsured,
    BigDecimal grossPremium,
    BigDecimal discountAmount,
    BigDecimal loadingAmount,
    BigDecimal netPremium,
    BigDecimal ourGrossPremium,
    BigDecimal ourDiscount,
    BigDecimal ourLoading,
    BigDecimal ourNetPremium,
    BigDecimal coinsurerPremium,
    BigDecimal billedPremium,
    BigDecimal dst,
    BigDecimal vat,
    BigDecimal lgt,
    BigDecimal fst,
    BigDecimal premiumTax,
    BigDecimal policyFee,
    BigDecimal taxesAndCharges,
    BigDecimal totalDue,
    BigDecimal commissionRate,
    BigDecimal commission,
    BigDecimal withholdingRate,
    BigDecimal withholdingTax,
    BigDecimal netCommission) {

  /**
   * Maps a breakdown.
   *
   * @param b breakdown
   * @return response
   */
  public static PremiumResponse from(PremiumBreakdown b) {
    return new PremiumResponse(
        b.getSumInsured(),
        b.getOurSumInsured(),
        b.getGrossPremium(),
        b.getDiscountAmount(),
        b.getLoadingAmount(),
        b.getNetPremium(),
        b.getOurGrossPremium(),
        b.getOurDiscount(),
        b.getOurLoading(),
        b.getOurNetPremium(),
        b.getCoinsurerPremium(),
        b.getBilledPremium(),
        b.getDst(),
        b.getVat(),
        b.getLgt(),
        b.getFst(),
        b.getPremiumTax(),
        b.getPolicyFee(),
        b.taxesAndCharges(),
        b.getTotalDue(),
        b.getCommissionRate(),
        b.getCommission(),
        b.getWithholdingRate(),
        b.getWithholdingTax(),
        b.getNetCommission());
  }
}
