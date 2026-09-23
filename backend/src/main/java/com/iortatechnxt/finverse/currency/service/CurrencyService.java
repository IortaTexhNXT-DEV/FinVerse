package com.iortatechnxt.finverse.currency.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.currency.domain.Currency;
import com.iortatechnxt.finverse.currency.domain.CurrencyRepository;
import com.iortatechnxt.finverse.currency.domain.ExchangeRate;
import com.iortatechnxt.finverse.currency.domain.ExchangeRateRepository;
import com.iortatechnxt.finverse.currency.domain.RateType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Currency master, exchange rate maintenance and conversion to base currency. */
@Service
@Transactional
public class CurrencyService {

  private final CurrencyRepository currencies;
  private final ExchangeRateRepository rates;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param currencies currency repository
   * @param rates rate repository
   * @param audit audit trail
   */
  public CurrencyService(
      CurrencyRepository currencies, ExchangeRateRepository rates, AuditTrailService audit) {
    this.currencies = currencies;
    this.rates = rates;
    this.audit = audit;
  }

  /**
   * Lists currencies.
   *
   * @return currencies
   */
  @Transactional(readOnly = true)
  public List<Currency> listCurrencies() {
    return currencies.findAllByOrderByCode();
  }

  /**
   * Returns an active currency or fails.
   *
   * @param code ISO code
   * @return currency
   */
  @Transactional(readOnly = true)
  public Currency requireActive(String code) {
    Currency currency =
        currencies
            .findById(code)
            .orElseThrow(() -> new ResourceNotFoundException("Currency", code));
    if (!currency.isActive()) {
      throw new BusinessRuleException("INACTIVE_CURRENCY", "Currency " + code + " is not active");
    }
    return currency;
  }

  /**
   * Resolves the rate in force for a currency on a date. Base currency always has rate 1.
   *
   * @param baseCurrency company base currency
   * @param currency transaction currency
   * @param rateType rate type
   * @param date date
   * @return rate (base units per foreign unit)
   */
  @Transactional(readOnly = true)
  public BigDecimal rateOn(
      String baseCurrency, String currency, RateType rateType, LocalDate date) {
    if (baseCurrency.equals(currency)) {
      return BigDecimal.ONE;
    }
    return rates
        .findFirstByCurrencyCodeAndRateTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            currency, rateType, date)
        .map(ExchangeRate::getRate)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "RATE_NOT_FOUND",
                    "No " + rateType + " rate for " + currency + " on or before " + date));
  }

  /**
   * Converts an amount to base currency using the SPOT rate on a date.
   *
   * @param baseCurrency base currency
   * @param currency transaction currency
   * @param amount amount in transaction currency
   * @param date date
   * @return base currency amount
   */
  @Transactional(readOnly = true)
  public BigDecimal toBase(
      String baseCurrency, String currency, BigDecimal amount, LocalDate date) {
    return Money.convert(amount, rateOn(baseCurrency, currency, RateType.SPOT, date));
  }

  /**
   * Lists rates in a date range.
   *
   * @param from start
   * @param to end
   * @return rates
   */
  @Transactional(readOnly = true)
  public List<ExchangeRate> listRates(LocalDate from, LocalDate to) {
    return rates.findByEffectiveDateBetweenOrderByEffectiveDateDescCurrencyCode(from, to);
  }

  /**
   * Creates or replaces the rate of a currency/type for a date.
   *
   * @param currency currency
   * @param rateType rate type
   * @param date effective date
   * @param rate positive rate
   * @return saved rate
   */
  public ExchangeRate saveRate(
      String currency, RateType rateType, LocalDate date, BigDecimal rate) {
    requireActive(currency);
    if (!Money.isPositive(rate)) {
      throw new BusinessRuleException("INVALID_RATE", "Exchange rate must be positive");
    }
    ExchangeRate entity =
        rates
            .findByCurrencyCodeAndRateTypeAndEffectiveDate(currency, rateType, date)
            .orElseGet(() -> new ExchangeRate(currency, rateType, date, rate));
    entity.setRate(rate);
    ExchangeRate saved = rates.save(entity);
    audit.record(
        "ExchangeRate",
        currency + "/" + rateType + "/" + date,
        AuditAction.UPDATE,
        "Rate set to " + rate.toPlainString());
    return saved;
  }
}
