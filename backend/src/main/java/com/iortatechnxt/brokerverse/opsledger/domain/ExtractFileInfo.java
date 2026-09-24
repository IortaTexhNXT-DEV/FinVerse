package com.iortatechnxt.brokerverse.opsledger.domain;

import java.time.Instant;

/**
 * A file of the extract repository without its content (listing).
 *
 * @param id id
 * @param folder folder
 * @param fileName file name
 * @param contentType MIME type
 * @param sizeBytes size
 * @param sha256 checksum
 * @param sourceModule module that produced it
 * @param sourceRef business reference
 * @param createdAt stored at
 * @param createdBy stored by
 */
public record ExtractFileInfo(
    Long id,
    String folder,
    String fileName,
    String contentType,
    long sizeBytes,
    String sha256,
    String sourceModule,
    String sourceRef,
    Instant createdAt,
    String createdBy) {}
