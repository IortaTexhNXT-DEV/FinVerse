package com.iortatechnxt.brokerverse.eb.programme.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeContact;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeStatus;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Programmes of the EB desk (BRID-006, 022.01; FR-EB-021): created from a client (prospect or
 * confirmed) with its benefit lines, team, funding and HR contacts, numbered {@code
 * EBP-<yyyy>-nnnnnn}; then maintained (profile, lines, contacts). A programme with a current policy
 * on any line is existing business (ACTIVE); otherwise it is a PROSPECT until its first placement.
 */
@Service
@Transactional
public class ProgrammeService {

  private final EbProgrammeRepository programmes;
  private final EbRecords records;
  private final ProgrammeRules rules;
  private final ClientService clients;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param programmes programmes
   * @param records programme look-up
   * @param rules validation
   * @param clients crm clients
   * @param numbers document numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ProgrammeService(
      EbProgrammeRepository programmes,
      EbRecords records,
      ProgrammeRules rules,
      ClientService clients,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.programmes = programmes;
    this.records = records;
    this.rules = rules;
    this.clients = clients;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Creates a programme.
   *
   * @param companyId company
   * @param input client, profile, lines and contacts
   * @return the programme
   */
  public EbProgramme create(Long companyId, ProgrammeInput input) {
    if (input.clientId() == null) {
      throw new BusinessRuleException("EB_CLIENT_REQUIRED", "Select the client");
    }
    Client client = clients.requireUsable(input.clientId());
    if (!client.getCompanyId().equals(companyId)) {
      throw new ResourceNotFoundException("Client", input.clientId());
    }
    String officer =
        input.accountOfficer() == null || input.accountOfficer().isBlank()
            ? currentUser.username()
            : input.accountOfficer().strip();
    EbProgramme.Profile profile = input.profile(officer);
    rules.checkProfile(profile);
    rules.checkNew(companyId, input.lines(), input.contacts());
    String number =
        numbers.next(
            EbCodes.series(EbCodes.PREFIX_PROGRAMME, BusinessClock.currentYear(clock).getValue()));
    EbProgramme programme =
        new EbProgramme(
            companyId,
            number,
            new EbProgramme.ClientRef(client.getId(), client.getCode(), client.getDisplayName()),
            profile);
    input.lines().forEach(programme::addLine);
    input.contacts().forEach(c -> programme.addContact(clean(c)));
    boolean existing =
        input.lines().stream()
            .anyMatch(l -> notBlank(l.currentPolicyNo()) || notBlank(l.currentArn()));
    programme.markStatus(existing ? EbProgrammeStatus.ACTIVE : EbProgrammeStatus.PROSPECT);
    EbProgramme saved = programmes.save(programme);
    audit.record(
        EbCodes.ENTITY_PROGRAMME,
        number,
        AuditAction.CREATE,
        "Programme "
            + profile.name()
            + " for "
            + client.getDisplayName()
            + " with "
            + input.lines().size()
            + " line(s)");
    return saved;
  }

  /**
   * Changes the profile of a programme.
   *
   * @param companyId company
   * @param id programme
   * @param profile name, team, funding, account officer, sales unit, renewal flag
   * @return the programme
   */
  public EbProgramme update(Long companyId, Long id, EbProgramme.Profile profile) {
    EbProgramme programme = records.programme(companyId, id);
    EbProgramme.Profile cleaned =
        new EbProgramme.Profile(
            profile.name() == null ? null : profile.name().strip(),
            profile.teamCode(),
            profile.funding(),
            profile.accountOfficer() == null || profile.accountOfficer().isBlank()
                ? programme.getAccountOfficer()
                : profile.accountOfficer().strip(),
            profile.salesUnit(),
            profile.renewalEligible());
    rules.checkProfile(cleaned);
    programme.update(cleaned);
    audit.record(
        EbCodes.ENTITY_PROGRAMME,
        programme.getProgrammeNo(),
        AuditAction.UPDATE,
        "Profile: "
            + cleaned.name()
            + ", team "
            + cleaned.teamCode()
            + ", AO "
            + cleaned.accountOfficer()
            + (cleaned.renewalEligible() ? ", eligible for renewal" : ", not for renewal"));
    return programme;
  }

  /**
   * Adds a benefit line.
   *
   * @param companyId company
   * @param id programme
   * @param data line data
   * @return the programme
   */
  public EbProgramme addLine(Long companyId, Long id, EbProgrammeLine.Data data) {
    EbProgramme programme = records.programme(companyId, id);
    rules.checkLine(companyId, data);
    EbProgrammeLine line = programme.addLine(data);
    audit.record(
        EbCodes.ENTITY_PROGRAMME,
        programme.getProgrammeNo(),
        AuditAction.UPDATE,
        "Line " + line.getLineNo() + " added: " + data.benefitLine());
    return programme;
  }

  /**
   * Changes a benefit line.
   *
   * @param companyId company
   * @param id programme
   * @param lineNo line number
   * @param data line data
   * @return the programme
   */
  public EbProgramme updateLine(Long companyId, Long id, int lineNo, EbProgrammeLine.Data data) {
    EbProgramme programme = records.programme(companyId, id);
    rules.checkLine(companyId, data);
    programme.line(lineNo).update(data);
    audit.record(
        EbCodes.ENTITY_PROGRAMME,
        programme.getProgrammeNo(),
        AuditAction.UPDATE,
        "Line " + lineNo + " changed: " + data.benefitLine() + ", ends " + data.periodTo());
    return programme;
  }

  /**
   * Removes a benefit line from the programme (kept for history); the last active line stays.
   *
   * @param companyId company
   * @param id programme
   * @param lineNo line number
   * @return the programme
   */
  public EbProgramme deactivateLine(Long companyId, Long id, int lineNo) {
    EbProgramme programme = records.programme(companyId, id);
    EbProgrammeLine line = programme.line(lineNo);
    long active = programme.getLines().stream().filter(EbProgrammeLine::isActive).count();
    if (line.isActive() && active <= 1) {
      throw new BusinessRuleException("EB_LINE_REQUIRED", "Add at least one benefit line");
    }
    line.deactivate();
    audit.record(
        EbCodes.ENTITY_PROGRAMME,
        programme.getProgrammeNo(),
        AuditAction.DEACTIVATE,
        "Line " + lineNo + " removed: " + line.getBenefitLine());
    return programme;
  }

  /**
   * Adds an HR contact.
   *
   * @param companyId company
   * @param id programme
   * @param data contact data
   * @return the programme
   */
  public EbProgramme addContact(Long companyId, Long id, EbProgrammeContact.Data data) {
    EbProgramme programme = records.programme(companyId, id);
    ProgrammeRules.checkContact(data);
    programme.addContact(clean(data));
    audit.record(
        EbCodes.ENTITY_PROGRAMME,
        programme.getProgrammeNo(),
        AuditAction.UPDATE,
        "Contact added: " + data.name().strip() + " (" + data.role() + ")");
    return programme;
  }

  /**
   * Changes an HR contact.
   *
   * @param companyId company
   * @param id programme
   * @param contactId contact
   * @param data contact data
   * @return the programme
   */
  public EbProgramme updateContact(
      Long companyId, Long id, Long contactId, EbProgrammeContact.Data data) {
    EbProgramme programme = records.programme(companyId, id);
    ProgrammeRules.checkContact(data);
    contact(programme, contactId).update(clean(data));
    audit.record(
        EbCodes.ENTITY_PROGRAMME,
        programme.getProgrammeNo(),
        AuditAction.UPDATE,
        "Contact changed: " + data.name().strip() + " (" + data.role() + ")");
    return programme;
  }

  /**
   * Removes an HR contact (kept for history); the last active contact stays.
   *
   * @param companyId company
   * @param id programme
   * @param contactId contact
   * @return the programme
   */
  public EbProgramme deactivateContact(Long companyId, Long id, Long contactId) {
    EbProgramme programme = records.programme(companyId, id);
    EbProgrammeContact contact = contact(programme, contactId);
    long active = programme.getContacts().stream().filter(EbProgrammeContact::isActive).count();
    if (contact.isActive() && active <= 1) {
      throw new BusinessRuleException("EB_CONTACT_REQUIRED", "Add at least one HR contact");
    }
    contact.deactivate();
    audit.record(
        EbCodes.ENTITY_PROGRAMME,
        programme.getProgrammeNo(),
        AuditAction.DEACTIVATE,
        "Contact removed: " + contact.getName());
    return programme;
  }

  private static EbProgrammeContact contact(EbProgramme programme, Long contactId) {
    return programme.getContacts().stream()
        .filter(c -> c.getId().equals(contactId))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("EbProgrammeContact", contactId));
  }

  private static EbProgrammeContact.Data clean(EbProgrammeContact.Data c) {
    return new EbProgrammeContact.Data(
        c.name().strip(),
        c.email().strip(),
        c.mobile() == null || c.mobile().isBlank() ? null : c.mobile().strip(),
        c.role(),
        c.receivesRa(),
        c.receivesSoa());
  }

  private static boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }
}
