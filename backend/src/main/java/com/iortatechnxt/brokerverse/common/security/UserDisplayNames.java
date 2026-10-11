package com.iortatechnxt.brokerverse.common.security;

/**
 * The name of a user as people read it, for modules that write texts about users but sit below the
 * security module (system parameters, for example). Implemented by the user directory.
 */
public interface UserDisplayNames {

  /**
   * The display name of a user: the full name, else the login id; null stays null.
   *
   * @param username login id, may be null
   * @return display name
   */
  String displayName(String username);
}
