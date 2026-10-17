package com.iortatechnxt.brokerverse.eb.programme.service;

import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.EmailAddresses;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeContact;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Validation of programme data (BRID-006, 022.01; FR-EB-021): name, team and benefit line from
 * their lists, funding, an EB account officer, at least one benefit line and one HR contact, valid
 * periods, head counts and e-mail addresses, and known incumbent insurers.
 */
@Component
public class ProgrammeRules {

  /** Longest programme name. */
  static final int MAX_NAME = 200;

  private static final String AO_PERMISSION = "EB_MARKET";

  private final LovService lovs;
  private final InsurerService insurers;
  private final UserDirectory users;
  private final Clock clock;

  /**
   * Creates the rules.
   *
   * @param lovs lists of values
   * @param insurers insurer master
   * @param users users holding the AO permission
   * @param clock clock
   */
  public ProgrammeRules(
      LovService lovs, InsurerService insurers, UserDirectory users, Clock clock) {
    this.lovs = lovs;
    this.insurers = insurers;
    this.users = users;
    this.clock = clock;
  }

  /**
   * Checks the maintainable data of a programme.
   *
   * @param profile name, team, funding, account officer
   */
  public void checkProfile(EbProgramme.Profile profile) {
    checkName(profile.name());
    if (profile.teamCode() == null || profile.teamCode().isBlank()) {
      throw new BusinessRuleException("EB_TEAM_REQUIRED", "Select the team");
    }
    lovs.requireValid(EbCodes.LOV_TEAM, profile.teamCode(), today());
    if (profile.funding() == null) {
      throw new BusinessRuleException("EB_FUNDING_REQUIRED", "Select the funding");
    }
    if (!users.usersWithPermission(AO_PERMISSION).contains(profile.accountOfficer())) {
      throw new BusinessRuleException(
          "EB_AO_INVALID",
          profile.accountOfficer() + " is not an Employee Benefits account officer");
    }
  }

  private static void checkName(String name) {
    if (name == null || name.isBlank()) {
      throw new BusinessRuleException("EB_PROGRAMME_NAME_REQUIRED", "Enter the programme name");
    }
    if (name.strip().length() > MAX_NAME) {
      throw new BusinessRuleException(
          "EB_PROGRAMME_NAME_TOO_LONG",
          "The programme name can have at most " + MAX_NAME + " characters");
    }
  }

  /**
   * Checks the lines and contacts of a new programme.
   *
   * @param companyId company
   * @param lines benefit lines
   * @param contacts contacts
   */
  public void checkNew(
      Long companyId, List<EbProgrammeLine.Data> lines, List<EbProgrammeContact.Data> contacts) {
    if (lines == null || lines.isEmpty()) {
      throw new BusinessRuleException("EB_LINE_REQUIRED", "Add at least one benefit line");
    }
    if (contacts == null || contacts.isEmpty()) {
      throw new BusinessRuleException("EB_CONTACT_REQUIRED", "Add at least one HR contact");
    }
    lines.forEach(l -> checkLine(companyId, l));
    contacts.forEach(ProgrammeRules::checkContact);
  }

  /**
   * Checks a benefit line.
   *
   * @param companyId company
   * @param line line data
   */
  public void checkLine(Long companyId, EbProgrammeLine.Data line) {
    if (line.benefitLine() == null || line.benefitLine().isBlank()) {
      throw new BusinessRuleException("EB_BENEFIT_LINE_REQUIRED", "Select the benefit line");
    }
    lovs.requireValid(EbCodes.LOV_BENEFIT_LINE, line.benefitLine(), today());
    checkPeriod(line);
    if (line.headcount() != null && line.headcount() < 0) {
      throw new BusinessRuleException("EB_HEADCOUNT_INVALID", "The headcount cannot be negative");
    }
    if (line.incumbentInsurer() != null && !line.incumbentInsurer().isBlank()) {
      insurers.requireInsurer(companyId, line.incumbentInsurer());
    }
  }

  private static void checkPeriod(EbProgrammeLine.Data line) {
    if (line.periodFrom() != null
        && line.periodTo() != null
        && !line.periodTo().isAfter(line.periodFrom())) {
      throw new BusinessRuleException(
          "EB_LINE_PERIOD_INVALID", "The period must end after it starts");
    }
  }

  /**
   * Checks a contact.
   *
   * @param contact contact data
   */
  public static void checkContact(EbProgrammeContact.Data contact) {
    if (contact.name() == null || contact.name().isBlank()) {
      throw new BusinessRuleException("EB_CONTACT_NAME_REQUIRED", "Enter the contact name");
    }
    if (contact.email() == null || !EmailAddresses.isValid(contact.email().strip())) {
      throw new BusinessRuleException("EMAIL_ADDRESS_INVALID", "Enter a valid e-mail address");
    }
    if (contact.role() == null) {
      throw new BusinessRuleException("EB_CONTACT_ROLE_REQUIRED", "Select the contact role");
    }
  }

  private LocalDate today() {
    return BusinessClock.today(clock);
  }
}
