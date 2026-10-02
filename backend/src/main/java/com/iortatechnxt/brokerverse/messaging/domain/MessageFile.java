package com.iortatechnxt.brokerverse.messaging.domain;

import java.util.Arrays;
import java.util.Objects;

/**
 * A file to send.
 *
 * @param fileName file name shown to the recipient
 * @param mimeType media type
 * @param content bytes
 */
public record MessageFile(String fileName, String mimeType, byte[] content) {

  /** Validates and copies the content. */
  public MessageFile {
    Objects.requireNonNull(fileName, "fileName");
    Objects.requireNonNull(mimeType, "mimeType");
    content = Objects.requireNonNull(content, "content").clone();
  }

  @Override
  public byte[] content() {
    return content.clone();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof MessageFile f
        && fileName.equals(f.fileName)
        && mimeType.equals(f.mimeType)
        && Arrays.equals(content, f.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileName, mimeType, Arrays.hashCode(content));
  }

  @Override
  public String toString() {
    return fileName + " (" + content.length + " bytes)";
  }
}
