package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * An insurer assignment rule of the expiry scan (BRIDSP-23; SP SQ11): per segment, vehicle type or
 * occupancy, the insurer to propose, highest priority first, optionally never the expiring
 * insurer. Maintained with maker and checker.
 */
@Entity
@Table(name = "sbm_insurer_rule")
public class SbmInsurerRule extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(nullable = false, length = 30)
  private String segment;

  @Column(name = "vehicle_type", length = 40)
  private String vehicleType;

  @Column(length = 120)
  private String occupancy;

  @Column(name = "insurer_code", nullable = false, length = 30)
  private String insurerCode;

  @Column(nullable = false)
  private int priority;

  @Column(name = "exclude_expiring", nullable = false)
  private boolean excludeExpiring;

  @Column(length = 250)
  private String description;

  protected SbmInsurerRule() {}

  /**
   * A rule.
   *
   * @param companyId company
   * @param row content
   */
  public SbmInsurerRule(Long companyId, Row row) {
    this.companyId = companyId;
    change(row);
  }

  /**
   * Changes the rule (pending approval again).
   *
   * @param row content
   */
  public void change(Row row) {
    this.segment = row.segment();
    this.vehicleType = row.vehicleType();
    this.occupancy = row.occupancy();
    this.insurerCode = row.insurerCode();
    this.priority = row.priority();
    this.excludeExpiring = row.excludeExpiring();
    this.description = row.description();
    markModified();
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

  public String getOccupancy() {
    return occupancy;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public int getPriority() {
    return priority;
  }

  public boolean isExcludeExpiring() {
    return excludeExpiring;
  }

  public String getDescription() {
    return description;
  }

  /**
   * The content of a rule.
   *
   * @param segment segment
   * @param vehicleType vehicle type, null for any
   * @param occupancy occupancy, null for any
   * @param insurerCode insurer
   * @param priority priority (highest first)
   * @param excludeExpiring never propose the expiring insurer
   * @param description description
   */
  public record Row(
      String segment,
      String vehicleType,
      String occupancy,
      String insurerCode,
      int priority,
      boolean excludeExpiring,
      String description) {}
}
