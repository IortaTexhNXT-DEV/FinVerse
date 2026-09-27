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
  EML("message/rfc822", List.of("eml"), new byte[0]),
  /** Plain text (BRCSF-007): text without NUL bytes. */
  TXT("text/plain", List.of("txt"), new byte[0]),
  /** Rich text (BRCSF-007). */
  RTF("application/rtf", List.of("rtf"), "{\\rtf".getBytes(StandardCharsets.US_ASCII)),
  /**
   * HEIF / HEIC photo (BRCSF-007): an ISO media file whose {@code ftyp} box at offset 4 names an
   * image brand.
   */
  HEIC("image/heic", List.of("heic", "heif"), "ftyp".getBytes(StandardCharsets.US_ASCII)),
  /** GIF image (BRCSF-007). */
  GIF("image/gif", List.of("gif"), "GIF8".getBytes(StandardCharsets.US_ASCII)),
  /** Bitmap image (BRCSF-007). */
  BMP("image/bmp", List.of("bmp"), "BM".getBytes(StandardCharsets.US_ASCII)),
  /** TIFF image, little or big endian (BRCSF-007). */
  TIFF("image/tiff", List.of("tif", "tiff"), new byte[] {'I', 'I', '*', 0}),
  /** WebP image: a RIFF container of form type WEBP (BRCSF-007). */
  WEBP("image/webp", List.of("webp"), "RIFF".getBytes(StandardCharsets.US_ASCII));

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
    return switch (this) {
      case CSV, EML, TXT -> isText(content);
      case HEIC -> Signatures.isHeif(content);
      case TIFF -> startsWith(content, signature) || startsWith(content, Signatures.TIFF_BIG);
      case WEBP ->
          startsWith(content, signature)
              && Signatures.at(content, Signatures.BRAND_OFFSET, Signatures.WEBP);
      default -> startsWith(content, signature);
    };
  }

  private static boolean isText(byte[] content) {
    int limit = Math.min(content.length, TEXT_PROBE);
    for (int i = 0; i < limit; i++) {
      if (content[i] == 0) {
        return false;
      }
    }
    return true;
  }

  private static boolean startsWith(byte[] content, byte[] prefix) {
    return Signatures.at(content, 0, prefix);
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

    static final byte[] TIFF_BIG = {'M', 'M', 0, '*'};
    static final byte[] WEBP = "WEBP".getBytes(StandardCharsets.US_ASCII);
    static final byte[] FTYP = "ftyp".getBytes(StandardCharsets.US_ASCII);

    /** Brands of an ISO media file that hold a HEIF / HEIC image. */
    static final List<String> HEIF_BRANDS = List.of("heic", "heix", "heif", "mif1", "msf1", "hevc");

    private static final int FTYP_OFFSET = 4;
    static final int BRAND_OFFSET = 8;
    private static final int BRAND_LENGTH = 4;

    private Signatures() {}

    static boolean at(byte[] content, int offset, byte[] expected) {
      return content.length >= offset + expected.length
          && Arrays.equals(content, offset, offset + expected.length, expected, 0, expected.length);
    }

    static boolean isHeif(byte[] content) {
      if (!at(content, FTYP_OFFSET, FTYP) || content.length < BRAND_OFFSET + BRAND_LENGTH) {
        return false;
      }
      String brand =
          new String(content, BRAND_OFFSET, BRAND_LENGTH, StandardCharsets.US_ASCII)
              .toLowerCase(Locale.ROOT);
      return HEIF_BRANDS.contains(brand);
    }
  }
}
