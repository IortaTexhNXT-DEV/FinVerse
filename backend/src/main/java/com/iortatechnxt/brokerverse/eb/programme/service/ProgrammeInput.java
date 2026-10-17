package com.iortatechnxt.brokerverse.eb.programme.service;

import com.iortatechnxt.brokerverse.eb.domain.EbFunding;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeContact;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import java.util.List;

/**
 * A new programme as entered on the New Programme screen (FR-EB-021).
 *
 * @param clientId crm client (prospect or confirmed)
 * @param name programme name
 * @param teamCode team (list EB_TEAM)
 * @param funding employer or voluntary
 * @param accountOfficer AO user name, null for the current user
 * @param salesUnit sales unit, may be null
 * @param renewalEligible whether the renewal advice job renews it
 * @param lines benefit lines, at least one
 * @param contacts HR contacts, at least one
 */
public record ProgrammeInput(
    Long clientId,
    String name,
    String teamCode,
    EbFunding funding,
    String accountOfficer,
    String salesUnit,
    boolean renewalEligible,
    List<EbProgrammeLine.Data> lines,
    List<EbProgrammeContact.Data> contacts) {

  /** Defensive copies; null lists become empty. */
  public ProgrammeInput {
    lines = lines == null ? List.of() : List.copyOf(lines);
    contacts = contacts == null ? List.of() : List.copyOf(contacts);
  }

  /**
   * The maintainable data with the account officer resolved.
   *
   * @param officer account officer
   * @return profile
   */
  public EbProgramme.Profile profile(String officer) {
    return new EbProgramme.Profile(
        name == null ? null : name.strip(), teamCode, funding, officer, salesUnit, renewalEligible);
  }
}
