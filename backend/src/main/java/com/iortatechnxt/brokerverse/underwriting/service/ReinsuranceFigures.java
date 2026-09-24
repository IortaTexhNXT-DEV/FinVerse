package com.iortatechnxt.brokerverse.underwriting.service;

import com.iortatechnxt.brokerverse.common.util.Money;
import java.math.BigDecimal;

/**
 * Reinsurance split of the company's net premium of one premium transaction, in the policy
 * currency.
 *
 * @param treatyPremium premium ceded to treaties (quota share + surplus)
 * @param facPremium premium ceded facultatively
 * @param netRetention premium retained (our net premium − treaty − FAC)
 */
public record ReinsuranceFigures(
    BigDecimal treatyPremium, BigDecimal facPremium, BigDecimal netRetention) {

  /**
   * Figures of a transaction with no cession: everything retained.
   *
   * @param ourNetPremium company net premium
   * @return figures with zero treaty and FAC premium
   */
  public static ReinsuranceFigures noCession(BigDecimal ourNetPremium) {
    return new ReinsuranceFigures(Money.zero(), Money.zero(), Money.round(ourNetPremium));
  }
}
