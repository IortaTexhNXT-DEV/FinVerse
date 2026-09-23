package com.iortatechnxt.finverse.currency.api;

import com.iortatechnxt.finverse.currency.api.dto.CurrencyResponse;
import com.iortatechnxt.finverse.currency.api.dto.ExchangeRateRequest;
import com.iortatechnxt.finverse.currency.api.dto.ExchangeRateResponse;
import com.iortatechnxt.finverse.currency.domain.RateType;
import com.iortatechnxt.finverse.currency.service.CurrencyService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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

  /**
   * Creates the controller.
   *
   * @param service currency service
   */
  public CurrencyController(CurrencyService service) {
    this.service = service;
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
}
