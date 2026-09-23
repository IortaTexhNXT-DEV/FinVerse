package com.iortatechnxt.finverse.closing.service;

import com.iortatechnxt.finverse.currency.domain.ExchangeRate;
import com.iortatechnxt.finverse.currency.domain.ExchangeRateRepository;
import com.iortatechnxt.finverse.currency.domain.RateType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * CLOSING (month-end) rate lookup that reports a missing rate as an empty result instead of an
 * error, so checklists and previews can show it; posting refuses to run without it.
 */
@Component
public class ClosingRates {

  private final ExchangeRateRepository rates;

  /**
   * Creates the lookup.
   *
   * @param rates rate repository
   */
  public ClosingRates(ExchangeRateRepository rates) {
    this.rates = rates;
  }

  /**
   * Closing rate in force on a date (latest on or before it).
   *
   * @param baseCurrency company base currency
   * @param currency foreign currency
   * @param date date
   * @return rate (1 for the base currency), empty when no rate is maintained
   */
  public Optional<BigDecimal> closing(String baseCurrency, String currency, LocalDate date) {
    if (baseCurrency.equals(currency)) {
      return Optional.of(BigDecimal.ONE);
    }
    return rates
        .findFirstByCurrencyCodeAndRateTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            currency, RateType.CLOSING, date)
        .map(ExchangeRate::getRate);
  }
}
