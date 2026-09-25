package com.iortatechnxt.brokerverse.currency.api;

import com.iortatechnxt.brokerverse.currency.api.dto.CurrencyResponse;
import com.iortatechnxt.brokerverse.currency.api.dto.ExchangeRateRequest;
import com.iortatechnxt.brokerverse.currency.api.dto.ExchangeRateResponse;
import com.iortatechnxt.brokerverse.currency.api.dto.RevaluationRateRequest;
import com.iortatechnxt.brokerverse.currency.domain.RateType;
import com.iortatechnxt.brokerverse.currency.service.CurrencyService;
import com.iortatechnxt.brokerverse.currency.service.RevaluationRateService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST API for currencies and exchange rates. */
@RestController
@RequestMapping("/api/v1/currencies")
public class CurrencyController {

  private final CurrencyService service;
  private final RevaluationRateService revaluation;

  /**
   * Creates the controller.
   *
   * @param service currency service
   * @param revaluation monthly revaluation rates
   */
  public CurrencyController(CurrencyService service, RevaluationRateService revaluation) {
    this.service = service;
    this.revaluation = revaluation;
  }

  /**
   * Lists currencies.
   *
   * @return currencies
   */
  @GetMapping
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public List<CurrencyResponse> currencies() {
    return service.listCurrencies().stream().map(CurrencyResponse::from).toList();
  }

  /**
   * Lists rates in a date range.
   *
   * @param from start date
   * @param to end date
   * @return rates
   */
  @GetMapping("/rates")
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public List<ExchangeRateResponse> rates(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return service.listRates(from, to).stream().map(ExchangeRateResponse::from).toList();
  }

  /**
   * Returns the rate in force on a date.
   *
   * @param baseCurrency base currency
   * @param currency currency
   * @param rateType rate type
   * @param date date
   * @return rate
   */
  @GetMapping("/rates/effective")
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public BigDecimal effectiveRate(
      @RequestParam String baseCurrency,
      @RequestParam String currency,
      @RequestParam(defaultValue = "SPOT") RateType rateType,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return service.rateOn(baseCurrency, currency, rateType, date);
  }

  /**
   * Creates or replaces a rate.
   *
   * @param request request
   * @return rate
   */
  @PostMapping("/rates")
  @PreAuthorize("hasAuthority('MASTER_MAINTAIN')")
  public ExchangeRateResponse saveRate(@Valid @RequestBody ExchangeRateRequest request) {
    return ExchangeRateResponse.from(
        service.saveRate(
            request.currencyCode(), request.rateType(), request.effectiveDate(), request.rate()));
  }

  /**
   * Monthly revaluation rates of a year (FRBS 2.2.0).
   *
   * @param year year
   * @return CLOSING rates on month ends
   */
  @GetMapping("/revaluation-rates")
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public List<ExchangeRateResponse> revaluationRates(@RequestParam int year) {
    return revaluation.list(year).stream().map(ExchangeRateResponse::from).toList();
  }

  /**
   * Enters the revaluation rate of a month (GL Team Head, FRBS 2.2.0).
   *
   * @param request currency, month and rate
   * @return the CLOSING rate on the month end
   */
  @PostMapping("/revaluation-rates")
  @PreAuthorize("hasAuthority('REVALUATION_RATE_MAINTAIN')")
  public ExchangeRateResponse saveRevaluationRate(
      @Valid @RequestBody RevaluationRateRequest request) {
    return ExchangeRateResponse.from(
        revaluation.save(request.currencyCode(), request.month(), request.rate()));
  }

  /**
   * Copies the revaluation rates of a month as the BOOK rates of the next month (OQ08 proposal).
   *
   * @param month month whose rates are copied
   * @return BOOK rates created
   */
  @PostMapping("/revaluation-rates/{month}/copy-to-book")
  @PreAuthorize("hasAuthority('REVALUATION_RATE_MAINTAIN')")
  public List<ExchangeRateResponse> copyToBook(@PathVariable YearMonth month) {
    return revaluation.copyToBook(month, true).stream().map(ExchangeRateResponse::from).toList();
  }
}
