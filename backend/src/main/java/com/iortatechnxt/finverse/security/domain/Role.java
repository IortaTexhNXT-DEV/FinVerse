package com.iortatechnxt.finverse.security.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.EnumSet;
import java.util.Set;

/** A named bundle of permissions assigned to users (e.g. ACCOUNTANT, AUTHORIZER). */
@Entity
@Table(name = "sec_role")
public class Role extends BaseEntity {

  @Column(nullable = false, unique = true, length = 40)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "sec_role_permission", joinColumns = @JoinColumn(name = "role_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "permission", nullable = false, length = 50)
  private final Set<Permission> permissions = EnumSet.noneOf(Permission.class);

  protected Role() {}

  /**
   * Creates a role.
   *
   * @param code unique role code
   * @param name display name
   */
  public Role(String code, String name) {
    this.code = code;
    this.name = name;
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

  /**
   * Returns an immutable copy of the permissions.
   *
   * @return permissions
   */
  public Set<Permission> getPermissions() {
    return permissions.isEmpty() ? Set.of() : EnumSet.copyOf(permissions);
  }

  /**
   * Replaces the role permissions.
   *
   * @param newPermissions permissions to grant
   */
  public void replacePermissions(Set<Permission> newPermissions) {
    permissions.clear();
    permissions.addAll(newPermissions);
  }
}
