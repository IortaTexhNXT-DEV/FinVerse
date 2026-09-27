package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Sales organisation unit (region, department or team) for production statistics (BRNB.075) with
 * the default cost center of a team (BRNB.108, dimension COST_CENTER).
 */
@Entity
@Table(name = "cat_sales_unit")
public class SalesUnit extends AuthorizableEntity implements CatalogRecord {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Enumerated(EnumType.STRING)
  @Column(name = "unit_level", nullable = false, length = 12, updatable = false)
  private SalesLevel level;

  @Column(nullable = false, length = 20, updatable = false)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(name = "parent_code", length = 20)
  private String parentCode;

  @Column(name = "cost_center", length = 20)
  private String costCenter;

  @Column(name = "head_username", length = 50)
  private String headUsername;

  protected SalesUnit() {}

  /**
   * Sets or clears the Unit Head (BRCLXN.011/012, CQ05): an operational attribute used by the
   * collection worklist, audited by the caller and not re-authorized.
   *
   * @param username head of the unit, null to clear
   */
  public void assignHead(String username) {
    this.headUsername = username;
  }

  public String getHeadUsername() {
    return headUsername;
  }

  /**
   * Creates a unit, pending authorization.
   *
   * @param companyId company
   * @param level level
   * @param code code
   * @param details name, parent and cost center
   */
  public SalesUnit(Long companyId, SalesLevel level, String code, UnitDetails details) {
    this.companyId = companyId;
    this.level = level;
    this.code = code;
    apply(details);
  }

  /**
   * Changes the unit; it must be authorized again.
   *
   * @param details new attributes
   */
  public void update(UnitDetails details) {
    apply(details);
    markModified();
  }

  private void apply(UnitDetails d) {
    this.name = d.name();
    this.parentCode = d.parentCode();
    this.costCenter = d.costCenter();
  }

  @Override
  public String catalogReference() {
    return code;
  }

  @Override
  public String catalogDescription() {
    return level + " " + name;
  }

  @Override
  public Long catalogCompanyId() {
    return companyId;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public SalesLevel getLevel() {
    return level;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getParentCode() {
    return parentCode;
  }

  public String getCostCenter() {
    return costCenter;
  }

  /**
   * Maintainable attributes of a unit.
   *
   * @param name name
   * @param parentCode parent unit (department of a team, region of a department)
   * @param costCenter default cost center (teams)
   */
  public record UnitDetails(String name, String parentCode, String costCenter) {}
}
