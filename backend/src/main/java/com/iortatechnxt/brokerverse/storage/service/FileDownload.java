package com.iortatechnxt.brokerverse.storage.service;

import java.util.Arrays;
import java.util.Objects;

/**
 * What a module download endpoint answers after its own checks: a stored file (the client is sent
 * to a presigned link) or, for a record whose content is still in its {@code bytea} column (not yet
 * copied by {@code FILE_BYTEA_MIGRATION}) or built on the fly, the bytes themselves.
 *
 * @param storedFileId stored file, null for inline content
 * @param fileName file name of inline content
 * @param contentType content type of inline content
 * @param content inline content
 */
public record FileDownload(Long storedFileId, String fileName, String contentType, byte[] content) {

  /** Defensive copy. */
  public FileDownload {
    content = content == null ? null : content.clone();
  }

  /**
   * A stored file.
   *
   * @param storedFileId stored file id
   * @return download
   */
  public static FileDownload stored(Long storedFileId) {
    return new FileDownload(storedFileId, null, null, null);
  }

  /**
   * Content sent by the application itself.
   *
   * @param fileName file name
   * @param contentType content type
   * @param content bytes
   * @return download
   */
  public static FileDownload inline(String fileName, String contentType, byte[] content) {
    return new FileDownload(
        null,
        fileName,
        contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType,
        content == null ? new byte[0] : content);
  }

  /**
   * Whether the file is stored (answered with a link).
   *
   * @return true for a stored file
   */
  public boolean isStored() {
    return storedFileId != null;
  }

  @Override
  public byte[] content() {
    return content == null ? null : content.clone();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof FileDownload d
        && Objects.equals(storedFileId, d.storedFileId)
        && Objects.equals(fileName, d.fileName)
        && Arrays.equals(content, d.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(storedFileId, fileName) * 31 + Arrays.hashCode(content);
  }

  @Override
  public String toString() {
    return isStored()
        ? "FileDownload[stored " + storedFileId + "]"
        : "FileDownload[" + fileName + ", " + content.length + " bytes]";
  }
}
