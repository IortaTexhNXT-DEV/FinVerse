package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * TSU routing rule (BRNB.098): an account needs TSU clearance when every criterion set on the rule
 * holds (product class, line, fleet units, locations, total sum insured, endorsement type). Rules
 * are evaluated by priority; the first match is reported.
 */
@Entity
@Table(name = "cat_tsu_rule")
public class TsuRule extends AuthorizableEntity implements CatalogRecord {

  @Column(nullable = false, length = 30, updatable = false)
  private String code;

  @Column(nullable = false, length = 200)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "product_class", nullable = false, length = 12)
  private ProductClass productClass;

  @Column(name = "line_code", length = 30)
  private String lineCode;

  @Column(name = "min_fleet_units")
  private Integer minFleetUnits;

  @Column(name = "min_locations")
  private Integer minLocations;

  @Column(name = "tsi_above", precision = 19, scale = 2)
  private BigDecimal tsiAbove;

  @Column(name = "endorsement_type", length = 40)
  private String endorsementType;

  @Column(nullable = false)
  private int priority;

  protected TsuRule() {}

  /**
   * Creates a rule, pending authorization.
   *
   * @param code code
   * @param criteria description and criteria
   */
  public TsuRule(String code, TsuCriteria criteria) {
    this.code = code;
    apply(criteria);
  }

  /**
   * Changes the rule; it must be authorized again.
   *
   * @param criteria new criteria
   */
  public void update(TsuCriteria criteria) {
    apply(criteria);
    markModified();
  }

  private void apply(TsuCriteria c) {
    this.description = c.description();
    this.productClass = c.productClass() == null ? ProductClass.ANY : c.productClass();
    this.lineCode = c.lineCode();
    this.minFleetUnits = c.minFleetUnits();
    this.minLocations = c.minLocations();
    this.tsiAbove = c.tsiAbove();
    this.endorsementType = c.endorsementType();
    this.priority = c.priority();
  }

  /**
   * Whether an account matches every criterion set on the rule.
   *
   * @param facts account facts
   * @return true when TSU must clear the account
   */
  public boolean matches(TsuFacts facts) {
    return classMatches(facts.packaged()) && sizeMatches(facts) && thresholdsMatch(facts);
  }

  private boolean sizeMatches(TsuFacts facts) {
    boolean line = lineCode == null || lineCode.equals(facts.lineCode());
    boolean fleet = minFleetUnits == null || facts.vehicleCount() >= minFleetUnits;
    boolean locations = minLocations == null || facts.locationCount() >= minLocations;
    return line && fleet && locations;
  }

  private boolean thresholdsMatch(TsuFacts facts) {
    boolean tsi =
        tsiAbove == null
            || facts.totalSumInsured() != null && facts.totalSumInsured().compareTo(tsiAbove) > 0;
    return tsi
        && (endorsementType == null || Objects.equals(endorsementType, facts.endorsementType()));
  }

  private boolean classMatches(boolean packaged) {
    return switch (productClass) {
      case PACKAGE -> packaged;
      case NON_PACKAGE -> !packaged;
      case ANY -> true;
    };
  }

  @Override
  public String catalogReference() {
    return code;
  }

  @Override
  public String catalogDescription() {
    return description;
  }

  public String getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  public ProductClass getProductClass() {
    return productClass;
  }

  public String getLineCode() {
    return lineCode;
  }

  public Integer getMinFleetUnits() {
    return minFleetUnits;
  }

  public Integer getMinLocations() {
    return minLocations;
  }

  public BigDecimal getTsiAbove() {
    return tsiAbove;
  }

  public String getEndorsementType() {
    return endorsementType;
  }

  public int getPriority() {
    return priority;
  }

  /**
   * Criteria of a rule; null criteria are not checked.
   *
   * @param description description shown to users
   * @param productClass package, non-package or any
   * @param lineCode product line, null for any
   * @param minFleetUnits minimum number of vehicles
   * @param minLocations minimum number of locations of risk
   * @param tsiAbove total sum insured strictly above this amount
   * @param endorsementType endorsement type
   * @param priority evaluation order (lowest first)
   */
  public record TsuCriteria(
      String description,
      ProductClass productClass,
      String lineCode,
      Integer minFleetUnits,
      Integer minLocations,
      BigDecimal tsiAbove,
      String endorsementType,
      int priority) {}

  /**
   * Facts of an account (or quotation) checked against the rules.
   *
   * @param packaged package product
   * @param lineCode product line
   * @param vehicleCount number of vehicles
   * @param locationCount number of locations of risk
   * @param totalSumInsured total sum insured
   * @param endorsementType endorsement type, null for new business
   */
  public record TsuFacts(
      boolean packaged,
      String lineCode,
      int vehicleCount,
      int locationCount,
      BigDecimal totalSumInsured,
      String endorsementType) {}
}
