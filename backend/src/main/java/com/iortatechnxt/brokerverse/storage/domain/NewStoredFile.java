package com.iortatechnxt.brokerverse.storage.domain;

import com.iortatechnxt.brokerverse.common.storage.ObjectRef;
import java.time.LocalDate;

/**
 * Facts of a file about to be recorded.
 *
 * @param owner owning record
 * @param documentType document type of the owning module (optional)
 * @param recordClass record class
 * @param fileName sanitized file name
 * @param contentType content type
 * @param sizeBytes size
 * @param sha256 SHA-256 (hex, lower case)
 * @param bucket bucket name
 * @param ref object reference
 * @param versionId object version (null when not versioned or not yet uploaded)
 * @param scanStatus initial scan status
 * @param retentionUntil end of retention
 */
public record NewStoredFile(
    FileOwner owner,
    String documentType,
    RecordClass recordClass,
    String fileName,
    String contentType,
    long sizeBytes,
    String sha256,
    String bucket,
    ObjectRef ref,
    String versionId,
    ScanStatus scanStatus,
    LocalDate retentionUntil) {}
