package com.iortatechnxt.brokerverse.common.util;

/**
 * Case-insensitive comparison of ASCII identifiers (user names, environment kinds, parameter codes)
 * without Unicode case mapping: only the letters A to Z and a to z are folded, so no other
 * character can ever compare equal to one of them (no dotless i, no Kelvin sign).
 */
public final class AsciiCase {

  private static final int CASE_OFFSET = 'a' - 'A';

  private AsciiCase() {}

  /**
   * Whether two texts are equal, ignoring the case of ASCII letters only.
   *
   * @param a first text, may be null
   * @param b second text, may be null
   * @return true when both are null or equal up to ASCII case
   */
  public static boolean equalsIgnoreCase(String a, String b) {
    if (a == null || b == null) {
      return a == null && b == null;
    }
    if (a.length() != b.length()) {
      return false;
    }
    for (int i = 0; i < a.length(); i++) {
      if (lower(a.charAt(i)) != lower(b.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * The text with the ASCII letters in upper case; other characters unchanged.
   *
   * @param text text
   * @return upper-case text
   */
  public static String upper(String text) {
    StringBuilder out = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      out.append(c >= 'a' && c <= 'z' ? (char) (c - CASE_OFFSET) : c);
    }
    return out.toString();
  }

  private static char lower(char c) {
    return c >= 'A' && c <= 'Z' ? (char) (c + CASE_OFFSET) : c;
  }
}
