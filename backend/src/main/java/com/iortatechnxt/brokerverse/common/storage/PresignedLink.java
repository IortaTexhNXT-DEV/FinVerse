package com.iortatechnxt.brokerverse.common.storage;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

/**
 * A short-lived link to an object.
 *
 * @param url the link (absolute for S3; relative to the application for the local store)
 * @param method HTTP method the link is valid for ({@code GET} or {@code PUT})
 * @param expiresAt end of validity
 * @param headers headers the client must send with the request (signed headers of a PUT)
 */
public record PresignedLink(
    URI url, String method, Instant expiresAt, Map<String, String> headers) {

  /** Copies the headers. */
  public PresignedLink {
    headers = headers == null ? Map.of() : Map.copyOf(headers);
  }
}
