package com.iortatechnxt.brokerverse.common.util;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Business-user wording of texts shown on screens and in exported reports (client feedback,
 * 26-Sep-2026): requirement and traceability references such as "(ADJID.021)", "(BRNB.110)",
 * "(CSHID.023 Annex II #1)" or "(OQ42)", and design notes such as "layout to confirm", are removed.
 * The references stay in the code and its comments for traceability.
 */
public final class BusinessText {

  /** One reference inside a parenthesis: "ADJID.021", "BRCLXN.001-012", "045", "PMADD06". */
  private static final List<Pattern> REFERENCE_PARTS =
      List.of(
          Pattern.compile(
              "[A-Z]{2,6}ID\\.\\d+[a-z]?(?:[-/]\\d+[a-z]?)*(?: addendum)?(?: Annex II #\\d+)?"),
          Pattern.compile("BR[A-Z]{2,4}\\.\\d+[a-z]?(?:[-/]\\d+[a-z]?)*"),
          Pattern.compile("FRBS \\d+\\.\\d+(?:\\.\\d+|\\.x)?"),
          Pattern.compile("Annex II #\\d+"),
          Pattern.compile("OQ\\d+(?:/OQ\\d+)*"),
          Pattern.compile("[A-Z]{0,3}Q\\d{2}"),
          Pattern.compile("[A-Z]{2,4}ADD\\d{2}"),
          Pattern.compile("FR-[A-Z]{2}-?\\d+"),
          Pattern.compile("SNSRP-\\d+"),
          Pattern.compile("(?:[A-Z][a-z]+ )?summary \\d+\\.[a-z0-9]+"),
          Pattern.compile("\\d{3}[a-z]?(?:[-/]\\d{3}[a-z]?)*"));

  /** Design notes that may stand next to references inside a parenthesis. */
  private static final Pattern NOTE_PART =
      Pattern.compile("layout to confirm|draft|to confirm", Pattern.CASE_INSENSITIVE);

  private static final Pattern PARENTHESIS = Pattern.compile(" ?\\(([^()]*)\\)");

  private static final Pattern SEPARATORS = Pattern.compile("[,;]| and ");

  /** A reference left outside a parenthesis, e.g. "(Aging report, ADJID.021)". */
  private static final Pattern INLINE =
      Pattern.compile(
          "[,;] ?(?:[A-Z]{2,6}ID\\.\\d+(?:[-/]\\d+)*|BR[A-Z]{2,4}\\.\\d+(?:[-/]\\d+)*|OQ\\d+)"
              + "(?=[);.,]|$)");

  /** Patterns that must never appear in a business text (used by the tests). */
  public static final Pattern FORBIDDEN =
      Pattern.compile(
          "\\b[A-Z]{2,6}ID\\.\\d|\\bBR[A-Z]{2,4}\\.\\d|\\bAnnex II\\b|\\bOQ\\d+\\b"
              + "|\\blayout to confirm\\b");

  /** A footnote that is only a design note ("Draft layout, to be confirmed with FRBS"). */
  private static final Pattern DESIGN_NOTE =
      Pattern.compile(
          "^\\s*(draft layout|layout to confirm)|\\bto be confirmed (with|by)\\b",
          Pattern.CASE_INSENSITIVE);

  private BusinessText() {}

  /**
   * Whether a text is only a design note, which is left out of what business users see.
   *
   * @param text text (may be null)
   * @return true for a design note
   */
  public static boolean isDesignNote(String text) {
    return text != null && DESIGN_NOTE.matcher(text).find();
  }

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
    Matcher m = PARENTHESIS.matcher(text);
    StringBuilder out = new StringBuilder();
    while (m.find()) {
      m.appendReplacement(
          out, onlyReferences(m.group(1)) ? "" : Matcher.quoteReplacement(m.group()));
    }
    m.appendTail(out);
    String cleaned = INLINE.matcher(out.toString()).replaceAll("");
    return cleaned.replaceAll("\\s{2,}", " ").replaceAll("\\s+([.,;:])", "$1").trim();
  }

  /** Whether the text of a parenthesis holds only references and design notes. */
  private static boolean onlyReferences(String inside) {
    List<String> parts =
        Arrays.stream(SEPARATORS.split(inside))
            .map(String::trim)
            .filter(p -> !p.isEmpty())
            .toList();
    boolean anyReference = parts.stream().anyMatch(BusinessText::isReference);
    return anyReference
        && parts.stream().allMatch(p -> isReference(p) || NOTE_PART.matcher(p).matches());
  }

  private static boolean isReference(String part) {
    return REFERENCE_PARTS.stream().anyMatch(p -> p.matcher(part).matches());
  }
}
