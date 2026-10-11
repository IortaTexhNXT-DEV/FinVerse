package com.iortatechnxt.brokerverse.common.storage;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * What the store knows about an object.
 *
 * @param size size in bytes
 * @param contentType content type
 * @param lastModified time of the last write
 * @param versionId version (null when the bucket is not versioned)
 * @param tags object tags, e.g. the malware scan result
 * @param legalHold whether an Object Lock legal hold is on
 */
public record ObjectMetadata(
    long size,
    String contentType,
    Instant lastModified,
    String versionId,
    Map<String, String> tags,
    boolean legalHold) {

  /** Copies the tags. */
  public ObjectMetadata {
    tags = tags == null ? Map.of() : Map.copyOf(tags);
  }

  /**
   * One tag.
   *
   * @param name tag name
   * @return value when present
   */
  public Optional<String> tag(String name) {
    return Optional.ofNullable(tags.get(name));
  }
}
