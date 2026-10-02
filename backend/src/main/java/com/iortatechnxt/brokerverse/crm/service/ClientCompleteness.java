package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * Completeness of client information (BRNB.029 "flag clients where client info is incomplete"): the
 * fields a client needs are configured per client type in the parameters {@value
 * #INDIVIDUAL_FIELDS} and {@value #CORPORATE_FIELDS}. The KYC field set of the BDO standard is
 * parked (Q16), hence configurable.
 */
@Component
public class ClientCompleteness {

  /** Parameter: minimum fields of an individual client. */
  public static final String INDIVIDUAL_FIELDS = "CLIENT_MIN_FIELDS_INDIVIDUAL";

  /** Parameter: minimum fields of a corporate client. */
  public static final String CORPORATE_FIELDS = "CLIENT_MIN_FIELDS_CORPORATE";

  private static final Map<String, Field> FIELDS =
      Map.of(
          "BIRTH_DATE", new Field("Birth date", c -> c.getBirthDate() != null),
          "TIN", new Field("TIN", c -> present(c.getTin())),
          "ID", new Field("ID type and number", c -> present(c.getIdNumber())),
          "CONTACT",
              new Field("E-mail or mobile", c -> present(c.getEmail()) || present(c.getMobile())),
          "ADDRESS",
              new Field(
                  "Address and city", c -> present(c.getAddressLine()) && present(c.getCity())),
          "MARKET_SEGMENT", new Field("Market segment", c -> present(c.getMarketSegment())),
          "NATIONALITY", new Field("Nationality", c -> present(c.profile().nationality())),
          "SOURCE_OF_FUNDS",
              new Field("Source of funds", c -> present(c.profile().sourceOfFunds())),
          "OCCUPATION",
              new Field("Occupation / nature of business", c -> present(c.profile().occupation())));

  private final SystemParameterService parameters;

  /**
   * Creates the checker.
   *
   * @param parameters system parameters
   */
  public ClientCompleteness(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  /**
   * Labels of the configured minimum fields the client lacks.
   *
   * @param client client
   * @return missing fields, empty when complete
   */
  public List<String> missing(Client client) {
    String key =
        client.getClientType() == ClientType.CORPORATE ? CORPORATE_FIELDS : INDIVIDUAL_FIELDS;
    List<String> missing = new ArrayList<>();
    for (String code : parameters.items(key)) {
      Field field = FIELDS.get(code);
      if (field != null && !field.present().test(client)) {
        missing.add(field.label());
      }
    }
    return missing;
  }

  /**
   * Whether the client information is complete.
   *
   * @param client client
   * @return true when no minimum field is missing
   */
  public boolean isComplete(Client client) {
    return missing(client).isEmpty();
  }

  private static boolean present(String value) {
    return value != null && !value.isBlank();
  }

  private record Field(String label, Predicate<Client> present) {}
}
