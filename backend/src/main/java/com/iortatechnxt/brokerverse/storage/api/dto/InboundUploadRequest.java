package com.iortatechnxt.brokerverse.storage.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Announces a large inbound file uploaded by presigned PUT.
 *
 * @param companyId company (optional)
 * @param ownerType owner entity type
 * @param ownerId owner key
 * @param documentType document type (optional)
 * @param recordClass record class (bucket class INBOUND)
 * @param fileName file name
 * @param sizeBytes size
 * @param sha256 SHA-256 of the file (hex)
 */
public record InboundUploadRequest(
    Long companyId,
    @NotBlank @Size(max = 60) String ownerType,
    @NotBlank @Size(max = 60) String ownerId,
    @Size(max = 40) String documentType,
    @NotBlank @Size(max = 40) String recordClass,
    @NotBlank @Size(max = 255) String fileName,
    @Positive long sizeBytes,
    @NotBlank @Pattern(regexp = "[0-9a-fA-F]{64}") String sha256) {}
