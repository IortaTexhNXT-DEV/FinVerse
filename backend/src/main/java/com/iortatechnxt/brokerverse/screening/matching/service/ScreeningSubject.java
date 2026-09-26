package com.iortatechnxt.brokerverse.screening.matching.service;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.screening.config.domain.SubjectType;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A client as the matching engine and the risk rules read it (SNSRP-301, 302): the names screened,
 * the attributes compared (birth date, nationality, TIN and ID number) and the attributes the risk
 * rules test (client type, occupation, source of funds, market segment). Read from the client
 * master; screening never writes it.
 *
 * @param clientId client id
 * @param companyId company
 * @param code client code, or prospect code before confirmation
 * @param displayName display name
 * @param status PROSPECT, CONFIRMED or INACTIVE
 * @param subjectType INDIVIDUAL for an individual client, ENTITY for a corporate client
 * @param names the names screened (full name, and first and last name without the middle name)
 * @param birthDate birth date, may be {@code null}
 * @param nationality nationality, may be {@code null}
 * @param ids TIN and ID number, normalised
 * @param clientType INDIVIDUAL or CORPORATE
 * @param occupation occupation code, may be {@code null}
 * @param sourceOfFunds source of funds code, may be {@code null}
 * @param marketSegment market segment code, may be {@code null}
 * @param riskRating current KYC risk rating, may be {@code null}
 */
public record ScreeningSubject(
    Long clientId,
    Long companyId,
    String code,
    String displayName,
    String status,
    SubjectType subjectType,
    List<String> names,
    LocalDate birthDate,
    String nationality,
    Set<String> ids,
    String clientType,
    String occupation,
    String sourceOfFunds,
    String marketSegment,
    String riskRating) {

  /** Defensive copies. */
  public ScreeningSubject {
    names = List.copyOf(names);
    ids = Set.copyOf(ids);
  }

  /**
   * Reads a client.
   *
   * @param c the client
   * @return the subject
   */
  public static ScreeningSubject of(Client c) {
    boolean corporate = c.getClientType() == ClientType.CORPORATE;
    Set<String> names = new LinkedHashSet<>();
    if (corporate) {
      addName(names, c.getCorporateName());
    } else {
      addName(names, join(c.getFirstName(), c.getMiddleName(), c.getLastName()));
      addName(names, join(c.getFirstName(), c.getLastName()));
    }
    addName(names, c.getDisplayName());
    Set<String> ids =
        Stream.of(c.getTin(), c.getIdNumber())
            .map(NameNormaliser::identifier)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    return new ScreeningSubject(
        c.getId(),
        c.getCompanyId(),
        c.getCode(),
        c.getDisplayName(),
        c.getStatus().name(),
        corporate ? SubjectType.ENTITY : SubjectType.INDIVIDUAL,
        List.copyOf(names),
        c.getBirthDate(),
        c.profile().nationality(),
        ids,
        c.getClientType().name(),
        c.profile().occupation(),
        c.profile().sourceOfFunds(),
        c.getMarketSegment(),
        c.getRiskRating());
  }

  /**
   * The blocking keys of all names of the subject.
   *
   * @return keys per name
   */
  public List<NameKeys> keys() {
    return names.stream().map(NameKeys::of).filter(k -> !k.isEmpty()).toList();
  }

  private static void addName(Set<String> names, String name) {
    if (name != null && !name.isBlank()) {
      names.add(name.trim());
    }
  }

  private static String join(String... parts) {
    return Stream.of(parts)
        .filter(p -> p != null && !p.isBlank())
        .map(String::trim)
        .collect(Collectors.joining(" "));
  }
}
