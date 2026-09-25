package com.iortatechnxt.brokerverse.screening.watchlist.api.dto;

import com.iortatechnxt.brokerverse.screening.config.domain.SubjectType;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistEntry;
import java.time.LocalDate;
import java.util.Map;

/**
 * A watchlist entry in a list (SNSRP-203).
 *
 * @param id id
 * @param sourceCode source code
 * @param externalRef reference in the source
 * @param listType list type
 * @param entityType individual or entity
 * @param primaryName name
 * @param firstName first name
 * @param lastName last name
 * @param birthDate birth date
 * @param nationality nationality
 * @param idNumbers ID numbers
 * @param listedOn listing date
 * @param delistedOn delisting date
 * @param status status
 * @param effectiveFrom effective date of the last applied change
 * @param entryVersion version of the data
 * @param remarks remarks of the last change
 */
public record EntryRow(
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
    LocalDate listedOn,
    LocalDate delistedOn,
    EntryStatus status,
    LocalDate effectiveFrom,
    int entryVersion,
    String remarks) {

  /**
   * Maps an entry.
   *
   * @param e entry
   * @param sourceCodes source codes by id
   * @return row
   */
  public static EntryRow from(WatchlistEntry e, Map<Long, String> sourceCodes) {
    return new EntryRow(
        e.getId(),
        sourceCodes.get(e.getSourceId()),
        e.getExternalRef(),
        e.getListType(),
        e.getEntityType(),
        e.getPrimaryName(),
        e.getFirstName(),
        e.getLastName(),
        e.getBirthDate(),
        e.getNationality(),
        e.getIdNumbers(),
        e.getListedOn(),
        e.getDelistedOn(),
        e.getStatus(),
        e.getEffectiveFrom(),
        e.getEntryVersion(),
        e.getRemarks());
  }
}
