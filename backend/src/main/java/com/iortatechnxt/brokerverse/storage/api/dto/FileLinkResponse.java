package com.iortatechnxt.brokerverse.storage.api.dto;

import com.iortatechnxt.brokerverse.common.storage.PresignedLink;
import java.time.Instant;
import java.util.Map;

/**
 * A presigned link.
 *
 * @param fileId stored file id
 * @param url the link
 * @param method HTTP method
 * @param expiresAt end of validity
 * @param headers headers the client must send (upload links)
 * @param fileName file name
 * @param contentType content type
 */
public record FileLinkResponse(
    Long fileId,
    String url,
    String method,
    Instant expiresAt,
    Map<String, String> headers,
    String fileName,
    String contentType) {

  /**
   * Maps a link.
   *
   * @param fileId stored file id
   * @param link link
   * @param fileName file name
   * @param contentType content type
   * @return response
   */
  public static FileLinkResponse from(
      Long fileId, PresignedLink link, String fileName, String contentType) {
    return new FileLinkResponse(
        fileId,
        link.url().toString(),
        link.method(),
        link.expiresAt(),
        link.headers(),
        fileName,
        contentType);
  }
}
