package com.iortatechnxt.brokerverse.audit.service;

/**
 * Port: the group profiles (roles) a user holds now, for the audit entries of a user acting outside
 * a signed-in request (sign-in, sign-out). Implemented by the security module.
 */
public interface ActorRoles {

  /**
   * Role names of a user.
   *
   * @param username user name
   * @return role names separated by commas, null for an unknown user
   */
  String rolesOf(String username);
}
