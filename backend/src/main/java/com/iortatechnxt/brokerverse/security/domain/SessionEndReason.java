package com.iortatechnxt.brokerverse.security.domain;

/**
 * Why a sign-in session ended (UAM-NFR-35). TOKEN_REUSED: a refresh token that had already been
 * replaced was presented again after the grace period, so the session is ended in case the token
 * was stolen (V1180).
 */
public enum SessionEndReason {
  LOGOUT,
  IDLE_TIMEOUT,
  EXPIRED,
  ADMIN_ENDED,
  LOCKED,
  TOKEN_REUSED
}
