package com.iortatechnxt.brokerverse.screening.watchlist.domain;

import com.iortatechnxt.brokerverse.screening.config.domain.SubjectType;
import java.time.LocalDate;
import java.util.List;

/**
 * The data of a watchlist entry (SNSRP-203): the "before" and "after" values of a change and the
 * content of a list file record.
 *
 * @param listType list type code ({@code SCR_LIST_TYPE})
 * @param entityType individual or entity
 * @param primaryName name as listed
 * @param firstName first name (individuals), may be {@code null}
 * @param lastName last name (individuals), may be {@code null}
 * @param birthDate birth date, may be {@code null}
 * @param nationality nationality, may be {@code null}
 * @param idNumbers identification numbers, may be {@code null}
 * @param listedOn listing date, may be {@code null}
 * @param delistedOn delisting date, may be {@code null}
 * @param aliases aliases
 */
public record EntryValues(
    String listType,
    SubjectType entityType,
    String primaryName,
    String firstName,
    String lastName,
    LocalDate birthDate,
    String nationality,
    String idNumbers,
    LocalDate listedOn,
    LocalDate delistedOn,
    List<Alias> aliases) {

  /** Null list becomes empty. */
  public EntryValues {
    aliases = aliases == null ? List.of() : List.copyOf(aliases);
  }

  /**
   * An alias.
   *
   * @param name alias name
   * @param type AKA, FKA or SPELLING
   */
  public record Alias(String name, AliasType type) {}
}
