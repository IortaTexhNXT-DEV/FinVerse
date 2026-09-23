package com.iortatechnxt.finverse.underwriting.service;

import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.party.domain.PartyType;
import com.iortatechnxt.finverse.party.service.PartyService;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Resolves the business partners named on underwriting documents (active parties only). */
@Component
public class UnderwritingParties {

  private static final Set<PartyType> CLIENTS =
      EnumSet.of(PartyType.INDIVIDUAL_CLIENT, PartyType.CORPORATE_CLIENT);
  private static final Set<PartyType> INTERMEDIARIES =
      EnumSet.of(PartyType.AGENT, PartyType.BROKER);

  private final PartyService parties;

  /**
   * Creates the resolver.
   *
   * @param parties party service
   */
  public UnderwritingParties(PartyService parties) {
    this.parties = parties;
  }

  /**
   * Active client.
   *
   * @param companyId company
   * @param code party code
   * @return party
   */
  public Party client(Long companyId, String code) {
    return parties.requireActive(companyId, code, CLIENTS);
  }

  /**
   * Active agent or broker.
   *
   * @param companyId company
   * @param code party code, blank for none
   * @return party or null
   */
  public Party intermediary(Long companyId, String code) {
    return isBlank(code) ? null : parties.requireActive(companyId, code, INTERMEDIARIES);
  }

  /**
   * Active coinsurer.
   *
   * @param companyId company
   * @param code party code, blank for none
   * @return party or null
   */
  public Party coinsurer(Long companyId, String code) {
    return isBlank(code)
        ? null
        : parties.requireActive(companyId, code, Set.of(PartyType.COINSURER));
  }

  private static boolean isBlank(String code) {
    return code == null || code.isBlank();
  }
}
