package com.iortatechnxt.finverse.attachment.domain;

/**
 * Validated facts of an uploaded file.
 *
 * @param fileName sanitized file name
 * @param contentType canonical MIME type
 * @param sizeBytes size in bytes
 * @param sha256 SHA-256 checksum (hex)
 */
public record StoredFile(String fileName, String contentType, long sizeBytes, String sha256) {}
