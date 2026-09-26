package com.iortatechnxt.brokerverse.messaging.service;

/**
 * Port for the password convention of protected outbound documents (BRNB.035: "standard assigned
 * value or system-generated following a defined syntax"). The default {@link
 * GeneratedPasswordPolicy} generates a random password; the BDOI convention is parked (Q07).
 */
public interface DocumentPasswordPolicy {

  /**
   * Creates a password for one e-mail.
   *
   * @return password
   */
  String newPassword();
}
