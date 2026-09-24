package com.iortatechnxt.brokerverse.issuance.api.dto;

import com.iortatechnxt.brokerverse.issuance.domain.UploadBatch;
import com.iortatechnxt.brokerverse.issuance.domain.UploadItem;
import java.time.Instant;
import java.util.List;

/**
 * A bulk e-policy upload with its match review (BRNB.073).
 *
 * @param id id
 * @param status REVIEW, CONFIRMED or DISCARDED
 * @param fileCount files
 * @param createdAt uploaded at
 * @param createdBy uploaded by
 * @param items files with their proposed account and outcome
 */
public record UploadBatchResponse(
    Long id, String status, int fileCount, Instant createdAt, String createdBy, List<Item> items) {

  /**
   * Maps an upload.
   *
   * @param b upload
   * @return response
   */
  public static UploadBatchResponse from(UploadBatch b) {
    return new UploadBatchResponse(
        b.getId(),
        b.getStatus().name(),
        b.getFileCount(),
        b.getCreatedAt(),
        b.getCreatedBy(),
        b.getItems().stream().map(Item::from).toList());
  }

  /**
   * A file of the upload.
   *
   * @param id id
   * @param lineNo position
   * @param fileName file name
   * @param arn proposed or chosen account
   * @param matchMethod how it was matched
   * @param message explanation
   * @param included stored on confirmation
   * @param epolicyId e-policy created
   * @param outcome outcome of the confirmation
   */
  public record Item(
      Long id,
      int lineNo,
      String fileName,
      String arn,
      String matchMethod,
      String message,
      boolean included,
      Long epolicyId,
      String outcome) {

    /**
     * Maps a file.
     *
     * @param i item
     * @return response
     */
    public static Item from(UploadItem i) {
      return new Item(
          i.getId(),
          i.getLineNo(),
          i.getFileName(),
          i.getArn(),
          i.getMatchMethod() == null ? null : i.getMatchMethod().name(),
          i.getMessage(),
          i.isIncluded(),
          i.getEpolicyId(),
          i.getOutcome());
    }
  }
}
