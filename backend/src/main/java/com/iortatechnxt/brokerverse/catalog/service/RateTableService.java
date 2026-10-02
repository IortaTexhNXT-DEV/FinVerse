package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.CatalogRate;
import com.iortatechnxt.brokerverse.catalog.domain.CatalogRateRepository;
import com.iortatechnxt.brokerverse.catalog.domain.CatalogRecord;
import com.iortatechnxt.brokerverse.catalog.domain.CommissionRate;
import com.iortatechnxt.brokerverse.catalog.domain.CommissionRate.RateValidity;
import com.iortatechnxt.brokerverse.catalog.domain.CommissionRateRepository;
import com.iortatechnxt.brokerverse.catalog.domain.MotorCoverage;
import com.iortatechnxt.brokerverse.catalog.domain.MotorLimit;
import com.iortatechnxt.brokerverse.catalog.domain.MotorLimit.LimitPrice;
import com.iortatechnxt.brokerverse.catalog.domain.MotorLimitRepository;
import com.iortatechnxt.brokerverse.catalog.domain.RateCode;
import com.iortatechnxt.brokerverse.catalog.domain.ShortPeriodRate;
import com.iortatechnxt.brokerverse.catalog.domain.ShortPeriodRateRepository;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintenance of the effective-dated rate tables (Appendix A): commission per insurer and product,
 * taxes and rating factors per line, the short-period table and the motor BI / PD limits. Every
 * change is a new or changed dated row pending authorization; rates are never constants.
 */
@Service
@Transactional
public class RateTableService {

  private final CommissionRateRepository commissions;
  private final CatalogRateRepository rates;
  private final ShortPeriodRateRepository shortPeriods;
  private final MotorLimitRepository motorLimits;
  private final InsurerService insurers;
  private final ProductCatalogService products;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param commissions commission rates
   * @param rates taxes and factors
   * @param shortPeriods short-period table
   * @param motorLimits motor limits
   * @param insurers insurers
   * @param products products and lines
   * @param audit audit trail
   */
  public RateTableService(
      CommissionRateRepository commissions,
      CatalogRateRepository rates,
      ShortPeriodRateRepository shortPeriods,
      MotorLimitRepository motorLimits,
      InsurerService insurers,
      ProductCatalogService products,
      AuditTrailService audit) {
    this.commissions = commissions;
    this.rates = rates;
    this.shortPeriods = shortPeriods;
    this.motorLimits = motorLimits;
    this.insurers = insurers;
    this.products = products;
    this.audit = audit;
  }

  /**
   * Commission rates of an insurer.
   *
   * @param companyId company
   * @param insurerCode insurer party code
   * @return rates
   */
  @Transactional(readOnly = true)
  public List<CommissionRate> commissions(Long companyId, String insurerCode) {
    return commissions.findByCompanyIdAndInsurerCodeOrderByProductCodeAscEffectiveFromDesc(
        companyId, insurerCode);
  }

  /**
   * Adds a commission rate, pending authorization.
   *
   * @param companyId company
   * @param insurerCode insurer party code
   * @param productCode product, null for every product
   * @param validity rate and effectivity
   * @return rate
   */
  public CommissionRate createCommission(
      Long companyId, String insurerCode, String productCode, RateValidity validity) {
    insurers.requireInsurer(companyId, insurerCode);
    if (productCode != null) {
      products.requireProduct(productCode);
    }
    return created(
        CatalogKind.COMMISSION_RATE,
        commissions.save(new CommissionRate(companyId, insurerCode, productCode, validity)));
  }

  /**
   * Changes a commission rate, pending authorization.
   *
   * @param id rate
   * @param validity rate and effectivity
   * @return rate
   */
  public CommissionRate updateCommission(Long id, RateValidity validity) {
    CommissionRate rate = find(commissions, CatalogKind.COMMISSION_RATE, id);
    rate.update(validity);
    return updated(CatalogKind.COMMISSION_RATE, rate);
  }

