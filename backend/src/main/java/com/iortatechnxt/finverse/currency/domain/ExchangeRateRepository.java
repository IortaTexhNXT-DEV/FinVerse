package com.iortatechnxt.finverse.currency.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link ExchangeRate}. */
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

  /**
   * Finds the rate in force on a date (latest effective date on or before it).
   *
   * @param currencyCode currency
   * @param rateType rate type
   * @param date date
   * @return rate if any
   */
  Optional<ExchangeRate>
      findFirstByCurrencyCodeAndRateTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
          String currencyCode, RateType rateType, LocalDate date);

  /**
   * Finds a rate for an exact date.
   *
   * @param currencyCode currency
   * @param rateType rate type
   * @param effectiveDate date
   * @return rate if any
   */
  Optional<ExchangeRate> findByCurrencyCodeAndRateTypeAndEffectiveDate(
      String currencyCode, RateType rateType, LocalDate effectiveDate);

  /**
   * Lists rates in a date range, newest first.
   *
   * @param from inclusive start
   * @param to inclusive end
   * @return rates
   */
  List<ExchangeRate> findByEffectiveDateBetweenOrderByEffectiveDateDescCurrencyCode(
      LocalDate from, LocalDate to);
}
