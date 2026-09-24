package com.iortatechnxt.brokerverse.crm.domain;

import java.time.LocalDate;

/**
 * Maintainable client data.
 *
 * @param clientType individual or corporate
 * @param name person or corporate name
 * @param birthDate birth date (individuals)
 * @param identity tax and ID numbers
 * @param contact contact details and address
 * @param marketSegment market segment (list of values MARKET_SEGMENT)
 * @param bankClient whether the client is a BDO bank client
 * @param bankCif BDO customer information file number
 */
public record ClientDetails(
    ClientType clientType,
    PersonName name,
    LocalDate birthDate,
    Identity identity,
    Contact contact,
    String marketSegment,
    boolean bankClient,
    String bankCif) {

  /**
   * Name of an individual (last, first, middle, suffix) or of a corporate client.
   *
   * @param lastName last name
   * @param firstName first name
   * @param middleName middle name
   * @param suffix suffix (Jr., III)
   * @param corporateName corporate name
   */
  public record PersonName(
      String lastName, String firstName, String middleName, String suffix, String corporateName) {}

  /**
   * Tax and identity numbers.
   *
   * @param tin tax identification number
   * @param idType ID document type (list of values ID_TYPE)
   * @param idNumber ID document number
   */
  public record Identity(String tin, String idType, String idNumber) {}

  /**
   * Contact details and address.
   *
   * @param email e-mail
   * @param mobile mobile number
   * @param phone landline
   * @param addressLine street address
   * @param city city / municipality
   * @param province province
   * @param postalCode postal code
   */
  public record Contact(
      String email,
      String mobile,
      String phone,
      String addressLine,
      String city,
      String province,
      String postalCode) {}
}
