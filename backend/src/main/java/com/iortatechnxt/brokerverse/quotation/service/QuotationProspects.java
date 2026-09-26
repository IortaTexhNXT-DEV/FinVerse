package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.Contact;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.PersonName;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Finds the client of a quotation request or bulk row, or creates the prospect with the minimum
 * data (BRNB.029/063/065): a person is written "Last, First", anything else is a company.
 */
@Component
public class QuotationProspects {

  private final ClientService clients;

  /**
   * Creates the helper.
   *
   * @param clients clients
   */
  public QuotationProspects(ClientService clients) {
    this.clients = clients;
  }

  /**
   * An existing client by code, or one whose name (and birth date, when given) match exactly.
   *
   * @param companyId company
   * @param code client or prospect code, may be null
   * @param name name, may be null
   * @param birthDate birth date, may be null
   * @return client
   */
  public Optional<Client> find(Long companyId, String code, String name, LocalDate birthDate) {
    if (code != null && !code.isBlank()) {
      return Optional.of(clients.requireByCode(companyId, code.strip()));
    }
    if (name == null || name.isBlank()) {
      throw new BusinessRuleException(
          "QUOTATION_CLIENT_REQUIRED", "Give the client code or the prospect's name");
    }
    String display = displayName(name);
    return clients.lookup(companyId, display).stream()
        .filter(c -> String.CASE_INSENSITIVE_ORDER.compare(c.getDisplayName(), display) == 0)
        .filter(c -> birthDate == null || birthDate.equals(c.getBirthDate()))
        .findFirst();
  }

  /**
   * Creates a prospect.
   *
   * @param companyId company
   * @param prospect name, birth date, contact and segment
   * @return the prospect
   */
  public Client create(Long companyId, Prospect prospect) {
    String[] parts = prospect.name().strip().split(",", 2);
    boolean person = parts.length == 2;
    PersonName name =
        person
            ? new PersonName(parts[0].strip(), parts[1].strip(), null, null, null)
            : new PersonName(null, null, null, null, prospect.name().strip());
    return clients.createProspect(
        companyId,
        new ClientDetails(
            person ? ClientType.INDIVIDUAL : ClientType.CORPORATE,
            name,
            person ? prospect.birthDate() : null,
            null,
            new Contact(prospect.email(), prospect.mobile(), null, null, null, null, null),
            prospect.segment(),
            false,
            null));
  }

  /**
   * The display name crm gives a name ("Last, First" for a person).
   *
   * @param name name as given
   * @return display name
   */
  static String displayName(String name) {
    String[] parts = name.strip().split(",", 2);
    return parts.length == 2 ? parts[0].strip() + ", " + parts[1].strip() : name.strip();
  }

  /**
   * Minimum data of a prospect.
   *
   * @param name "Last, First" or company name
   * @param birthDate birth date of a person, may be null
   * @param email e-mail, may be null
   * @param mobile mobile, may be null
   * @param segment market segment, may be null
   */
  public record Prospect(
      String name, LocalDate birthDate, String email, String mobile, String segment) {}
}
