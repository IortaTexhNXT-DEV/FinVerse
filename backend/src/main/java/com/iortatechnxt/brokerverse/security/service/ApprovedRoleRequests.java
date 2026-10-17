package com.iortatechnxt.brokerverse.security.service;

import java.util.Optional;

/**
 * Port: the approved group-profile requests waiting for the System Administrator (BRD-11 p.6
 * "System Administrator to create / modify group profile"; PQ17). The Roles screen may create or
 * change a role outside the emergency path only when the call carries the number of such a request
 * for that role.
 *
 * <p>Implemented by {@code nbadmin} (wave U1-A, FOR_IMPLEMENTATION requests); until then the
 * default refuses every request number, so only the emergency path {@code UAM_DIRECT_ROLE_EDIT}
 * allows role edits. {@code security} never depends on {@code nbadmin}.
 */
public interface ApprovedRoleRequests {

  /**
   * The approver of an approved request that asks for a change of a role and is not yet
   * implemented.
   *
   * @param requestNo access request number
   * @param roleCode role the change is for
   * @return the approver when the request allows the change, empty otherwise
   */
  Optional<String> approverOf(String requestNo, String roleCode);
}
