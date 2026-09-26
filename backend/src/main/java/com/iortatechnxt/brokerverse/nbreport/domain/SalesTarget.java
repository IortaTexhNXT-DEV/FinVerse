package com.iortatechnxt.brokerverse.nbreport.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Production target of a sales unit for a period, in PHP (BRNB.075): booked accounts, premium and
 * commission. The hierarchy and target values are open with BDOI (Q41); the seed data seeds sample
 * targets only.
 */
@Entity
@Table(name = "nbr_sales_target")
public class SalesTarget extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Enumerated(EnumType.STRING)
  @Column(name = "unit_level", nullable = false, updatable = false, length = 12)
  private UnitLevel unitLevel;

  @Column(name = "unit_code", nullable = false, updatable = false, length = 50)
  private String unitCode;

  @Column(name = "period_from", nullable = false)
  private LocalDate periodFrom;

  @Column(name = "period_to", nullable = false)
  private LocalDate periodTo;

  @Column(name = "target_count", nullable = false)
  private int targetCount;

  @Column(name = "target_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal targetPremium;

  @Column(name = "target_commission", nullable = false, precision = 19, scale = 2)
  private BigDecimal targetCommission;

  @Column(nullable = false, length = 3)
  private String currency;

  protected SalesTarget() {}

  /**
   * Creates a target.
   *
   * @param companyId company
   * @param unit unit and period
   * @param values target values
   */
  public SalesTarget(Long companyId, Unit unit, Values values) {
    this.companyId = companyId;
    this.unitLevel = unit.level();
    this.unitCode = unit.code().strip();
    this.currency = "PHP";
    apply(unit.periodFrom(), unit.periodTo(), values);
  }

  /**
   * Changes the period and the values (the unit and the period start identify the target).
   *
   * @param from period start
   * @param to period end
   * @param values values
   */
  public final void apply(LocalDate from, LocalDate to, Values values) {
    if (from == null || to == null || to.isBefore(from)) {
      throw new BusinessRuleException(
          "TARGET_PERIOD_INVALID", "The period end must not be before the period start");
    }
    if (values.count() < 0 || negative(values.premium()) || negative(values.commission())) {
      throw new BusinessRuleException("TARGET_NEGATIVE", "Targets cannot be negative");
    }
    this.periodFrom = from;
    this.periodTo = to;
    this.targetCount = values.count();
    this.targetPremium = values.premium().setScale(2, RoundingMode.HALF_UP);
    this.targetCommission = values.commission().setScale(2, RoundingMode.HALF_UP);
  }

  private static boolean negative(BigDecimal amount) {
    return amount == null || amount.signum() < 0;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public UnitLevel getUnitLevel() {
    return unitLevel;
  }

  public String getUnitCode() {
    return unitCode;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public int getTargetCount() {
    return targetCount;
  }

  public BigDecimal getTargetPremium() {
    return targetPremium;
  }

  public BigDecimal getTargetCommission() {
    return targetCommission;
  }

  public String getCurrency() {
    return currency;
  }

  /**
   * The unit and period of a target.
   *
   * @param level unit level
   * @param code unit code (region, department or team code, or the officer's username)
   * @param periodFrom period start
   * @param periodTo period end
   */
  public record Unit(UnitLevel level, String code, LocalDate periodFrom, LocalDate periodTo) {}

  /**
   * Target values.
   *
   * @param count booked accounts
   * @param premium booked premium (PHP)
   * @param commission commission (PHP)
   */
  public record Values(int count, BigDecimal premium, BigDecimal commission) {}
}
