package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientProfile;
import com.iortatechnxt.brokerverse.crm.service.ClientRules.Violation;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Component;

/**
 * Validates client data against the list-of-values types and {@link ClientRules} with the
 * configured minimum age (BRNB.030/048/049).
 */
@Component
public class ClientValidator {

  /** Parameter holding the minimum age of an individual policyholder. */
  public static final String MIN_AGE = "CLIENT_MIN_AGE";

  private static final int DEFAULT_MIN_AGE = 18;

  private final LovService lovs;
  private final SystemParameterService parameters;
  private final Clock clock;

  /**
   * Creates the validator.
   *
   * @param lovs lists of values
   * @param parameters system parameters
   * @param clock clock
   */
  public ClientValidator(LovService lovs, SystemParameterService parameters, Clock clock) {
    this.lovs = lovs;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * Validates client data; throws the first violation.
   *
   * @param details client data
   * @param profile KYC profile
   */
  public void validate(ClientDetails details, ClientProfile profile) {
    List<Violation> found = violations(details, profile);
    if (!found.isEmpty()) {
      throw new BusinessRuleException(found.get(0).code(), found.get(0).message());
    }
  }

  /**
   * Every violation of client data (bulk upload shows them all on the row).
   *
   * @param details client data
   * @param profile KYC profile
   * @return violations
   */
  public List<Violation> violations(ClientDetails details, ClientProfile profile) {
    LocalDate today = LocalDate.now(clock);
    List<Violation> found = new ArrayList<>();
    BiConsumer<String, String> lov = (type, code) -> checkLov(type, code, today, found);
    lov.accept("MARKET_SEGMENT", details.marketSegment());
    if (details.identity() != null) {
      lov.accept("ID_TYPE", details.identity().idType());
    }
    ClientProfile p = profile == null ? ClientProfile.EMPTY : profile;
    lov.accept("NATIONALITY", p.nationality());
    lov.accept("CIVIL_STATUS", p.civilStatus());
    lov.accept("SOURCE_OF_FUNDS", p.sourceOfFunds());
    lov.accept("KYC_RISK_RATING", p.riskRating());
    found.addAll(ClientRules.violations(details, today, minimumAge()));
    return found;
  }

  /**
   * Configured minimum age of an individual policyholder.
   *
   * @return years
   */
  public int minimumAge() {
    return parameters.intValue(MIN_AGE, DEFAULT_MIN_AGE);
  }

  private void checkLov(String type, String code, LocalDate today, List<Violation> found) {
    if (code == null || code.isBlank()) {
      return;
    }
    boolean usable =
        lovs.activeValues(type, today).stream().anyMatch(v -> v.getCode().equals(code));
    if (!usable) {
      found.add(
          new Violation(
              "LOV_VALUE_INVALID",
              "'" + code + "' is not a valid value of " + type + " on " + today));
    }
  }
}
