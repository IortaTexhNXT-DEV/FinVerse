package com.iortatechnxt.brokerverse.screening.watchlist.api.dto;

import com.iortatechnxt.brokerverse.screening.watchlist.domain.SourceTransport;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistSource;
import jakarta.validation.constraints.Size;

/**
 * A watchlist source (SNSRP-201); also the payload of a settings change (code, list type and
 * transport are then ignored).
 *
 * @param id id
 * @param code code
 * @param name name
 * @param listType list type
 * @param transport FILE, API or MANUAL
 * @param schedule schedule text
 * @param fileLayout CSV or XLSX
 * @param fullFile whether a file replaces the whole list
 * @param active whether the source is read
 */
public record SourceDto(
    Long id,
    String code,
    @Size(max = 100) String name,
    String listType,
    SourceTransport transport,
    @Size(max = 100) String schedule,
    String fileLayout,
    boolean fullFile,
    boolean active) {

  /**
   * Maps a source.
   *
   * @param s source
   * @return DTO
   */
  public static SourceDto from(WatchlistSource s) {
    return new SourceDto(
        s.getId(),
        s.getCode(),
        s.getName(),
        s.getListType(),
        s.getTransport(),
        s.getSchedule(),
        s.getFileLayout(),
        s.isFullFile(),
        s.isActive());
  }
}
