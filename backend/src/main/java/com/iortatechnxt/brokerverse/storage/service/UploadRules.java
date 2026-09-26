package com.iortatechnxt.brokerverse.storage.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.storage.StorageProperties;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Upload validation of the storage module: size (up to {@code brokerverse.storage.max-upload-size},
 * 25 MB by default), file type by extension from {@code brokerverse.storage.allowed-extensions},
 * confirmed by the file signature (a renamed executable is refused; the browser's content type is
 * never trusted), and a safe file name.
 */
@Component
public class UploadRules {

  private static final Pattern UNSAFE_NAME_CHARS = Pattern.compile("[\\p{Cntrl}\"\\\\/:*?<>|]");
  private static final int MAX_NAME = 255;
  private static final int TEXT_PROBE = 4096;
  private static final long MB = 1024L * 1024L;

  private static final byte[] PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
  private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
  private static final byte[] ZIP = {'P', 'K', 0x03, 0x04};
  private static final byte[] OLE2 = {
    (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
  };
  private static final byte[] TEXT = new byte[0];

  /** Extension to content type and signature. */
  private static final Map<String, Kind> KINDS =
      Map.ofEntries(
          Map.entry("pdf", new Kind("application/pdf", PDF)),
          Map.entry("png", new Kind("image/png", PNG)),
          Map.entry("jpg", new Kind("image/jpeg", JPEG)),
          Map.entry("jpeg", new Kind("image/jpeg", JPEG)),
          Map.entry(
              "xlsx",
              new Kind("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ZIP)),
          Map.entry(
              "docx",
              new Kind(
                  "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ZIP)),
          Map.entry("ods", new Kind("application/vnd.oasis.opendocument.spreadsheet", ZIP)),
          Map.entry("odt", new Kind("application/vnd.oasis.opendocument.text", ZIP)),
          Map.entry("zip", new Kind("application/zip", ZIP)),
          Map.entry("xls", new Kind("application/vnd.ms-excel", OLE2)),
          Map.entry("doc", new Kind("application/msword", OLE2)),
          Map.entry("msg", new Kind("application/vnd.ms-outlook", OLE2)),
          Map.entry("csv", new Kind("text/csv", TEXT)),
          Map.entry("txt", new Kind("text/plain", TEXT)),
          Map.entry("eml", new Kind("message/rfc822", TEXT)));

  private final StorageProperties properties;

  /**
   * Creates the rules.
   *
   * @param properties storage settings
   */
  public UploadRules(StorageProperties properties) {
    this.properties = properties;
  }

  /**
   * Refuses empty files and files above the maximum size.
   *
   * @param sizeBytes size
   * @param maxBytes largest accepted size
   */
  public void requireSize(long sizeBytes, long maxBytes) {
    if (sizeBytes <= 0) {
      throw new BusinessRuleException("FILE_EMPTY", "The file is empty");
    }
    if (sizeBytes > maxBytes) {
      throw new BusinessRuleException(
          "FILE_TOO_LARGE", "The file exceeds the maximum size of " + maxBytes / MB + " MB");
    }
  }

  /**
   * Largest file accepted through the application.
   *
   * @return bytes
   */
  public long maxUploadBytes() {
    return properties.maxUploadSize().toBytes();
  }

  /**
   * The content type of an allowed file name.
   *
   * @param fileName sanitized file name
   * @return content type
   */
  public String contentTypeOf(String fileName) {
    return kindOf(fileName).contentType();
  }

  /**
   * Checks type and signature of a file and returns its content type.
   *
   * @param fileName sanitized file name
   * @param content bytes
   * @return content type
   */
  public String requireType(String fileName, byte[] content) {
    Kind kind = kindOf(fileName);
    if (!kind.matches(content)) {
      throw new BusinessRuleException(
          "FILE_CONTENT_MISMATCH", "The file content does not match its type");
    }
    return kind.contentType();
  }

  private Kind kindOf(String fileName) {
    String ext = extension(fileName);
    Kind kind = KINDS.get(ext);
    if (kind == null || !properties.allowedExtensions().contains(ext)) {
      throw new BusinessRuleException(
          "FILE_TYPE_NOT_ALLOWED",
          "Only these file types are allowed: "
              + String.join(", ", properties.allowedExtensions()));
    }
    return kind;
  }

  /**
   * Keeps the last path segment of a name and replaces characters unsafe in file names and headers.
   *
   * @param name original name
   * @return safe name
   */
  public static String sanitize(String name) {
    String base = name == null ? "" : name;
    int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
    String clean = UNSAFE_NAME_CHARS.matcher(base.substring(slash + 1)).replaceAll("_").strip();
    if (clean.isEmpty() || clean.startsWith(".")) {
      clean = "file" + clean;
    }
    return clean.length() > MAX_NAME ? clean.substring(clean.length() - MAX_NAME) : clean;
  }

  private static String extension(String fileName) {
    int dot = fileName.lastIndexOf('.');
    return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  private record Kind(String contentType, byte[] signature) {

    boolean matches(byte[] content) {
      if (signature.length == 0) {
        byte[] probe = Arrays.copyOf(content, Math.min(content.length, TEXT_PROBE));
        for (byte b : probe) {
          if (b == 0) {
            return false;
          }
        }
        return true;
      }
      return content.length >= signature.length
          && Arrays.equals(Arrays.copyOf(content, signature.length), signature);
    }
  }
}
