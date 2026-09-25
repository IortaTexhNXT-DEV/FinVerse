package com.iortatechnxt.brokerverse.nbadmin.domain;

import java.util.Set;
import java.util.TreeSet;

/**
 * The permissions a role-permission change request adds to and removes from a role (PMADD05,
 * PRODUCT_MAINTENANCE_DESIGN section 6.3). Permission codes are the names of {@code
 * security.domain.Permission}.
 *
 * @param roleCode role to change
 * @param added permissions to grant
 * @param removed permissions to withdraw
 */
public record RolePermissionChange(String roleCode, Set<String> added, Set<String> removed) {

  /** Sorted defensive copies. */
  public RolePermissionChange {
    added = added == null ? Set.of() : Set.copyOf(new TreeSet<>(added));
    removed = removed == null ? Set.of() : Set.copyOf(new TreeSet<>(removed));
  }

  /**
   * Whether the change adds or removes anything.
   *
   * @return true when both sets are empty
   */
  public boolean isEmpty() {
    return added.isEmpty() && removed.isEmpty();
  }
}
