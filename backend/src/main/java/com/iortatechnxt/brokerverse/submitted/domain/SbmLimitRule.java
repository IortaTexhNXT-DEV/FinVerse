package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * An acceptance or coverage limit of an insurer (BRIDSP-16; design section 4.2), maintained with
 * maker and checker until Product Maintenance takes the limits over (SP SQ08): the maximum sum
 * insured, the maximum vehicle age and one other attribute limit, per insurer and segment.
 */
@Entity
@Table(name = "sbm_limit_rule")
public class SbmLimitRule extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "insurer_code", nullable = false, length = 30)
  private String insurerCode;

  @Column(length = 30)
  private String segment;

  @Column(length = 40)
  private String line;

  @Column(name = "max_sum_insured", precision = 19, scale = 2)
  private BigDecimal maxSumInsured;

  @Column(name = "max_vehicle_age")
  private Integer maxVehicleAge;

  @Column(length = 40)
  private String attribute;

  @Column(name = "attribute_limit", length = 120)
  private String attributeLimit;

  @Column(length = 250)
  private String description;

  protected SbmLimitRule() {}

  /**
   * A limit rule.
   *
   * @param companyId company
   * @param limits insurer, segment, limits and description
   */
  public SbmLimitRule(Long companyId, Limits limits) {
    this.companyId = companyId;
    set(limits);
  }

  /**
   * Changes the limits (pending approval again).
   *
   * @param limits insurer, segment, limits and description
   */
  public final void change(Limits limits) {
    set(limits);
    markModified();
  }

  private void set(Limits limits) {
    this.insurerCode = limits.insurerCode();
    this.segment = limits.segment();
    this.line = limits.line();
    this.maxSumInsured = limits.maxSumInsured();
    this.maxVehicleAge = limits.maxVehicleAge();
    this.attribute = limits.attribute();
    this.attributeLimit = limits.attributeLimit();
    this.description = limits.description();
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getSegment() {
    return segment;
  }

  public String getLine() {
    return line;
  }

  public BigDecimal getMaxSumInsured() {
    return maxSumInsured;
  }

  public Integer getMaxVehicleAge() {
    return maxVehicleAge;
  }

  public String getAttribute() {
    return attribute;
  }

  public String getAttributeLimit() {
    return attributeLimit;
  }

  public String getDescription() {
    return description;
  }

  /**
   * The limits of a rule.
   *
   * @param insurerCode insurer
   * @param segment segment, null for all
   * @param line product line, null for all
   * @param maxSumInsured maximum sum insured, may be null
   * @param maxVehicleAge maximum vehicle age in years, may be null
   * @param attribute other fact limited (a fact of the rules), may be null
   * @param attributeLimit allowed values of that fact, comma separated
   * @param description description
   */
  public record Limits(
      String insurerCode,
      String segment,
      String line,
      BigDecimal maxSumInsured,
      Integer maxVehicleAge,
      String attribute,
      String attributeLimit,
      String description) {}
}
