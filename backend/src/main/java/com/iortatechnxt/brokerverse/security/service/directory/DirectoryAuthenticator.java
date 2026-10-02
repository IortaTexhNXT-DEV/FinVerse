package com.iortatechnxt.brokerverse.security.service.directory;

/**
 * Port checking a sign-in password (USER_ACCESS_DESIGN sections 1.5 and 10; FR-UA-003). The LOCAL
 * adapter ({@link LocalPasswordAuthenticator}) checks the password held by BrokerVerse. A DIRECTORY
 * adapter (BDO EUA with the Windows ID, LDAP / Active Directory bind) registers a bean of this
 * interface whose {@link #mode()} is {@link AuthMode#DIRECTORY}; it is parked until BDO supplies
 * the protocol, host and messages (Q42, UQ04). Without it, {@code AUTH_MODE} = DIRECTORY refuses
 * every sign-in with a service message.
 *
 * <p>The lockout counter, the audit and the session log stay in {@code AuthService} and are the
 * same in both modes; roles and permissions always come from BrokerVerse. A directory adapter never
 * stores the password.
 */
public interface DirectoryAuthenticator {

  /**
   * The sign-in mode this adapter serves.
   *
   * @return mode
   */
  AuthMode mode();

  /**
   * Checks a password.
   *
   * @param userId the BrokerVerse user name (LOCAL) or the Windows ID (DIRECTORY)
   * @param password the password entered; the caller clears it afterwards
   * @return the outcome, with the message of the authenticator
   */
  DirectoryResult authenticate(String userId, char[] password);
}
