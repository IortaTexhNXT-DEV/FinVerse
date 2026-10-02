package com.iortatechnxt.brokerverse.security.service;

/**
 * A role was created, changed, deactivated or reactivated on the authority of an access request
 * (BRD-11 p.6 "System Administrator to create / modify group profile"). Published by {@link
 * UserAdminService} inside the transaction of the change; {@code nbadmin} marks the approved
 * group-profile request implemented when it was waiting for the System Administrator.
 *
 * @param requestNo access request number
 * @param roleCode role changed
 * @param actor user who made the change
 */
public record RoleChangedOnRequest(String requestNo, String roleCode, String actor) {}
