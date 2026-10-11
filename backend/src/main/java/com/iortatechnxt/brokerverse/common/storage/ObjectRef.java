package com.iortatechnxt.brokerverse.common.storage;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The address of an object: bucket class and key. Keys carry no personal data and only the
 * characters of {@link ObjectKeys}, so they are safe in URLs, file systems and logs.
 *
 * @param bucket bucket class
 * @param key object key, e.g. {@code bdoi/attachment/2026/09/0b6f...}
 */
public record ObjectRef(BucketClass bucket, String key) {

  private static final Pattern KEY = Pattern.compile("[a-z0-9][a-z0-9/_.-]{0,511}");

  /** Validates the key. */
  public ObjectRef {
    Objects.requireNonNull(bucket, "bucket");
    if (!isValidKey(key)) {
      throw new IllegalArgumentException("Invalid object key");
    }
  }

  /**
   * Whether a text is a valid key: lower case letters, digits, {@code / _ . -}, at most 512
   * characters, no empty or relative path segment.
   *
   * @param key candidate
   * @return true when valid
   */
  public static boolean isValidKey(String key) {
    return key != null
        && KEY.matcher(key).matches()
        && !key.contains("..")
        && !key.contains("//")
        && !key.endsWith("/");
  }

  /**
   * The same key in another place of the bucket, e.g. under the quarantine prefix.
   *
   * @param prefix prefix ending with a slash
   * @return new reference
   */
  public ObjectRef withPrefix(String prefix) {
    return new ObjectRef(bucket, prefix + key);
  }
}
