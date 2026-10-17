package com.iortatechnxt.brokerverse.screening.watchlist.api.dto;

import com.iortatechnxt.brokerverse.screening.config.domain.SubjectType;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryValues;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * A manual addition or change of a watchlist entry (FR-SS-022). Mandatory fields are checked by the
 * service with the FRS messages.
 *
 * @param sourceCode source (additions; blank = INTERNAL)
 * @param listType list type code
 * @param entityType individual or entity
 * @param primaryName name as listed
 * @param firstName first name
 * @param lastName last name
 * @param birthDate birth date
 * @param nationality nationality
 * @param idNumbers ID numbers
 * @param listedOn listing date
 * @param delistedOn delisting date
 * @param aliases aliases
 * @param remarks reason for the change
 */
public record EntryRequest(
    String sourceCode,
    String listType,
    SubjectType entityType,
    @Size(max = 300) String primaryName,
    @Size(max = 100) String firstName,
    @Size(max = 100) String lastName,
    LocalDate birthDate,
    @Size(max = 60) String nationality,
    @Size(max = 300) String idNumbers,
    LocalDate listedOn,
    LocalDate delistedOn,
    List<EntryValues.Alias> aliases,
    @Size(max = 2000) String remarks) {

  /**
   * The entry values of the request.
   *
   * @return values
   */
  public EntryValues values() {
    return new EntryValues(
        listType,
        entityType,
        primaryName,
        firstName,
        lastName,
        birthDate,
        nationality,
        idNumbers,
        listedOn,
        delistedOn,
        aliases);
  }
}
