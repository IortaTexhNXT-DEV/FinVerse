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

  @Column(name = "owner_permission", length = 50)
  private String ownerPermission;

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

  /**
   * Permission of the module that owns the list (CLAIMS_BROKING_DESIGN 12.1): its holders may
   * maintain the values, and authorize another user's change, without the global {@code LOV_MANAGE}
   * / {@code MASTER_AUTHORIZE} (e.g. {@code BCL_SETUP} for the Claims lists).
   *
   * @return permission name, null when only the global permissions apply
   */
  public String getOwnerPermission() {
    return ownerPermission;
  }
}
