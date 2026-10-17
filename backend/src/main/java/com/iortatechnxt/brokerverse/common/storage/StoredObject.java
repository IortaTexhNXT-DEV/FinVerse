package com.iortatechnxt.brokerverse.common.storage;

/**
 * Result of a write.
 *
 * @param ref where the object is
 * @param versionId version created (null when the bucket is not versioned)
 */
public record StoredObject(ObjectRef ref, String versionId) {}
