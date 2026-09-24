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
 */
public record BulkHandlerResponse(
    String code, String title, List<BulkColumn> columns, String instructions) {

  /**
   * Maps a handler.
   *
   * @param h handler
   * @return response
   */
  public static BulkHandlerResponse from(BulkImportHandler h) {
    return new BulkHandlerResponse(h.code(), h.title(), h.columns(), h.instructions());
  }
}
