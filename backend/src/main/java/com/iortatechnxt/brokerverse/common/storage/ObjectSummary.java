package com.iortatechnxt.brokerverse.common.storage;

import java.time.Instant;

/**
 * One entry of a bucket listing.
 *
 * @param ref object reference
 * @param size size in bytes
 * @param lastModified time of the last write
 */
public record ObjectSummary(ObjectRef ref, long size, Instant lastModified) {}
