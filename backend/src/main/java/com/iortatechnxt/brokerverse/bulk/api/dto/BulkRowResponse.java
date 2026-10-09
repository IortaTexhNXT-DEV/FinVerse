package com.iortatechnxt.brokerverse.bulk.api.dto;

import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRecord;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowStatus;
import java.util.Map;

/**
 * An uploaded row.
 *
 * @param rowNo row number in the file
 * @param status status
 * @param messages validation or commit messages
 * @param resultRef record created or updated
 * @param values values by column
 * @param outcome outcome category of a committed row (BRQID.006)
 * @param action ADD or UPDATE: what applying the valid row does (preview), null when not told
 * @param attempts commit attempts
 */
public record BulkRowResponse(
    int rowNo,
    BulkRowStatus status,
    String messages,
    String resultRef,
    Map<String, String> values,
    String outcome,
    int attempts,
    String action) {

  /**
   * Maps a row.
   *
   * @param r row
   * @param values its values
   * @return response
   */
  public static BulkRowResponse from(BulkRowRecord r, Map<String, String> values) {
    return new BulkRowResponse(
        r.getRowNo(),
        r.getStatus(),
        r.getMessages(),
        r.getResultRef(),
        values,
        r.getOutcome(),
        r.getAttempts(),
        r.getAction());
  }
}
