package com.iortatechnxt.finverse.underwriting.service;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import com.iortatechnxt.finverse.underwriting.domain.PolicyRisk;
import com.iortatechnxt.finverse.underwriting.domain.PremiumBreakdown;
import com.iortatechnxt.finverse.underwriting.domain.PremiumInput;
import com.iortatechnxt.finverse.underwriting.domain.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * Applies {@link PremiumBreakdown#calculate} to policies and endorsements: taxes and policy fee
 * from the product, commission from the policy (override, else the intermediary's rate, else the
 * product default) and withholding tax from the intermediary.
 */
@Component
public class PolicyPremiumCalculator {

  private static final BigDecimal MAX_RATE = BigDecimal.valueOf(100);

  /**
   * Commission % applicable to a policy: the rate entered on the policy (validated to 0–100 %),
   * else the intermediary's rate, else the product's default rate. Preview, save and update all use
   * this rule, so a saved draft carries exactly the commission its preview showed.
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
      if (override.signum() < 0 || override.compareTo(MAX_RATE) > 0) {
        throw new BusinessRuleException(
            "INVALID_COMMISSION_RATE", "Commission must be between 0 and 100 %, not " + override);
      }
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
            sum(policy.getRisks(), PolicyRisk::getSumInsured),
            sum(policy.getRisks(), PolicyRisk::getPremium),
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

  private static BigDecimal sum(List<PolicyRisk> risks, Function<PolicyRisk, BigDecimal> amount) {
    return risks.stream().map(amount).reduce(Money.zero(), BigDecimal::add);
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
