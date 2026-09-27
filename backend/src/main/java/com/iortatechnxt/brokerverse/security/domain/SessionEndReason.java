package com.iortatechnxt.brokerverse.security.domain;

/** Why a sign-in session ended (UAM-NFR-35). */
public enum SessionEndReason {
  LOGOUT,
  IDLE_TIMEOUT,
  EXPIRED,
  ADMIN_ENDED,
  LOCKED
}
