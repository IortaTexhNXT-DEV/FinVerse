package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.catalog.domain.CatalogRate;
import com.iortatechnxt.brokerverse.catalog.domain.CatalogRateRepository;
import com.iortatechnxt.brokerverse.catalog.domain.CommissionRate;
import com.iortatechnxt.brokerverse.catalog.domain.CommissionRateRepository;
import com.iortatechnxt.brokerverse.catalog.domain.EffectiveDatedRecord;
import com.iortatechnxt.brokerverse.catalog.domain.MotorCoverage;
import com.iortatechnxt.brokerverse.catalog.domain.MotorLimit;
import com.iortatechnxt.brokerverse.catalog.domain.MotorLimitRepository;
import com.iortatechnxt.brokerverse.catalog.domain.RateCode;
import com.iortatechnxt.brokerverse.catalog.domain.ShortPeriodRate;
import com.iortatechnxt.brokerverse.catalog.domain.ShortPeriodRateRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds the rate in force on a date: the authorized row effective on that date with the latest
 * start, a specific row (line, product) winning over the generic one.
 */
@Service
@Transactional(readOnly = true)
public class RateResolver {

  private final CatalogRateRepository rates;
  private final CommissionRateRepository commissions;
  private final ShortPeriodRateRepository shortPeriods;
  private final MotorLimitRepository motorLimits;

  /**
   * Creates the resolver.
   *
   * @param rates taxes and factors
   * @param commissions commission rates
   * @param shortPeriods short-period table
   * @param motorLimits motor limits
   */
  public RateResolver(
      CatalogRateRepository rates,
      CommissionRateRepository commissions,
      ShortPeriodRateRepository shortPeriods,
      MotorLimitRepository motorLimits) {
    this.rates = rates;
    this.commissions = commissions;
    this.shortPeriods = shortPeriods;
    this.motorLimits = motorLimits;
  }

  /**
   * A tax or factor for a line on a date (line row first, then the all-lines row).
   *
   * @param code tax or factor
   * @param lineCode product line
   * @param date rating date
   * @return rate in percent, empty when none is set up
   */
  public Optional<BigDecimal> rate(RateCode code, String lineCode, LocalDate date) {
    List<CatalogRate> rows = rates.findByRateCode(code);
    return latest(rows, date, r -> Objects.equals(lineCode, r.getLineCode()))
        .or(() -> latest(rows, date, r -> r.getLineCode() == null))
        .map(CatalogRate::getRate);
  }

  /**
   * The commission of an insurer on a product on a date (product row first, then the insurer's
   * all-products row).
   *
   * @param companyId company
   * @param insurerCode insurer party code
   * @param productCode product
   * @param date rating date
   * @return rate in percent, empty when the insurer has none
   */
  public Optional<BigDecimal> commission(
      Long companyId, String insurerCode, String productCode, LocalDate date) {
    List<CommissionRate> rows =
        commissions.findByCompanyIdAndInsurerCodeOrderByProductCodeAscEffectiveFromDesc(
            companyId, insurerCode);
    return latest(rows, date, r -> Objects.equals(productCode, r.getProductCode()))
        .or(() -> latest(rows, date, r -> r.getProductCode() == null))
        .map(CommissionRate::getRate);
  }

  /**
   * The short-period percentage for a number of months covered.
   *
   * @param months months covered (1 to 12)
   * @param date rating date
   * @return percent of the annual premium
   */
  public Optional<BigDecimal> shortPeriodPercent(int months, LocalDate date) {
    return latest(
            shortPeriods.findAllByOrderByMonthsCoveredAscEffectiveFromDesc(),
            date,
            r -> r.getMonthsCovered() == months)
        .map(ShortPeriodRate::getPercentOfAnnual);
  }

  /**
   * The premium of a motor BI or PD limit.
   *
   * @param coverage BI or PD
   * @param limit limit amount
   * @param date rating date
   * @return premium, empty when the limit is not in the table
   */
  public Optional<BigDecimal> motorLimitPremium(
      MotorCoverage coverage, BigDecimal limit, LocalDate date) {
    return latest(
            motorLimits.findAllByOrderByCoverageAscLimitAmountAscEffectiveFromDesc(),
            date,
            r -> r.getCoverage() == coverage && r.getLimitAmount().compareTo(limit) == 0)
        .map(MotorLimit::getPremium);
  }

  private static <R extends EffectiveDatedRecord> Optional<R> latest(
      List<R> rows, LocalDate date, Predicate<R> filter) {
    return rows.stream()
        .filter(r -> r.isEffectiveOn(date))
        .filter(filter)
        .max(Comparator.comparing(EffectiveDatedRecord::getEffectiveFrom));
  }
}
