package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import java.time.LocalDate;

/**
 * The identifying data of a client to check for duplicates (BRNB.032); any value may be missing.
 *
 * @param clientType type
 * @param tin TIN
 * @param idType ID type
 * @param idNumber ID number
 * @param email e-mail
 * @param mobile mobile number
 * @param lastName last name (individuals)
 * @param firstName first name (individuals)
 * @param birthDate birth date (individuals)
 * @param corporateName corporate name
 */
public record DuplicateProbe(
    ClientType clientType,
    String tin,
    String idType,
    String idNumber,
    String email,
    String mobile,
    String lastName,
    String firstName,
    LocalDate birthDate,
    String corporateName) {

  /**
   * Probe of new or changed client data.
   *
   * @param d client data
   * @return probe
   */
  public static DuplicateProbe of(ClientDetails d) {
    ClientDetails.Identity i =
        d.identity() == null ? new ClientDetails.Identity(null, null, null) : d.identity();
    ClientDetails.Contact c =
        d.contact() == null
            ? new ClientDetails.Contact(null, null, null, null, null, null, null)
            : d.contact();
    ClientDetails.PersonName n =
        d.name() == null ? new ClientDetails.PersonName(null, null, null, null, null) : d.name();
    boolean person = d.clientType() != ClientType.CORPORATE;
    return new DuplicateProbe(
        d.clientType(),
        i.tin(),
        i.idType(),
        i.idNumber(),
        c.email(),
        c.mobile(),
        person ? n.lastName() : null,
        person ? n.firstName() : null,
        d.birthDate(),
        person ? null : n.corporateName());
  }

  /**
   * Probe of a stored client (re-checked at confirmation).
   *
   * @param c client
   * @return probe
   */
  public static DuplicateProbe of(Client c) {
    return new DuplicateProbe(
        c.getClientType(),
        c.getTin(),
        c.getIdType(),
        c.getIdNumber(),
        c.getEmail(),
        c.getMobile(),
        c.getLastName(),
        c.getFirstName(),
        c.getBirthDate(),
        c.getCorporateName());
  }
}
