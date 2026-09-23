package com.iortatechnxt.finverse.attachment.domain;

/**
 * The record a document is attached to.
 *
 * @param entityType entity type, e.g. "JournalBatch"
 * @param entityId entity id or business key
 */
public record AttachmentTarget(String entityType, String entityId) {}
