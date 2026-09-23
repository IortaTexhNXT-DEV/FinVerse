package com.iortatechnxt.finverse.dimension.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** A value of a financial dimension, e.g. cost centre "FIN" or business line "MOTOR". */
@Entity
@Table(name = "dim_value")
public class DimensionValue extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Enumerated(EnumType.STRING)
  @Column(name = "dimension_type", nullable = false, length = 20)
  private DimensionType type;

  @Column(nullable = false, length = 20)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false)
  private boolean active = true;

  protected DimensionValue() {}

  /**
   * Creates a dimension value.
   *
   * @param companyId company
   * @param type dimension type
   * @param code code
   * @param name name
   */
  public DimensionValue(Long companyId, DimensionType type, String code, String name) {
    this.companyId = companyId;
    this.type = type;
    this.code = code;
    this.name = name;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public DimensionType getType() {
    return type;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
