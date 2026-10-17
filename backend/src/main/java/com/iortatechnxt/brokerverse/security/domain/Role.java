package com.iortatechnxt.brokerverse.security.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/**
 * A named bundle of permissions assigned to users (e.g. ACCOUNTANT, AUTHORIZER); the BDOI "user
 * access group profile". A deactivated role keeps its permissions and members but grants nothing
 * until it is reactivated (BRD 3.002.3 / 3.002.4); its privilege level drives the second approval
 * of risky changes (UAM-NFR-40).
 */
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

  @Column(nullable = false)
  private boolean active = true;

  @Column(length = 500)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "privilege_level", nullable = false, length = 10)
  private PrivilegeLevel privilegeLevel = PrivilegeLevel.STANDARD;

  @Column(name = "deactivated_at")
  private Instant deactivatedAt;

  @Column(name = "deactivated_by", length = 50)
  private String deactivatedBy;

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

  /**
   * Deactivates the role: it keeps its permissions and members but grants nothing (BRD 3.002.3).
   *
   * @param user user who deactivates it
   * @param when time
   */
  public void deactivate(String user, Instant when) {
    if (!active) {
      throw new BusinessRuleException(
          "ROLE_ALREADY_INACTIVE", "Role " + code + " is already inactive");
    }
    this.active = false;
    this.deactivatedBy = user;
    this.deactivatedAt = when;
  }

  /** Reactivates the role with its last permissions (BRD 3.002.4). */
  public void reactivate() {
    if (active) {
      throw new BusinessRuleException("ROLE_ALREADY_ACTIVE", "Role " + code + " is already active");
    }
    this.active = true;
    this.deactivatedBy = null;
    this.deactivatedAt = null;
  }

  public boolean isActive() {
    return active;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public PrivilegeLevel getPrivilegeLevel() {
    return privilegeLevel;
  }

  /**
   * Sets the privilege level; null keeps the current level.
   *
   * @param level privilege level
   */
  public void setPrivilegeLevel(PrivilegeLevel level) {
    if (level != null) {
      this.privilegeLevel = level;
    }
  }

  public Instant getDeactivatedAt() {
    return deactivatedAt;
  }

  public String getDeactivatedBy() {
    return deactivatedBy;
  }
}
