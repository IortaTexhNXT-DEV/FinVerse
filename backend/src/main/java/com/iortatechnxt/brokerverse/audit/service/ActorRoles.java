package com.iortatechnxt.brokerverse.audit.service;

import java.util.Collection;
import java.util.Map;

/**
 * Port: the group profiles (roles) a user holds now, for the audit entries of a user acting outside
 * a signed-in request (sign-in, sign-out), and the Windows ID of the users shown on the Audit
 * Trail. Implemented by the security module.
 */
public interface ActorRoles {

  /**
   * Role names of a user.
   *
   * @param username user name
   * @return role names separated by commas, null for an unknown user
   */
  String rolesOf(String username);

  /**
   * The Windows ID of users (BDOI FRS FRUM.008.02: User Id).
   *
   * @param usernames user names
   * @return Windows ID by user name in lower case; users without one are left out
   */
  Map<String, String> windowsIds(Collection<String> usernames);
}
