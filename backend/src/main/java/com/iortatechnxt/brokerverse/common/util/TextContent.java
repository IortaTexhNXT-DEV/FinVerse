package com.iortatechnxt.brokerverse.common.util;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Content checks of the text files BIBS accepts (CSV, TXT, EML): the whole file, not only its
 * start, must be text, i.e. hold no NUL byte and no control character other than tab, line feed,
 * vertical tab, form feed, carriage return, escape (ISO-2022 e-mail) and the end-of-file mark
 * (0x1A); a file read as data (bulk and journal uploads) must also be valid UTF-8 (an optional byte
 * order mark is dropped) and stay within {@value #MAX_TEXT_BYTES} bytes.
 */
public final class TextContent {

  /** Largest text file read as data (25 MiB, the upload limit of the application). */
  public static final long MAX_TEXT_BYTES = 25L * 1024 * 1024;

  private static final char BOM = '\uFEFF';
  private static final int SPACE = 0x20;
  private static final int TAB = 0x09;
  private static final int CARRIAGE_RETURN = 0x0D;
  private static final int ESCAPE = 0x1B;
  private static final int END_OF_FILE = 0x1A;

  private TextContent() {}

  /**
   * Tells whether the bytes are text: no NUL byte and no binary control character anywhere.
   *
   * @param content file bytes
   * @return true for text in any ASCII-compatible encoding
   */
  public static boolean isText(byte[] content) {
    for (byte b : content) {
      int c = b & 0xFF;
      if (c < SPACE && !allowedControl(c)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Decodes a text file read as data.
   *
   * @param content file bytes
   * @return the text without a leading byte order mark
   * @throws BusinessRuleException {@code FILE_TOO_LARGE} above {@link #MAX_TEXT_BYTES}, {@code
   *     FILE_NOT_TEXT} for binary content, {@code FILE_TEXT_ENCODING} when the bytes are not UTF-8
   */
  public static String utf8(byte[] content) {
    if (content.length > MAX_TEXT_BYTES) {
      throw new BusinessRuleException(
          "FILE_TOO_LARGE",
          "The text file exceeds the maximum size of " + MAX_TEXT_BYTES / (1024 * 1024) + " MB");
    }
    if (!isText(content)) {
      throw new BusinessRuleException(
          "FILE_NOT_TEXT", "The file holds binary data; upload a text (CSV or TXT) file");
    }
    String text;
    try {
      text =
          StandardCharsets.UTF_8
              .newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT)
              .decode(ByteBuffer.wrap(content))
              .toString();
    } catch (CharacterCodingException e) {
      throw new BusinessRuleException(
          "FILE_TEXT_ENCODING",
          "The file is not UTF-8 text; save it as \"CSV UTF-8\" or as UTF-8 text and upload it"
              + " again",
          e);
    }
    return !text.isEmpty() && text.charAt(0) == BOM ? text.substring(1) : text;
  }

  private static boolean allowedControl(int c) {
    return (c >= TAB && c <= CARRIAGE_RETURN) || c == ESCAPE || c == END_OF_FILE;
  }
}
