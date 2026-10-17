package com.iortatechnxt.brokerverse.bulk.api.dto;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import java.util.List;

/**
 * A bulk upload type.
 *
 * @param code code
 * @param title title
 * @param columns template columns
 * @param instructions extra instructions
 * @param outcomeCategories outcome categories of committed rows (BRQID.006)
 * @param blocksDuplicateFiles whether an identical earlier file is refused
 * @param requiresApproval whether a second user approves the upload before its rows apply
 * @param exportable whether the current data can be downloaded in the template layout
 * @param textFile whether the file is a text file (.txt) in a layout of its own (bank and channel
 *     payment files, CSHID.008), rather than a sheet with the template headers
 */
public record BulkHandlerResponse(
    String code,
    String title,
    List<BulkColumn> columns,
    String instructions,
    List<String> outcomeCategories,
    boolean blocksDuplicateFiles,
    boolean textFile,
    boolean requiresApproval,
    boolean exportable) {

  /**
   * Maps a handler.
   *
   * @param h handler
   * @return response
   */
  public static BulkHandlerResponse from(BulkImportHandler h) {
    return new BulkHandlerResponse(
        h.code(),
        h.title(),
        h.columns(),
        h.instructions(),
        h.outcomeCategories(),
        h.blocksDuplicateFiles(),
        h.textFile(),
        h.approvePermission() != null,
        h.exportable());
  }
}
