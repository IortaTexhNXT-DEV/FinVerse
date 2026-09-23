package com.iortatechnxt.finverse.consolidation.service;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.currency.domain.ExchangeRate;
import com.iortatechnxt.finverse.currency.domain.ExchangeRateRepository;
import com.iortatechnxt.finverse.currency.domain.RateType;
import com.iortatechnxt.finverse.currency.service.CurrencyService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Rates for translating a member's balances into the consolidation currency.
 *
 * <p>Balance sheet items use the CLOSING rate on the as-of date. Income statement items use the
 * AVERAGE rate in force on the as-of date when one was published within the member's fiscal year;
 * otherwise the arithmetic mean of the SPOT rates of the fiscal year to date. Rates are quoted as
 * consolidation-currency units per member-currency unit, as maintained in the rate table.
 */
@Component
public class TranslationRates {

  private final CurrencyService currencies;
  private final ExchangeRateRepository rates;

  /**
   * Creates the helper.
   *
   * @param currencies currency service
   * @param rates rate repository
   */
  public TranslationRates(CurrencyService currencies, ExchangeRateRepository rates) {
    this.currencies = currencies;
    this.rates = rates;
  }

  /**
   * Closing rate.
   *
   * @param groupCurrency consolidation currency
   * @param currency member base currency
   * @param asOf date
   * @return rate (fails with RATE_NOT_FOUND when missing)
   */
  public BigDecimal closing(String groupCurrency, String currency, LocalDate asOf) {
    return currencies.rateOn(groupCurrency, currency, RateType.CLOSING, asOf);
  }

  /**
   * Average rate for the fiscal year to date.
   *
   * @param groupCurrency consolidation currency
   * @param currency member base currency
   * @param yearStart first day of the member's fiscal year
   * @param asOf date
   * @return rate
   */
  public BigDecimal average(
      String groupCurrency, String currency, LocalDate yearStart, LocalDate asOf) {
    if (groupCurrency.equals(currency)) {
      return BigDecimal.ONE;
    }
    return rates
        .findFirstByCurrencyCodeAndRateTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            currency, RateType.AVERAGE, asOf)
        .filter(r -> !r.getEffectiveDate().isBefore(yearStart))
        .map(ExchangeRate::getRate)
        .orElseGet(() -> meanSpot(groupCurrency, currency, yearStart, asOf));
  }

  private BigDecimal meanSpot(
      String groupCurrency, String currency, LocalDate yearStart, LocalDate asOf) {
    List<BigDecimal> spot =
        currencies.listRates(yearStart, asOf).stream()
            .filter(r -> r.getCurrencyCode().equals(currency) && r.getRateType() == RateType.SPOT)
            .map(ExchangeRate::getRate)
            .toList();
    if (spot.isEmpty()) {
      return closing(groupCurrency, currency, asOf);
    }
    return spot.stream()
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .divide(BigDecimal.valueOf(spot.size()), Money.RATE_SCALE, RoundingMode.HALF_EVEN);
  }
}
