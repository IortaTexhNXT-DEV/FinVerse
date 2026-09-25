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
  EXPORT
}
