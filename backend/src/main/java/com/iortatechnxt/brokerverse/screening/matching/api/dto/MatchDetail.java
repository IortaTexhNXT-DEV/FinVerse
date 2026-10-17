package com.iortatechnxt.brokerverse.screening.matching.api.dto;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.screening.watchlist.service.ListedEntry;
import java.time.LocalDate;
import java.util.List;

/**
 * A match with the client and the list entry side by side (FR-SS-032 "compares the client and the
 * entry side by side").
 *
 * @param match the match
 * @param client the client's screened data
 * @param entry the entry's current data, {@code null} when the entry is no longer readable
 */
public record MatchDetail(MatchRow match, Side client, Side entry) {

  /**
   * Maps a match with its client and entry.
   *
   * @param match the match
   * @param client the client
   * @param entry the entry, may be {@code null}
   * @return detail
   */
  public static MatchDetail from(MatchRow match, Client client, ListedEntry entry) {
    Side clientSide =
        new Side(
            client.getDisplayName(),
            client.getCode(),
            client.getClientType().name(),
            client.getStatus().name(),
            client.getBirthDate(),
            client.profile().nationality(),
            join(client.getTin(), client.getIdNumber()),
            List.of(),
            null,
            null);
    Side entrySide =
        entry == null
            ? null
            : new Side(
                entry.primaryName(),
                entry.externalRef(),
                entry.entityType().name(),
                entry.status().name(),
                entry.birthDate(),
                entry.nationality(),
                entry.idNumbers(),
                entry.aliases(),
                entry.sourceCode() + " / " + entry.listType(),
                entry.entryVersion());
    return new MatchDetail(match, clientSide, entrySide);
  }

  private static String join(String a, String b) {
    if (a == null || a.isBlank()) {
      return b;
    }
    return b == null || b.isBlank() ? a : a + "; " + b;
  }

  /**
   * One side of the comparison.
   *
   * @param name name
   * @param reference client code or list reference
   * @param type client type or entity type
   * @param status status
   * @param birthDate birth date
   * @param nationality nationality
   * @param ids TIN / ID numbers
   * @param aliases aliases (entry side)
   * @param list source and list type (entry side)
   * @param entryVersion current entry version (entry side)
   */
  public record Side(
      String name,
      String reference,
      String type,
      String status,
      LocalDate birthDate,
      String nationality,
      String ids,
      List<String> aliases,
      String list,
      Integer entryVersion) {

    /** Defensive copy. */
    public Side {
      aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }
  }
}
