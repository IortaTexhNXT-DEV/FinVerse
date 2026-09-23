package com.iortatechnxt.finverse.attachment.api.dto;

import com.iortatechnxt.finverse.attachment.domain.Attachment;
import java.time.Instant;

/**
 * Attachment metadata.
 *
 * @param id id
 * @param entityType owning entity type
 * @param entityId owning entity id
 * @param fileName file name
 * @param contentType MIME type
 * @param sizeBytes size
 * @param sha256 SHA-256 checksum
 * @param description description
 * @param uploadedBy uploader
 * @param uploadedAt upload time
 */
public record AttachmentResponse(
    Long id,
    String entityType,
    String entityId,
    String fileName,
    String contentType,
    long sizeBytes,
    String sha256,
    String description,
    String uploadedBy,
    Instant uploadedAt) {

  /**
   * Maps an entity.
   *
   * @param a attachment
   * @return response
   */
  public static AttachmentResponse from(Attachment a) {
    return new AttachmentResponse(
        a.getId(),
        a.getEntityType(),
        a.getEntityId(),
        a.getFileName(),
        a.getContentType(),
        a.getSizeBytes(),
        a.getSha256(),
        a.getDescription(),
        a.getCreatedBy(),
        a.getCreatedAt());
  }
}
