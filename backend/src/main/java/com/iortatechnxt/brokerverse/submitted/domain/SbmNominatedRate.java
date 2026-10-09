package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A nominated package rate (FR-SP-066; BRIDSP-18): per segment, vehicle classification (blank for
 * every classification) and insurer, the rate in percent of the sum insured proposed for the
 * renewal. Effective-dated, maker-checker.
 */
@Entity
@Table(name = "sbm_nominated_rate")
public class SbmNominatedRate extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(nullable = false, length = 30, updatable = false)
  private String segment;

  @Column(name = "vehicle_type", length = 40, updatable = false)
  private String vehicleType;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(nullable = false, precision = 9, scale = 6)
  private BigDecimal rate;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  protected SbmNominatedRate() {}

  /**
   * A rate pending authorization.
   *
   * @param companyId company
   * @param row values
   */
  public SbmNominatedRate(Long companyId, Row row) {
    this.companyId = companyId;
    this.segment = row.segment();
    this.vehicleType = row.vehicleType();
    this.insurerCode = row.insurerCode();
    apply(row);
  }

  /**
   * Changes the rate or dates; the row must be authorized again.
   *
   * @param row new values (segment, classification and insurer are kept)
   */
  public void change(Row row) {
    apply(row);
    markModified();
  }

  private void apply(Row row) {
    if (row.effectiveTo() != null && row.effectiveTo().isBefore(row.effectiveFrom())) {
      throw new BusinessRuleException(
          "LOV_EFFECTIVITY_INVALID", "The effective-to date is before the effective-from date");
    }
    this.rate = row.rate();
    this.effectiveFrom = row.effectiveFrom();
    this.effectiveTo = row.effectiveTo();
  }

  /**
   * Whether the rate is authorized and in force on a date.
   *
   * @param date date
   * @return true when it counts
   */
  public boolean inForce(LocalDate date) {
    return isActive()
        && !date.isBefore(effectiveFrom)
        && (effectiveTo == null || !date.isAfter(effectiveTo));
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getSegment() {
    return segment;
  }

  public String getVehicleType() {
    return vehicleType;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public BigDecimal getRate() {
    return rate;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }

  /**
   * Values of a nominated rate.
   *
   * @param segment segment (list SBM_SEGMENT)
   * @param vehicleType vehicle classification, null for every classification
   * @param insurerCode insurer
   * @param rate rate in percent of the sum insured
   * @param effectiveFrom first day
   * @param effectiveTo last day, null when open
   */
  public record Row(
      String segment,
      String vehicleType,
      String insurerCode,
      BigDecimal rate,
      LocalDate effectiveFrom,
      LocalDate effectiveTo) {}
}
