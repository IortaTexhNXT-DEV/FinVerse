package com.iortatechnxt.brokerverse.audit.domain;

/** Kinds of auditable activity. */
public enum AuditAction {
  CREATE,
  UPDATE,
  AUTHORIZE,
  REJECT,
  DEACTIVATE,
  SUBMIT,
  POST,
  REVERSE,
  OPEN,
  CLOSE,
  REOPEN,
  RUN,
  LOGIN,
  LOGIN_FAILED,
  LOGOUT,
  /** The inactivity warning was shown to the user (BDOI FRS FRUM.001.03). */
  INACTIVITY,
  /** The session timed out: inactivity sign-out or end of the session (FRUM.001.04). */
  TIMEOUT,
  EXPORT
}
