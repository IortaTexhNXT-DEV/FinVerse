package com.iortatechnxt.brokerverse.identity.service;

import com.iortatechnxt.brokerverse.identity.domain.DirectoryAccount;
import java.util.Optional;

/**
 * Port: the user directory of the Enterprise SSO platform (BDOI FRS FRUM.002.01: validate that the
 * account exists and is active and retrieve its information; FRUM.002.02: on-demand
 * synchronisation). Implemented by the simulator of SIT and UAT or by the platform's SCIM 2.0
 * directory.
 */
public interface EnterpriseDirectory {

  /**
   * The account of a Windows ID.
   *
   * @param windowsId Windows ID
   * @return the account, empty when the directory has none
   * @throws IdentityRefused DIRECTORY_UNAVAILABLE when the directory cannot answer
   */
  Optional<DirectoryAccount> find(String windowsId);

  /**
   * Name of the directory shown to the users.
   *
   * @return name
   */
  String name();
}
