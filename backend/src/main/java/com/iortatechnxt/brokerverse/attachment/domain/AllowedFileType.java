package com.iortatechnxt.brokerverse.attachment.domain;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * File types accepted as attachments. The type is derived from the file extension and confirmed by
 * the file signature ("magic bytes"), so a renamed executable is rejected; the browser-supplied
 * content type is never trusted. OpenDocument, legacy Office and e-mail files are accepted for
 * broking documents (BRNB.026).
 */
public enum AllowedFileType {
  PDF("application/pdf", List.of("pdf"), "%PDF-".getBytes(StandardCharsets.US_ASCII)),
  PNG("image/png", List.of("png"), new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}),
  JPEG("image/jpeg", List.of("jpg", "jpeg"), new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
  XLSX(
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      List.of("xlsx"),
      Signatures.ZIP),
  DOCX(
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      List.of("docx"),
      Signatures.ZIP),
  CSV("text/csv", List.of("csv"), new byte[0]),
  /** OpenDocument spreadsheet (BRNB.026, .ods account lists). */
  ODS("application/vnd.oasis.opendocument.spreadsheet", List.of("ods"), Signatures.ZIP),
  /** OpenDocument text. */
  ODT("application/vnd.oasis.opendocument.text", List.of("odt"), Signatures.ZIP),
  /** Legacy Excel workbook. */
  XLS("application/vnd.ms-excel", List.of("xls"), Signatures.OLE2),
  /** Legacy Word document. */
  DOC("application/msword", List.of("doc"), Signatures.OLE2),
  /** Outlook message (e.g. a client acceptance e-mail). */
  MSG("application/vnd.ms-outlook", List.of("msg"), Signatures.OLE2),
  /** Internet e-mail message. */
  EML("message/rfc822", List.of("eml"), new byte[0]);

  /** Bytes inspected for text detection. */
  private static final int TEXT_PROBE = 4096;

  private final String mimeType;
  private final List<String> extensions;
  private final byte[] signature;

  AllowedFileType(String mimeType, List<String> extensions, byte[] signature) {
    this.mimeType = mimeType;
    this.extensions = extensions;
    this.signature = signature.clone();
  }

  /**
   * Resolves the type from a file name extension.
   *
   * @param fileName file name
   * @return type if the extension is allowed
   */
  public static Optional<AllowedFileType> fromFileName(String fileName) {
    int dot = fileName.lastIndexOf('.');
    if (dot < 0) {
      return Optional.empty();
    }
    String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    return Arrays.stream(values()).filter(t -> t.extensions.contains(ext)).findFirst();
  }

  /**
   * Checks that the content really is of this type.
   *
   * @param content file bytes
   * @return true when the signature matches (text without NUL bytes for CSV)
   */
  public boolean matches(byte[] content) {
    if (this == CSV || this == EML) {
      int limit = Math.min(content.length, TEXT_PROBE);
      for (int i = 0; i < limit; i++) {
        if (content[i] == 0) {
          return false;
        }
      }
      return true;
    }
    return content.length >= signature.length
        && Arrays.equals(content, 0, signature.length, signature, 0, signature.length);
  }

  /**
   * Allowed extensions, for messages.
   *
   * @return extensions of all types
   */
  public static String allowedExtensions() {
    return String.join(", ", Arrays.stream(values()).flatMap(t -> t.extensions.stream()).toList());
  }

  public String mimeType() {
    return mimeType;
  }

  /** Shared signatures. */
  private static final class Signatures {
    static final byte[] ZIP = {'P', 'K', 0x03, 0x04};
    static final byte[] OLE2 = {
      (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };

    private Signatures() {}
  }
}
