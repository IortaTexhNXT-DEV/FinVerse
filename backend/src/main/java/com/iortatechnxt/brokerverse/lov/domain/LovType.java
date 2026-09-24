package com.iortatechnxt.brokerverse.lov.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A list of values, e.g. {@code RETURN_REASON}. Types are seeded by the owning module. */
@Entity
@Table(name = "lov_type")
public class LovType extends BaseEntity {

  @Column(nullable = false, length = 40, updatable = false)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(length = 300)
  private String description;

  @Column(nullable = false)
  private boolean maintainable;

  protected LovType() {}

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Whether business administrators may maintain the values.
   *
   * @return true for business lists, false for lists fixed by the system
   */
  public boolean isMaintainable() {
    return maintainable;
  }
}
