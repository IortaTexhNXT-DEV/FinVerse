package com.iortatechnxt.brokerverse.eb.service;

import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeContact;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The parties an EB step writes to and the values its templates share: insurers and their
 * placement mailboxes (catalogue), the client's HR contacts and the account officer; the programme
 * values of the {@code EB_*} templates ({@code {{programmeName}}}, {@code {{clientName}}}, ...).
 */
@Component
@Transactional(readOnly = true)
public class EbParties {

  /** Date format of the templates and documents. */
  public static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

  private final InsurerService insurers;
  private final AppUserRepository users;
  private final LovService lovs;

  /**
   * Creates the helper.
   *
   * @param insurers insurer catalogue
   * @param users users (account officer)
   * @param lovs benefit line labels
   */
  public EbParties(InsurerService insurers, AppUserRepository users, LovService lovs) {
    this.insurers = insurers;
    this.users = users;
    this.lovs = lovs;
  }

  /**
   * An insurer of the catalogue.
   *
   * @param companyId company
   * @param code insurer party code
   * @return insurer, refused when unknown
   */
  public InsurerProfile insurer(Long companyId, String code) {
    if (code == null || code.isBlank()) {
      throw new BusinessRuleException("EB_INSURER_REQUIRED", "Select the insurer");
    }
    return insurers.insurers(companyId).stream()
        .filter(i -> i.getPartyCode().equals(code.strip()))
        .findFirst()
        .orElseThrow(
            () -> new BusinessRuleException("EB_INSURER_UNKNOWN", "Insurer " + code + " is unknown"));
  }

  /**
   * An insurer's name, or its code when unknown.
   *
   * @param companyId company
   * @param code insurer code
   * @return name
   */
  public String insurerName(Long companyId, String code) {
    return insurers.insurers(companyId).stream()
        .filter(i -> i.getPartyCode().equals(code))
        .findFirst()
        .map(InsurerProfile::getName)
        .orElse(code);
  }

  /**
   * The placement mailboxes of an insurer (the addresses requests and submissions go to).
   *
   * @param insurer insurer
   * @return addresses, refused when there is none
   */
  public static List<String> mailboxes(InsurerProfile insurer) {
    List<String> to = insurer.getPlacementEmailList();
    if (to.isEmpty()) {
      throw new BusinessRuleException(
          "EB_INSURER_NO_EMAIL",
          insurer.getName() + " has no placement e-mail address in the insurer maintenance");
    }
    return to;
  }

  /**
   * The active HR contacts of a programme.
   *
   * @param programme programme
   * @return e-mail addresses, refused when there is none
   */
  public static List<String> contactEmails(EbProgramme programme) {
    List<String> to =
        programme.getContacts().stream()
            .filter(EbProgrammeContact::isActive)
            .map(EbProgrammeContact::getEmail)
            .toList();
    if (to.isEmpty()) {
      throw new BusinessRuleException(
          "EB_CONTACT_REQUIRED",
          "Programme " + programme.getProgrammeNo() + " has no active HR contact");
    }
    return to;
  }

  /**
   * The HR contacts that receive the SOA (else every active contact).
   *
   * @param programme programme
   * @return e-mail addresses, may be empty
   */
  public static List<String> soaEmails(EbProgramme programme) {
    List<String> flagged =
        programme.getContacts().stream()
            .filter(c -> c.isActive() && c.isReceivesSoa())
            .map(EbProgrammeContact::getEmail)
            .toList();
    return flagged.isEmpty()
        ? programme.getContacts().stream()
            .filter(EbProgrammeContact::isActive)
            .map(EbProgrammeContact::getEmail)
            .toList()
        : flagged;
  }

  /**
   * The name of the first active contact (salutation).
   *
   * @param programme programme
   * @return name, "Sir / Madam" when none
   */
  public static String contactName(EbProgramme programme) {
    return programme.getContacts().stream()
        .filter(EbProgrammeContact::isActive)
        .map(EbProgrammeContact::getName)
        .findFirst()
        .orElse("Sir / Madam");
  }

  /**
   * The account officer's name.
   *
   * @param programme programme
   * @return full name, else the username
   */
  public String aoName(EbProgramme programme) {
    return ao(programme)
        .map(AppUser::getFullName)
        .filter(n -> n != null && !n.isBlank())
        .orElse(programme.getAccountOfficer());
  }

  /**
   * The account officer's e-mail address.
   *
   * @param programme programme
   * @return address as a list (empty when unknown), for the copy of an e-mail
   */
  public List<String> aoCopy(EbProgramme programme) {
    return ao(programme)
        .map(AppUser::getEmail)
        .filter(e -> e != null && !e.isBlank())
        .map(List::of)
        .orElse(List.of());
  }

  private Optional<AppUser> ao(EbProgramme programme) {
    return users.findByUsernameIgnoreCase(programme.getAccountOfficer());
  }

  /**
   * The benefit lines of the programme's active lines, as labels.
   *
   * @param programme programme
   * @return e.g. "HMO, Group Life"
   */
  public String lines(EbProgramme programme) {
    return programme.getLines().stream()
        .filter(EbProgrammeLine::isActive)
        .map(l -> lovs.label("EB_BENEFIT_LINE", l.getBenefitLine()))
        .distinct()
        .collect(Collectors.joining(", "));
  }

  /**
   * The values every EB template shares.
   *
   * @param programme programme
   * @param cycle cycle, may be null
   * @return mutable map of values
   */
  public Map<String, Object> values(EbProgramme programme, EbCycle cycle) {
    Map<String, Object> values = new HashMap<>();
    values.put("programmeName", programme.getName());
    values.put("programmeNo", programme.getProgrammeNo());
    values.put("clientName", programme.getClientName());
    values.put("contactName", contactName(programme));
    values.put("lines", lines(programme));
    values.put("aoName", aoName(programme));
    if (cycle != null) {
      values.put("policyYear", cycle.getPolicyYear());
      values.put("cycleNo", cycle.getCycleNo());
      LocalDate inception = cycle.getTargetInception();
      values.put("inceptionDate", inception == null ? "to be agreed" : DATE.format(inception));
    }
    return values;
  }

  /**
   * A date as the templates show it.
   *
   * @param date date, may be null
   * @return text, empty when null
   */
  public static String date(LocalDate date) {
    return date == null ? "" : DATE.format(date);
  }
}
