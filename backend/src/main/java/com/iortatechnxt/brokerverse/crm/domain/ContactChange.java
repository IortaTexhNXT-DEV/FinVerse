package com.iortatechnxt.brokerverse.crm.domain;

import java.util.List;

/**
 * A change of the contact details of a client (BRCSF-004): the new e-mail, mobile, phone and
 * address lines, where a null value keeps the current one and a blank value clears it, with the
 * source of the change, its reason and the reference of the request (for the CSF the change number
 * and the verification). Name, identity, civil status and the other client data are not part of it:
 * they change through the client maintenance screens.
 *
 * @param email e-mail
 * @param mobile mobile number
 * @param phone landline
 * @param addressLine address line
 * @param city city
 * @param province province
 * @param postalCode postal code
 * @param source source of the change, e.g. {@code CSF}
 * @param reason reason of the change
 * @param reference reference of the request
 */
public record ContactChange(
    String email,
    String mobile,
    String phone,
    String addressLine,
    String city,
    String province,
    String postalCode,
    String source,
    String reason,
    String reference) {

  /** Contact fields of the contract, in display order. */
  public static final List<String> FIELDS =
      List.of("EMAIL", "MOBILE", "PHONE", "ADDRESS_LINE", "CITY", "PROVINCE", "POSTAL_CODE");

  /**
   * The value asked for a field.
   *
   * @param field one of {@link #FIELDS}
   * @return new value, null when the field keeps its value
   */
  public String valueOf(String field) {
    return switch (field) {
      case "EMAIL" -> email;
      case "MOBILE" -> mobile;
      case "PHONE" -> phone;
      case "ADDRESS_LINE" -> addressLine;
      case "CITY" -> city;
      case "PROVINCE" -> province;
      case "POSTAL_CODE" -> postalCode;
      default -> throw new IllegalArgumentException("Not a contact field: " + field);
    };
  }

  /**
   * One changed field.
   *
   * @param field one of {@link #FIELDS}
   * @param oldValue value before
   * @param newValue value after
   */
  public record FieldChange(String field, String oldValue, String newValue) {}
}
