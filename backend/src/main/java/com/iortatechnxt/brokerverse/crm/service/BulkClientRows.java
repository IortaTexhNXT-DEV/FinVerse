package com.iortatechnxt.brokerverse.crm.service;

import static com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler.ADDRESS;
import static com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler.BANK;
import static com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler.BIRTH;
import static com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler.CIF;
import static com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler.CITY;
import static com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler.CORPORATE;
import static com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler.EMAIL;
import static com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler.FIRST;
import static com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler.ID_NUMBER;
import static com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler.ID_TYPE;
import static com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler.LAST;
import static com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler.MIDDLE;
import static com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler.MOBILE;
import static com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler.NATIONALITY;
import static com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler.POSTAL;
import static com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler.PROVINCE;
import static com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler.RISK;
import static com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler.SEGMENT;
import static com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler.SOURCE;
import static com.iortatechnxt.brokerverse.crm.service.ClientBulkHandler.TIN;

import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.Contact;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.Identity;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.PersonName;
import com.iortatechnxt.brokerverse.crm.domain.ClientProfile;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Maps a bulk client row to client data (BRNB.047/065). When the row updates an existing client, a
 * blank cell keeps the client's current value.
 */
final class BulkClientRows {

  private BulkClientRows() {}

  /**
   * Client data of a row.
   *
   * @param row row
   * @param type client type
   * @param existing client being updated, or null
   * @return client data
   */
  static ClientDetails details(BulkRow row, ClientType type, Client existing) {
    Cells cells = new Cells(row, existing);
    PersonName name =
        new PersonName(
            cells.text(LAST, Client::getLastName),
            cells.text(FIRST, Client::getFirstName),
            cells.text(MIDDLE, Client::getMiddleName),
            existing == null ? null : existing.getSuffix(),
            cells.text(CORPORATE, Client::getCorporateName));
    LocalDate birth = row.date(BIRTH);
    return new ClientDetails(
        type,
        name,
        birth != null || existing == null ? birth : existing.getBirthDate(),
        new Identity(
            cells.text(TIN, Client::getTin),
            cells.text(ID_TYPE, Client::getIdType),
            cells.text(ID_NUMBER, Client::getIdNumber)),
        new Contact(
            cells.text(EMAIL, Client::getEmail),
            cells.text(MOBILE, Client::getMobile),
            existing == null ? null : existing.getPhone(),
            cells.text(ADDRESS, Client::getAddressLine),
            cells.text(CITY, Client::getCity),
            cells.text(PROVINCE, Client::getProvince),
            cells.text(POSTAL, Client::getPostalCode)),
        cells.text(SEGMENT, Client::getMarketSegment),
        row.text(BANK) != null ? row.yes(BANK) : existing != null && existing.isBankClient(),
        cells.text(CIF, Client::getBankCif));
  }

  /**
   * KYC profile of a row.
   *
   * @param row row
   * @param existing client being updated, or null
   * @return profile
   */
  static ClientProfile profile(BulkRow row, Client existing) {
    Cells cells = new Cells(row, existing);
    ClientProfile current = existing == null ? ClientProfile.EMPTY : existing.profile();
    return new ClientProfile(
        cells.text(NATIONALITY, c -> current.nationality()),
        current.civilStatus(),
        current.occupation(),
        cells.text(SOURCE, c -> current.sourceOfFunds()),
        cells.text(RISK, c -> current.riskRating()));
  }

  /**
   * Missing names for the client type.
   *
   * @param d client data
   * @return messages
   */
  static List<String> nameErrors(ClientDetails d) {
    List<String> errors = new ArrayList<>();
    PersonName n = d.name();
    if (d.clientType() == ClientType.CORPORATE) {
      if (n.corporateName() == null) {
        errors.add("Enter the Corporate Name of a corporate client");
      }
    } else if (n.lastName() == null || n.firstName() == null) {
      errors.add("Enter the Last Name and First Name of an individual client");
    }
    return errors;
  }

  /** Cells of a row with the existing client's values as fallback. */
  private record Cells(BulkRow row, Client existing) {

    String text(String header, Function<Client, String> current) {
      String value = row.text(header);
      if (value != null || existing == null) {
        return value;
      }
      return current.apply(existing);
    }
  }
}
