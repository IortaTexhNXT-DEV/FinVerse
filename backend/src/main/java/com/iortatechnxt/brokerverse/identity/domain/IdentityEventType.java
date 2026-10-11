package com.iortatechnxt.brokerverse.identity.domain;

/** What a provisioning event of the Enterprise SSO platform or UIDM-ISC asks for. */
public enum IdentityEventType {
  /** A new user (create). */
  JOINER,
  /** Changed details of a user (roles unchanged). */
  MOVER,
  /** A user who left (deactivate, end the sessions). */
  LEAVER,
  /** A user back (reactivate with the roles held before). */
  REHIRE,
  /** A status change (Active reactivates; Inactive, Disabled, Locked, Deactivated deactivate). */
  STATUS
}
