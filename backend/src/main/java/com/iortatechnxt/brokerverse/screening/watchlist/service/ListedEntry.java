package com.iortatechnxt.brokerverse.screening.watchlist.service;

import com.iortatechnxt.brokerverse.screening.config.domain.SubjectType;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * A watchlist entry as the matching engine reads it (SNSRP-301): names, aliases and the attributes
 * compared, with the entry version that keys matches and suppressions (SQ12).
 *
 * @param id entry id
 * @param sourceCode source code (AML_ADVISORY, NLDS_PEP, INTERNAL ...)
 * @param externalRef reference in the source
 * @param listType list type code (SANCTION, PEP, INTERNAL, ADVERSE_MEDIA)
 * @param entityType individual or entity
 * @param primaryName name as listed
 * @param firstName first name, may be {@code null}
 * @param lastName last name, may be {@code null}
 * @param birthDate birth date, may be {@code null}
 * @param nationality nationality, may be {@code null}
 * @param idNumbers ID numbers, may be {@code null}
 * @param aliases alias names
 * @param entryVersion version of the entry's data
 * @param status status (the engine reads ACTIVE entries only)
 */
public record ListedEntry(
    Long id,
    String sourceCode,
    String externalRef,
    String listType,
    SubjectType entityType,
    String primaryName,
    String firstName,
    String lastName,
    LocalDate birthDate,
    String nationality,
    String idNumbers,
    List<String> aliases,
    int entryVersion,
    EntryStatus status) {

  /** Defensive copy. */
  public ListedEntry {
    aliases = aliases == null ? List.of() : List.copyOf(aliases);
  }
}