  /**
   * Every tax and rating factor row.
   *
   * @return rows
   */
  @Transactional(readOnly = true)
  public List<CatalogRate> rates() {
    return rates.findAllByOrderByRateCodeAscLineCodeAscEffectiveFromDesc();
  }

  /**
   * Adds a tax or factor row, pending authorization.
   *
   * @param code tax or factor
   * @param lineCode product line, null for every line
   * @param validity rate and effectivity
   * @return row
   */
  public CatalogRate createRate(RateCode code, String lineCode, RateValidity validity) {
    if (lineCode != null) {
      products.requireLine(lineCode);
    }
    return created(CatalogKind.RATE, rates.save(new CatalogRate(code, lineCode, validity)));
  }

  /**
   * Changes a tax or factor row, pending authorization.
   *
   * @param id row
   * @param validity rate and effectivity
   * @return row
   */
  public CatalogRate updateRate(Long id, RateValidity validity) {
    CatalogRate rate = find(rates, CatalogKind.RATE, id);
    rate.update(validity);
    return updated(CatalogKind.RATE, rate);
  }

  /**
   * The short-period table.
   *
   * @return rows
   */
  @Transactional(readOnly = true)
  public List<ShortPeriodRate> shortPeriods() {
    return shortPeriods.findAllByOrderByMonthsCoveredAscEffectiveFromDesc();
  }

  /**
   * Adds a short-period row, pending authorization.
   *
   * @param months months covered
   * @param validity percent of annual and effectivity
   * @return row
   */
  public ShortPeriodRate createShortPeriod(int months, RateValidity validity) {
    return created(
        CatalogKind.SHORT_PERIOD_RATE, shortPeriods.save(new ShortPeriodRate(months, validity)));
  }

  /**
   * Changes a short-period row, pending authorization.
   *
   * @param id row
   * @param validity percent of annual and effectivity
   * @return row
   */
  public ShortPeriodRate updateShortPeriod(Long id, RateValidity validity) {
    ShortPeriodRate row = find(shortPeriods, CatalogKind.SHORT_PERIOD_RATE, id);
    row.update(validity);
    return updated(CatalogKind.SHORT_PERIOD_RATE, row);
  }

  /**
   * The motor BI / PD limit tables.
   *
   * @return rows
   */
  @Transactional(readOnly = true)
  public List<MotorLimit> motorLimits() {
    return motorLimits.findAllByOrderByCoverageAscLimitAmountAscEffectiveFromDesc();
  }

  /**
   * Adds a motor limit row, pending authorization.
   *
   * @param coverage BI or PD
   * @param limit limit amount
   * @param price premium and effectivity
   * @return row
   */
  public MotorLimit createMotorLimit(MotorCoverage coverage, BigDecimal limit, LimitPrice price) {
    return created(
        CatalogKind.MOTOR_LIMIT, motorLimits.save(new MotorLimit(coverage, limit, price)));
  }

  /**
   * Changes a motor limit row, pending authorization.
   *
   * @param id row
   * @param price premium and effectivity
   * @return row
   */
  public MotorLimit updateMotorLimit(Long id, LimitPrice price) {
    MotorLimit row = find(motorLimits, CatalogKind.MOTOR_LIMIT, id);
    row.update(price);
    return updated(CatalogKind.MOTOR_LIMIT, row);
  }

  private static <E> E find(JpaRepository<E, Long> repository, CatalogKind kind, Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(kind.label(), id));
  }

  private <E extends CatalogRecord> E created(CatalogKind kind, E row) {
    audit.record(
        kind.label(), row.catalogReference(), AuditAction.CREATE, row.catalogDescription());
    return row;
  }

  private <E extends CatalogRecord> E updated(CatalogKind kind, E row) {
    audit.record(
        kind.label(), row.catalogReference(), AuditAction.UPDATE, row.catalogDescription());
    return row;
  }
}
