package com.iortatechnxt.brokerverse.security.service;

import java.time.Instant;

/**
 * A user asked for a "Forgot password?" link (UAM-NFR-37; FR-UA-005). Published inside the
 * transaction that stores the link; the listener e-mails the link to the registered address ({@code
 * nbadmin.service.PasswordNoticeMailer}), so {@code security} does not depend on {@code messaging}.
 *
 * @param username user
 * @param fullName full name
 * @param email registered e-mail address
 * @param link the single-use link (carries the token)
 * @param expiresAt end of validity of the link
 */
public record PasswordResetRequested(
    String username, String fullName, String email, String link, Instant expiresAt) {

  @Override
  public String toString() {
    return "PasswordResetRequested[username=" + username + ", link=***]";
  }
}
