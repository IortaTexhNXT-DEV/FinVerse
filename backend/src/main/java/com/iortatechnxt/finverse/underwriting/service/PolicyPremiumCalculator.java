package com.iortatechnxt.finverse.underwriting.service;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import com.iortatechnxt.finverse.underwriting.domain.PremiumBreakdown;
import com.iortatechnxt.finverse.underwriting.domain.PremiumInput;
import com.iortatechnxt.finverse.underwriting.domain.Product;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Applies {@link PremiumBreakdown#calculate} to policies and endorsements: taxes and policy fee
 * from the product, commission from the policy (override, else the intermediary's rate, else the
 * product default) and withholding tax from the intermediary.
 */
@Component
public class PolicyPremiumCalculator {

  /**
   * Commission % applicable to a policy.
   *
   * @param product product
   * @param intermediary intermediary, null for direct business
   * @param override rate entered on the policy, may be null
   * @return commission %
   */
  public BigDecimal commissionRate(Product product, Party intermediary, BigDecimal override) {
    if (intermediary == null) {
      return BigDecimal.ZERO;
    }
    if (override != null) {
      return override;
    }
    return intermediary.getCommissionRate() != null
        ? intermediary.getCommissionRate()
        : product.getDefaultCommissionRate();
  }

  /**
   * Premium of a policy's original issue from its risks.
   *
   * @param policy policy with risks
   * @param commissionRate commission %
   * @return breakdown
   */
  public PremiumBreakdown forPolicy(Policy policy, BigDecimal commissionRate) {
    Party intermediary = policy.getIntermediary();
    return PremiumBreakdown.calculate(
        new PremiumInput(
            policy.totalSumInsured(),
            policy.totalRiskPremium(),
            policy.getDiscountRate(),
            policy.getLoadingRate(),
            policy.getSharePct(),
            policy.isCoinsuranceLeader(),
            policy.getProduct().taxRates(),
            policy.getProduct().getPolicyFee(),
            intermediary == null ? BigDecimal.ZERO : commissionRate,
            intermediary == null
                ? BigDecimal.ZERO
                : Money.nz(intermediary.getWithholdingTaxRate())));
  }

  /**
   * Premium of an endorsement: the policy's terms applied to a change in gross premium.
   *
   * @param policy endorsed policy
   * @param grossChange gross premium change at 100 % (negative for return premium)
   * @param sumInsuredChange sum insured change at 100 %
   * @param policyFee policy fee to charge (renewals only)
   * @return breakdown
   */
  public PremiumBreakdown forEndorsement(
      Policy policy, BigDecimal grossChange, BigDecimal sumInsuredChange, BigDecimal policyFee) {
    PremiumBreakdown base = policy.getPremium();
    return PremiumBreakdown.calculate(
        new PremiumInput(
            sumInsuredChange,
            grossChange,
            policy.getDiscountRate(),
            policy.getLoadingRate(),
            policy.getSharePct(),
            policy.isCoinsuranceLeader(),
            policy.getProduct().taxRates(),
            policyFee,
            base.getCommissionRate(),
            base.getWithholdingRate()));
  }
}
