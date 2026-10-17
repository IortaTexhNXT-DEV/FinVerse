package com.iortatechnxt.brokerverse.eb.renewal.service;

import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeContact;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Composes the renewal advice and its reminder from templates {@code EB_RENEWAL_ADVICE} and {@code
 * EB_RA_REMINDER} (BRID-001, 002; design 8.4): programme, benefit lines, expiry, incumbent insurers
 * and the AO, addressed to the HR contacts that receive the renewal advice; and the advice as a PDF
 * on the company letterhead. The template wording is maintained by BDOI (EBQ21).
 */
@Component
public class RenewalAdviceLetter {

  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

  private final DocTemplateService templates;
  private final DocumentComposer composer;
  private final OrganizationService organizations;
  private final InsurerService insurers;
  private final AppUserRepository users;
  private final LovService lovs;

  /**
   * Creates the composer.
   *
   * @param templates document templates
   * @param composer PDF composer
   * @param organizations company name
   * @param insurers insurer names
   * @param users AO name and e-mail
   * @param lovs benefit line labels
   */
  public RenewalAdviceLetter(
      DocTemplateService templates,
      DocumentComposer composer,
      OrganizationService organizations,
      InsurerService insurers,
      AppUserRepository users,
      LovService lovs) {
    this.templates = templates;
    this.composer = composer;
    this.organizations = organizations;
    this.insurers = insurers;
    this.users = users;
    this.lovs = lovs;
  }

  /**
   * The HR contacts that receive the renewal advice.
   *
   * @param programme programme
   * @return active contacts flagged for the renewal advice
   */
  public static List<EbProgrammeContact> recipients(EbProgramme programme) {
    return programme.getContacts().stream().filter(c -> c.isActive() && c.isReceivesRa()).toList();
  }

  /**
   * The renewal advice of a cycle.
   *
   * @param programme programme
   * @param cycle renewal cycle
   * @param expiry expiry of the lines it announces
   * @param date date (template version in force)
   * @return subject and body
   */
  public MergedText advice(EbProgramme programme, EbCycle cycle, LocalDate expiry, LocalDate date) {
    return merge(EbCodes.TEMPLATE_RENEWAL_ADVICE, programme, cycle, expiry, date);
  }

  /**
   * The reminder of a renewal advice.
   *
   * @param programme programme
   * @param cycle renewal cycle
   * @param expiry expiry announced
   * @param date date (template version in force)
   * @return subject and body
   */
  public MergedText reminder(
      EbProgramme programme, EbCycle cycle, LocalDate expiry, LocalDate date) {
    return merge(EbCodes.TEMPLATE_RA_REMINDER, programme, cycle, expiry, date);
  }

  private MergedText merge(
      String template, EbProgramme programme, EbCycle cycle, LocalDate expiry, LocalDate date) {
    Map<String, Object> values = values(programme, cycle, expiry);
    MergedText text = templates.merge(template, date, values);
    return new MergedText(
        text.code(), text.versionNo(), DocTemplateService.fill(text.title(), values), text.text());
  }

  /**
   * The renewal advice as a PDF on the company letterhead.
   *
   * @param programme programme
   * @param cycle renewal cycle
   * @param expiry expiry announced
   * @param text composed advice
   * @return PDF bytes
   */
  public byte[] pdf(EbProgramme programme, EbCycle cycle, LocalDate expiry, MergedText text) {
    DocumentSpec spec =
        new DocumentSpec(
            organizations.getCompany(programme.getCompanyId()).getName(),
            "RENEWAL ADVICE",
            cycle.getCycleNo(),
            List.of(
                new Fields(
                    "Programme",
                    List.of(
                        new Field("Client", programme.getClientName()),
                        new Field("Programme", programme.getName()),
                        new Field("Programme no.", programme.getProgrammeNo()),
                        new Field("Benefit lines", lines(programme)),
                        new Field("Expiry", DATE.format(expiry)),
                        new Field("Current insurer(s)", insurerNames(programme)))),
                new Text(text.title(), text.text())),
            List.of(aoName(programme)),
            text.versionTag());
    return composer.pdf(spec);
  }

  /**
   * The AO's e-mail address, copied on the advice.
   *
   * @param programme programme
   * @return address, null when unknown
   */
  public String aoEmail(EbProgramme programme) {
    return users
        .findByUsernameIgnoreCase(programme.getAccountOfficer())
        .map(AppUser::getEmail)
        .filter(e -> e != null && !e.isBlank())
        .orElse(null);
  }

  private Map<String, Object> values(EbProgramme programme, EbCycle cycle, LocalDate expiry) {
    Map<String, Object> values = new HashMap<>();
    values.put("contactName", contactNames(programme));
    values.put("programmeName", programme.getName());
    values.put("programmeNo", programme.getProgrammeNo());
    values.put("clientName", programme.getClientName());
    values.put("lines", lines(programme));
    values.put("expiryDate", DATE.format(expiry));
    values.put("policyYear", cycle.getPolicyYear());
    values.put("insurers", insurerNames(programme));
    values.put("aoName", aoName(programme));
    return values;
  }

  private static String contactNames(EbProgramme programme) {
    return recipients(programme).stream()
        .map(EbProgrammeContact::getName)
        .collect(Collectors.joining(", "));
  }

  private String lines(EbProgramme programme) {
    return activeLines(programme).stream()
        .map(l -> lovs.label(EbCodes.LOV_BENEFIT_LINE, l.getBenefitLine()))
        .distinct()
        .collect(Collectors.joining(", "));
  }

  private String insurerNames(EbProgramme programme) {
    Map<String, String> names =
        insurers.insurers(programme.getCompanyId()).stream()
            .collect(
                Collectors.toMap(
                    InsurerProfile::getPartyCode, InsurerProfile::getName, (a, b) -> a));
    Set<String> result = new LinkedHashSet<>();
    activeLines(programme).stream()
        .map(EbProgrammeLine::getIncumbentInsurer)
        .filter(Objects::nonNull)
        .forEach(code -> result.add(names.getOrDefault(code, code)));
    return result.isEmpty() ? "-" : String.join(", ", result);
  }

  private String aoName(EbProgramme programme) {
    return users
        .findByUsernameIgnoreCase(programme.getAccountOfficer())
        .map(AppUser::getFullName)
        .filter(n -> n != null && !n.isBlank())
        .orElse(programme.getAccountOfficer());
  }

  private static List<EbProgrammeLine> activeLines(EbProgramme programme) {
    return programme.getLines().stream().filter(EbProgrammeLine::isActive).toList();
  }
}
