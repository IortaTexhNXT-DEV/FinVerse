package com.iortatechnxt.brokerverse.screening.watchlist.api.dto;

import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionError;
import java.util.List;

/**
 * A run with its failed records (SNSRP-201, 202).
 *
 * @param run the run
 * @param errors failed records by line
 * @param pendingChanges changes of the run still waiting for a checker
 */
public record RunDetail(RunDto run, List<ErrorRow> errors, int pendingChanges) {

  /** Defensive copy. */
  public RunDetail {
    errors = List.copyOf(errors);
  }

  /**
   * A failed record.
   *
   * @param lineNo line in the file
   * @param rawRecord record as read
   * @param reason reason
   * @param digested whether it was sent in a digest
   */
  public record ErrorRow(int lineNo, String rawRecord, String reason, boolean digested) {

    /**
     * Maps a failed record.
     *
     * @param e record
     * @return row
     */
    public static ErrorRow from(IngestionError e) {
      return new ErrorRow(
          e.getLineNo(), e.getRawRecord(), e.getReason(), e.getDigestedAt() != null);
    }
  }
}
