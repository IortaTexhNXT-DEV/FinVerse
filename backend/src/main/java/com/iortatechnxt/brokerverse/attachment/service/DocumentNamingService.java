package com.iortatechnxt.brokerverse.attachment.service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * File names of uploaded documents (BRNB.026 "inherit or nominate file name"): a file keeps its own
 * name (inherit) or is renamed with a named pattern ({@link NamingPattern}, nominate). The default
 * syntax is {@value #SYNTAX}, e.g. {@code ARN-2026-000123_IDF_1.pdf}; screening documents use
 * {@link NamingPattern#SCREENING}, e.g. {@code KYC-REVIEW_DELA-CRUZ-JUAN_20260915_VALID-ID_1.pdf}
 * (SNSRP-601). The general syntax is parked (Q23); these are the build defaults.
 */
@Component
public class DocumentNamingService {

  /** Nominated name syntax of the default pattern. */
  public static final String SYNTAX = "<REFERENCE>_<DOCTYPE>_<n>.<ext>";

  private static final Pattern UNSAFE = Pattern.compile("[^A-Za-z0-9-]");
  private static final Pattern REPEATED_DASH = Pattern.compile("-{2,}");
  private static final String GENERIC_TYPE = "DOC";
  private static final String SEPARATOR = "_";
  private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

  /**
   * The nominated name of a document with the default pattern.
   *
   * @param reference business reference of the record (ARN, client code...)
   * @param documentType document type code, null for a generic document
   * @param sequence number of this document among the record's documents of the type (1-based)
   * @param originalName uploaded file name (for its extension)
   * @return nominated name
   */
  public String nominate(String reference, String documentType, int sequence, String originalName) {
    return nominate(
        NamingPattern.DEFAULT, NamingFacts.of(reference, documentType, sequence, originalName));
  }

  /**
   * The nominated name of a document with a named pattern.
   *
   * @param pattern naming pattern
   * @param facts facts of the document
   * @return nominated name
   */
  public String nominate(NamingPattern pattern, NamingFacts facts) {
    String type = typeOf(facts.documentType());
    String tail = type + SEPARATOR + facts.sequence() + extensionOf(facts.originalName());
    return switch (pattern) {
      case DEFAULT -> clean(facts.reference()) + SEPARATOR + tail;
      case SCREENING ->
          clean(facts.formType())
              + SEPARATOR
              + cleanName(facts.clientName())
              + SEPARATOR
              + (facts.dateReceived() == null ? "NODATE" : DATE.format(facts.dateReceived()))
              + SEPARATOR
              + tail;
    };
  }

  private static String typeOf(String documentType) {
    return documentType == null || documentType.isBlank() ? GENERIC_TYPE : clean(documentType);
  }

  private static String clean(String value) {
    String upper = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
    String safe = UNSAFE.matcher(upper).replaceAll("-");
    return safe.isEmpty() ? GENERIC_TYPE : safe;
  }

  private static String cleanName(String value) {
    return REPEATED_DASH.matcher(clean(value)).replaceAll("-");
  }

  private static String extensionOf(String name) {
    if (name == null) {
      return "";
    }
    int dot = name.lastIndexOf('.');
    return dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
  }
}
