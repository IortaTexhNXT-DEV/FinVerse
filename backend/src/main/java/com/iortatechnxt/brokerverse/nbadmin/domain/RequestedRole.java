package com.iortatechnxt.brokerverse.nbadmin.domain;

import com.iortatechnxt.brokerverse.security.domain.PrivilegeLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * The group-profile data of a CREATE_ROLE request, or the new data of a MODIFY_ROLE_PERMISSIONS
 * request (BRD 3.002.1-2): name, description and privilege level (UAM-NFR-40).
 *
 * @param name role name; null keeps the name on a change
 * @param description description; null keeps it on a change
 * @param privilegeLevel privilege level; null keeps it on a change
 */
@Embeddable
public record RequestedRole(
    @Column(name = "role_name", length = 120) String name,
    @Column(name = "role_description", length = 500) String description,
    @Enumerated(EnumType.STRING) @Column(name = "privilege_level", length = 10)
        PrivilegeLevel privilegeLevel) {

  /** Blank values are absent. */
  public RequestedRole {
    name = name == null || name.isBlank() ? null : name.trim();
    description = description == null || description.isBlank() ? null : description.trim();
  }
}
