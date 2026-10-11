package com.iortatechnxt.brokerverse.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Contact used for communications about the account, defaulted from the client (BRNB.109).
 *
 * @param name contact person
 * @param email e-mail
 * @param mobile mobile number
 * @param address mailing address
 */
@Embeddable
public record AccountContact(
    @Column(name = "contact_name", length = 200) String name,
    @Column(name = "contact_email", length = 120) String email,
    @Column(name = "contact_mobile", length = 30) String mobile,
    @Column(name = "contact_address", length = 300) String address) {

  /** No contact details. */
  public static final AccountContact NONE = new AccountContact(null, null, null, null);

  /**
   * Whether no detail is filled in.
   *
   * @return true when empty
   */
  public boolean isEmpty() {
    return blank(name) && blank(email) && blank(mobile) && blank(address);
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
