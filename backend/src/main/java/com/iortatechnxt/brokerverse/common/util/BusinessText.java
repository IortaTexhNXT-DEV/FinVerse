package com.iortatechnxt.brokerverse.common.util;

import java.util.regex.Pattern;

/**
 * Business-user wording of texts shown on screens and in exported reports (client feedback,
 * 26-Sep-2026): requirement and traceability references such as "(ADJID.021)", "(BRNB.110)",
 * "(CSHID.023 Annex II #1)" or "(OQ42)", and design notes such as "layout to confirm", are removed.
 * The references stay in the code and its comments for traceability.
 */
public final class BusinessText {

  private static final String REF =
      "(?:[A-Z]{2,6}ID\\.\\d+(?:[-/]\\d+)*(?: addendum)?(?: Annex II #\\d+)?"
          + "|BR[A-Z]{2,4}\\.\\d+(?:[-/]\\d+)*"
          + "|FRBS \\d+\\.\\d+(?:\\.\\d+|\\.x)?"
          + "|Annex II #\\d+"
          + "|OQ\\d+(?:/OQ\\d+)*|[A-Z]{1,3}Q\\d{2}|Q\\d{2}"
          + "|FR-[A-Z]{2}-?\\d+|SNSRP-\\d+"
          + "|layout to confirm|draft|to confirm)";

  /** A parenthesis holding only references and design notes, with the space before it. */
  private static final Pattern REFERENCES =
      Pattern.compile("\\s*\\(" + REF + "(?:\\s*(?:,|;|/|and)\\s*" + REF + ")*\\)");

  /** A reference left outside a parenthesis, e.g. "(Aging report, ADJID.021)". */
  private static final Pattern INLINE =
      Pattern.compile("(?:[,;]\\s*|\\s+)" + REF.replace("|draft|to confirm", "") + "(?=[);.,]|$)");

  /** What is left of a parenthesis after its references are removed: "(Aging; )" or "()". */
  private static final Pattern EMPTY_PARENS = Pattern.compile("\\s*\\(\\s*[,;/]?\\s*\\)");

  private static final Pattern TRAILING_SEPARATOR = Pattern.compile("\\s*[,;]\\s*\\)");

  /** Patterns that must never appear in a business text (used by the tests). */
  public static final Pattern FORBIDDEN =
      Pattern.compile(
          "\\b[A-Z]{2,6}ID\\.\\d|\\bBR[A-Z]{2,4}\\.\\d|\\bAnnex II\\b|\\bOQ\\d+\\b"
              + "|\\blayout to confirm\\b");

  private BusinessText() {}

  /**
   * Removes requirement references and design notes from a text shown to business users.
   *
   * @param text text (may be null)
   * @return cleaned text, or null when the text is null
   */
  public static String clean(String text) {
    if (text == null || text.isEmpty()) {
      return text;
    }
    String out = REFERENCES.matcher(text).replaceAll("");
    out = INLINE.matcher(out).replaceAll("");
    out = TRAILING_SEPARATOR.matcher(out).replaceAll(")");
    out = EMPTY_PARENS.matcher(out).replaceAll("");
    return out.replaceAll("\\s{2,}", " ").replaceAll("\\s+([.,;:])", "$1").trim();
  }
}
