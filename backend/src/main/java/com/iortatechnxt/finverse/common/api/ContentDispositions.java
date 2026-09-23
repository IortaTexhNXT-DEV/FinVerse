package com.iortatechnxt.finverse.common.api;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.regex.Pattern;
import org.springframework.http.ContentDisposition;

/**
 * {@code Content-Disposition} header values for file downloads.
 *
 * <p>Every download carries an ASCII {@code filename} plus the exact name as RFC 5987 {@code
 * filename*} (UTF-8, percent encoded). Spring's {@code filename(name, UTF_8)} would instead put an
 * RFC 2047 encoded word ({@code =?UTF-8?Q?...?=}) into {@code filename}, which browsers and the
 * FinVerse client do not decode, so the downloaded file got a garbled name.
 */
public final class ContentDispositions {

  private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
  private static final Pattern NOT_PRINTABLE_ASCII = Pattern.compile("[^\\x20-\\x7E]");
  private static final String ATTR_CHAR_SYMBOLS = "!#$&+-.^_`|~";
  private static final HexFormat HEX = HexFormat.of().withUpperCase();
  private static final String DEFAULT_NAME = "download";
  private static final int BYTE_MASK = 0xFF;
  private static final int ASCII_LIMIT = 0x80;

  private ContentDispositions() {}

  /**
   * Header value for an attachment download.
   *
   * @param fileName file name as stored or generated (any characters)
   * @return e.g. {@code attachment; filename="Resume.pdf"; filename*=UTF-8''R%C3%A9sum%C3%A9.pdf}
   */
  public static String attachment(String fileName) {
    String name = fileName == null || fileName.isBlank() ? DEFAULT_NAME : fileName.strip();
    return ContentDisposition.attachment().filename(asciiFallback(name)).build()
        + "; filename*=UTF-8''"
        + rfc5987(name);
  }

  /**
   * ASCII-only variant of a file name: accents are dropped ("é" becomes "e"), any other non-ASCII
   * or control character becomes an underscore.
   *
   * @param name file name
   * @return printable ASCII name
   */
  static String asciiFallback(String name) {
    String decomposed = Normalizer.normalize(name, Normalizer.Form.NFD);
    String withoutMarks = COMBINING_MARKS.matcher(decomposed).replaceAll("");
    return NOT_PRINTABLE_ASCII.matcher(withoutMarks).replaceAll("_");
  }

  /**
   * Percent encodes the UTF-8 bytes of a value except RFC 5987 {@code attr-char}s.
   *
   * @param value value
   * @return encoded value
   */
  static String rfc5987(String value) {
    StringBuilder out = new StringBuilder();
    for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
      char c = (char) (b & BYTE_MASK);
      if (isAttrChar(c)) {
        out.append(c);
      } else {
        out.append('%').append(HEX.toHexDigits(b));
      }
    }
    return out.toString();
  }

  private static boolean isAttrChar(char c) {
    if (c >= ASCII_LIMIT) {
      return false;
    }
    return Character.isLetterOrDigit(c) || ATTR_CHAR_SYMBOLS.indexOf(c) >= 0;
  }
}
