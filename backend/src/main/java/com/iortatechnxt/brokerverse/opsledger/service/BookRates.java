package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.currency.domain.RateType;
import com.iortatechnxt.brokerverse.currency.service.CurrencyService;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The exchange rate of Operations postings (CSHID.012-014): the Comptrollership BOOK rate with 2
 * decimals, maintained in the currency master (rate type {@code OPS_BOOK_RATE_TYPE}, default BOOK;
 * the source table of Comptrollership is parked, OQ08). Every Operations module prices its business
 * events with {@link #price(BusinessEvent)}.
 */
@Component
@Transactional(readOnly = true)
public class BookRates {

  private final CurrencyService currencies;
  private final CompanyRepository companies;
  private final SystemParameterService parameters;

  /**
   * Creates the helper.
   *
   * @param currencies currency master
   * @param companies companies (base currency)
   * @param parameters business parameters
   */
  public BookRates(
      CurrencyService currencies, CompanyRepository companies, SystemParameterService parameters) {
    this.currencies = currencies;
    this.companies = companies;
    this.parameters = parameters;
  }

  /**
   * The rate of a currency on a date for a company.
   *
   * @param companyId company
   * @param currency transaction currency
   * @param date value date
   * @return 1 for the base currency, else the BOOK rate with 2 decimals
   */
  public BigDecimal rate(Long companyId, String currency, LocalDate date) {
    String base =
        companies
            .findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company", companyId))
            .getBaseCurrency();
    RateType type = rateType();
    return type.normalize(currencies.rateOn(base, currency, type, date));
  }

  /**
   * An event priced at the BOOK rate of its currency and value date.
   *
   * @param event business event
   * @return the event with its exchange rate
   */
  public BusinessEvent price(BusinessEvent event) {
    return event.withExchangeRate(rate(event.companyId(), event.currency(), event.valueDate()));
  }

  private RateType rateType() {
    return RateType.valueOf(
        parameters
            .text("OPS_BOOK_RATE_TYPE", RateType.BOOK.name())
            .strip()
            .toUpperCase(Locale.ROOT));
  }
}
