package com.iortatechnxt.brokerverse.common.util;

/**
 * Plausibility check of e-mail addresses without a backtracking regular expression (no ReDoS on
 * user-supplied text): exactly one {@code @} after a non-empty local part, a domain with a dot that
 * neither starts it nor ends it, and no whitespace or list separators.
 */
public final class EmailAddresses {

  /** Shortest top-level domain accepted after the last dot. */
  private static final int MIN_TLD = 2;

  /** Longest address accepted (RFC 5321 path limit). */
  private static final int MAX_LENGTH = 254;

  private EmailAddresses() {}

  /**
   * Whether a text is a plausible e-mail address.
   *
   * @param text trimmed text
   * @return true when it looks like an address
   */
  public static boolean isValid(String text) {
    if (text == null || text.isEmpty() || text.length() > MAX_LENGTH) {
      return false;
    }
    int at = text.indexOf('@');
    if (at <= 0 || at != text.lastIndexOf('@') || hasForbidden(text)) {
      return false;
    }
    String domain = text.substring(at + 1);
    int dot = domain.lastIndexOf('.');
    return dot > 0 && dot < domain.length() - MIN_TLD && !domain.startsWith(".");
  }

  private static boolean hasForbidden(String text) {
    return text.chars().anyMatch(c -> Character.isWhitespace(c) || c == ',' || c == ';');
  }
}
