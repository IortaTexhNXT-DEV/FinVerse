package com.iortatechnxt.brokerverse.docgen.service;

import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The texts every generated document prints around its content, the same in PDF and Word: the
 * reference line under the title and the business footer of every page.
 */
public final class DocumentText {

  private static final String FOOTER_SEPARATOR = "  |  ";

  /** A template code with its version, e.g. "PLACEMENT_SLIP v1" (never shown to users as such). */
  private static final Pattern TEMPLATE_TAG =
      Pattern.compile("\\b[A-Z][A-Z0-9_]{2,60} v(\\d{1,4})\\b");

  private DocumentText() {}

  /**
   * The line under the title: "Reference: PS-2026-000001 Date: 27-Sep-2026".
   *
   * @param spec document
   * @param date document date
   * @return text
   */
  public static String referenceLine(DocumentSpec spec, LocalDate date) {
    return "Reference: " + spec.reference() + "    Date: " + DisplayFormat.date(date);
  }

  /**
   * The business footer printed on every page before "Page x of y": the document title, the company
   * and the document's small print, where a template tag ("PLACEMENT_SLIP v1") is written as its
   * version ("Version 1"). The "Confidential" classification is added in front by the footer
   * itself.
   *
   * @param spec document
   * @return footer text
   */
  public static String pageFooter(DocumentSpec spec) {
    List<String> parts = new ArrayList<>();
    parts.add(titleCase(spec.title()));
    parts.add(spec.companyName());
    String note =
        spec.footer() == null ? "" : TEMPLATE_TAG.matcher(spec.footer()).replaceAll("Version $1");
    if (!note.isBlank()) {
      parts.add(note.strip());
    }
    return parts.stream()
        .filter(p -> p != null && !p.isBlank())
        .collect(Collectors.joining(FOOTER_SEPARATOR));
  }

  /**
   * The document title as the small print writes it: a title printed in capitals on the page (e.g.
   * SERVICE INVOICE) in title case (Service Invoice), like every other document footer.
   */
  private static String titleCase(String title) {
    if (title == null || title.chars().anyMatch(Character::isLowerCase)) {
      return title;
    }
    StringBuilder out = new StringBuilder(title.length());
    boolean wordStart = true;
    for (char c : title.toCharArray()) {
      out.append(wordStart ? c : Character.toLowerCase(c));
      wordStart = c == ' ';
    }
    return out.toString();
  }
}
