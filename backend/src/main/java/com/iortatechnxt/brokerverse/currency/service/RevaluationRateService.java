package com.iortatechnxt.brokerverse.currency.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.currency.domain.ExchangeRate;
import com.iortatechnxt.brokerverse.currency.domain.RateType;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Monthly revaluation rate (FRBS 2.2.0, 3.6.0; AQ03): the GL Team Head enters, per currency, the
 * rate of the month end, kept as the CLOSING rate on the last day of the month (used by the FX
 * revaluation). Proposal for OQ08: with {@code OPS_BOOK_RATE_SOURCE = CLOSING_PREV_MONTH} the job
 * {@code BOOK_RATE_FROM_CLOSING} copies it as the Operations BOOK rate of the next month (rounded
 * to 2 decimals), unless a BOOK rate was already entered for that day.
 */
@Service
@Transactional
public class RevaluationRateService {

  /** Job name. */
  public static final String JOB_NAME = "BOOK_RATE_FROM_CLOSING";

  /** Parameter value that enables the copy. */
  public static final String FROM_CLOSING = "CLOSING_PREV_MONTH";

  private final CurrencyService currencies;
  private final SystemParameterService parameters;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param currencies currency service (rate maintenance)
   * @param parameters OPS_BOOK_RATE_SOURCE
   * @param audit audit trail
   */
  public RevaluationRateService(
      CurrencyService currencies, SystemParameterService parameters, AuditTrailService audit) {
    this.currencies = currencies;
    this.parameters = parameters;
    this.audit = audit;
  }

  /**
   * Enters the revaluation rate of a month.
   *
   * @param currency currency
   * @param month month
   * @param rate base units per foreign unit
   * @return the CLOSING rate on the month end
   */
  public ExchangeRate save(String currency, YearMonth month, BigDecimal rate) {
    return currencies.saveRate(currency, RateType.CLOSING, month.atEndOfMonth(), rate);
  }

  /**
   * The revaluation rates of a year (CLOSING rates on month ends).
   *
   * @param year year
   * @return rates by month end and currency
   */
  @Transactional(readOnly = true)
  public List<ExchangeRate> list(int year) {
    return currencies
        .listRates(Year.of(year).atDay(1), Year.of(year).atMonth(Month.DECEMBER).atEndOfMonth())
        .stream()
        .filter(r -> r.getRateType() == RateType.CLOSING)
        .filter(
            r -> r.getEffectiveDate().equals(YearMonth.from(r.getEffectiveDate()).atEndOfMonth()))
        .toList();
  }

  /**
   * Copies the revaluation rates of a month as the BOOK rates of the next month, when the source
   * parameter asks for it.
   *
   * @param month month whose rates are copied
   * @param force copy even when {@code OPS_BOOK_RATE_SOURCE} is not CLOSING_PREV_MONTH (manual
   *     copy)
   * @return BOOK rates created
   */
  public List<ExchangeRate> copyToBook(YearMonth month, boolean force) {
    boolean enabled =
        FROM_CLOSING.equals(parameters.text("OPS_BOOK_RATE_SOURCE", FROM_CLOSING).trim());
    if (!enabled && !force) {
      return List.of();
    }
    LocalDate target = month.plusMonths(1).atDay(1);
    Map<String, ExchangeRate> existing =
        currencies.listRates(target, target).stream()
            .filter(r -> r.getRateType() == RateType.BOOK)
            .collect(Collectors.toMap(ExchangeRate::getCurrencyCode, Function.identity()));
    List<ExchangeRate> created = new ArrayList<>();
    for (ExchangeRate closing : list(month.getYear())) {
      boolean ofMonth = YearMonth.from(closing.getEffectiveDate()).equals(month);
      if (ofMonth && !existing.containsKey(closing.getCurrencyCode())) {
        created.add(
            currencies.saveRate(
                closing.getCurrencyCode(), RateType.BOOK, target, closing.getRate()));
      }
    }
    if (!created.isEmpty()) {
      audit.record(
          "ExchangeRate",
          "BOOK/" + target,
          AuditAction.CREATE,
          "BOOK rates of " + target + " copied from the revaluation rates of " + month);
    }
    return created;
  }
}
