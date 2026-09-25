package com.iortatechnxt.brokerverse.security.service;

/**
 * Published when a role was created or changed directly on the Roles screen through the emergency
 * path {@code UAM_DIRECT_ROLE_EDIT} (PQ17), after the change. {@code nbadmin} raises the alert
 * {@code UAM_DIRECT_ROLE_EDIT} from it.
 *
 * @param roleCode role
 * @param change what was done ("created" / "changed")
 * @param actor user who did it
 */
public record DirectRoleEditUsed(String roleCode, String change, String actor) {}
