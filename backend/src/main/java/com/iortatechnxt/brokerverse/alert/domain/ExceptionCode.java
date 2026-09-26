package com.iortatechnxt.brokerverse.alert.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Exception Codes Master entry: an exception condition with its severity and the thresholds that
 * trigger it. Codes are seeded by migrations (the rules are code); administrators tune severity,
 * thresholds and activation.
 */
@Entity
@Table(name = "alt_exception_code")
public class ExceptionCode extends BaseEntity {

  @Column(nullable = false, length = 40, updatable = false)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 500)
  private String description;

  @Column(nullable = false, length = 30)
  private String module;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private AlertSeverity severity;

  @Column(name = "threshold_amount", precision = 19, scale = 2)
  private BigDecimal thresholdAmount;

  @Column(name = "threshold_days")
  private Integer thresholdDays;

  @Column(nullable = false)
  private boolean active;

  protected ExceptionCode() {}

  /**
   * Changes the tunable settings.
   *
   * @param newSeverity severity
   * @param amount threshold amount (null = not used)
   * @param days threshold days (null = not used)
   * @param isActive whether the condition is monitored
   */
  public void configure(
      AlertSeverity newSeverity, BigDecimal amount, Integer days, boolean isActive) {
    this.severity = newSeverity;
    this.thresholdAmount = amount;
    this.thresholdDays = days;
    this.active = isActive;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getModule() {
    return module;
  }

  public AlertSeverity getSeverity() {
    return severity;
  }

  public BigDecimal getThresholdAmount() {
    return thresholdAmount;
  }

  public Integer getThresholdDays() {
    return thresholdDays;
  }

  public boolean isActive() {
    return active;
  }
}
