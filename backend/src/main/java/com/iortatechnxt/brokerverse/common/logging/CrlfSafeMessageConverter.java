package com.iortatechnxt.brokerverse.common.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Log message of every pattern layout ({@code %m}, {@code %msg}, {@code %message}) with line breaks
 * neutralised: CR, LF, NEL and the Unicode line and paragraph separators become {@code _}, so data
 * written into a message (a user name, a file name, a request value) can never start a forged log
 * line.
 *
 * <p>Registered for the three conversion words in {@code logback-spring.xml}, so it applies to the
 * console appender and to any file appender added there, whatever pattern a deployment sets in
 * {@code logging.pattern.*}. Structured (JSON) formats escape line breaks inside their string
 * values by themselves.
 */
public class CrlfSafeMessageConverter extends MessageConverter {

  private static final char REPLACEMENT = '_';
  private static final char NEXT_LINE = (char) 0x85;
  private static final char LINE_SEPARATOR = (char) 0x2028;
  private static final char PARAGRAPH_SEPARATOR = (char) 0x2029;

  @Override
  public String convert(ILoggingEvent event) {
    return neutralise(super.convert(event));
  }

  /**
   * Replaces every line break of a text.
   *
   * @param text text, may be null
   * @return the text on one line
   */
  public static String neutralise(String text) {
    if (text == null) {
      return null;
    }
    StringBuilder out = null;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (isLineBreak(c)) {
        if (out == null) {
          out = new StringBuilder(text);
        }
        out.setCharAt(i, REPLACEMENT);
      }
    }
    return out == null ? text : out.toString();
  }

  private static boolean isLineBreak(char c) {
    return c == '\r'
        || c == '\n'
        || c == NEXT_LINE
        || c == LINE_SEPARATOR
        || c == PARAGRAPH_SEPARATOR;
  }
}
