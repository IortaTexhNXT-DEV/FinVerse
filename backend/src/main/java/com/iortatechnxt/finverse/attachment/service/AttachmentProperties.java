package com.iortatechnxt.finverse.attachment.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Attachment settings ({@code finverse.attachments.*}).
 *
 * @param maxSize largest accepted file (default 10 MB); keep {@code
 *     spring.servlet.multipart.max-file-size} at least this large
 */
@ConfigurationProperties(prefix = "finverse.attachments")
public record AttachmentProperties(DataSize maxSize) {

  private static final long DEFAULT_MB = 10;

  /** Canonical constructor applying the default size. */
  public AttachmentProperties {
    maxSize = maxSize == null ? DataSize.ofMegabytes(DEFAULT_MB) : maxSize;
  }
}
