package com.iortatechnxt.brokerverse.attachment.api.dto;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
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
 * @param documentType document type (list DOCUMENT_TYPE)
 * @param linked whether the file belongs to another record and is linked to this one
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
    Instant uploadedAt,
    String documentType,
    boolean linked) {

  /**
   * Maps an entity.
   *
   * @param a attachment
   * @return response
   */
  public static AttachmentResponse from(Attachment a) {
    return from(a, null);
  }

  /**
   * Maps an entity listed for a record (a file of another record is flagged as linked).
   *
   * @param a attachment
   * @param target record the list is for, null when not relevant
   * @return response
   */
  public static AttachmentResponse from(Attachment a, AttachmentTarget target) {
    boolean own =
        target == null
            || target.entityType().equals(a.getEntityType())
                && target.entityId().equals(a.getEntityId());
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
        a.getCreatedAt(),
        a.getDocumentType(),
        !own);
  }
}
