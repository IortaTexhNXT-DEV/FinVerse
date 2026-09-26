package com.iortatechnxt.brokerverse.storage.api.dto;

import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Metadata of a stored file (no bucket or key: the object is reached only through a link).
 *
 * @param id id
 * @param companyId company
 * @param ownerEntityType owner entity type
 * @param ownerEntityId owner key
 * @param documentType document type
 * @param recordClass record class
 * @param fileName file name
 * @param contentType content type
 * @param sizeBytes size
 * @param sha256 SHA-256
 * @param scanStatus malware scan status
 * @param retentionUntil end of retention
 * @param legalHold under legal hold
 * @param legalHoldReason reason of the hold
 * @param ecmStatus ECM archive status
 * @param ecmReference ECM reference
 * @param createdBy uploader
 * @param createdAt upload time
 */
public record StoredFileResponse(
    Long id,
    Long companyId,
    String ownerEntityType,
    String ownerEntityId,
    String documentType,
    String recordClass,
    String fileName,
    String contentType,
    long sizeBytes,
    String sha256,
    String scanStatus,
    LocalDate retentionUntil,
    boolean legalHold,
    String legalHoldReason,
    String ecmStatus,
    String ecmReference,
    String createdBy,
    Instant createdAt) {

  /**
   * Maps a file.
   *
   * @param f file
   * @return response
   */
  public static StoredFileResponse from(StoredFile f) {
    return new StoredFileResponse(
        f.getId(),
        f.getCompanyId(),
        f.getOwnerEntityType(),
        f.getOwnerEntityId(),
        f.getDocumentType(),
        f.getRecordClass(),
        f.getFileName(),
        f.getContentType(),
        f.getSizeBytes(),
        f.getSha256(),
        f.getScanStatus().name(),
        f.getRetentionUntil(),
        f.isLegalHold(),
        f.getLegalHoldReason(),
        f.getEcmStatus().name(),
        f.getEcmReference(),
        f.getCreatedBy(),
        f.getCreatedAt());
  }
}
