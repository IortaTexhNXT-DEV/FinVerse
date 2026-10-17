package com.iortatechnxt.brokerverse.storage.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * How the download endpoints of the modules answer for a stored file ({@code
 * brokerverse.storage.downloads.mode}): {@code redirect} sends the client to the presigned link
 * (the bytes never pass through the application; the browser must be allowed to fetch from the
 * buckets: CORS on the buckets and {@code connect-src} of the web client), {@code stream} (default)
 * reads the file through the application with its SHA-256 checked, as before ST1, until those
 * browser settings are deployed. {@code GET /api/v1/files/{id}/link} always returns the link.
 *
 * @param mode {@code redirect} or {@code stream}
 */
@ConfigurationProperties(prefix = "brokerverse.storage.downloads")
public record DownloadProperties(String mode) {

  /** Answer with a redirect to the presigned link. */
  public static final String REDIRECT = "redirect";

  /** Stream the bytes through the application. */
  public static final String STREAM = "stream";

  /** Applies the default and checks the value. */
  public DownloadProperties {
    mode = mode == null || mode.isBlank() ? STREAM : mode.strip();
    if (!REDIRECT.equals(mode) && !STREAM.equals(mode)) {
      throw new IllegalArgumentException(
          "brokerverse.storage.downloads.mode must be redirect or stream");
    }
  }

  /**
   * Whether stored files are answered with a redirect.
   *
   * @return true for {@code redirect}
   */
  public boolean redirect() {
    return REDIRECT.equals(mode);
  }
}
