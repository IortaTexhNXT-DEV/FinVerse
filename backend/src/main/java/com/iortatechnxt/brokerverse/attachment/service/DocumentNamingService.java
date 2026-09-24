package com.iortatechnxt.brokerverse.attachment.service;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * File names of uploaded documents (BRNB.026 "inherit or nominate file name"): a file keeps its own
 * name (inherit) or is renamed with the syntax {@value #SYNTAX} (nominate), e.g. {@code
 * ARN-2026-000123_IDF_1.pdf}. The syntax itself is parked (Q23); this is the build default.
 */
@Component
public class DocumentNamingService {

  /** Nominated name syntax. */
  public static final String SYNTAX = "<REFERENCE>_<DOCTYPE>_<n>.<ext>";

  private static final Pattern UNSAFE = Pattern.compile("[^A-Za-z0-9-]");
  private static final String GENERIC_TYPE = "DOC";

  /**
   * The nominated name of a document.
   *
   * @param reference business reference of the record (ARN, client code...)
   * @param documentType document type code, null for a generic document
   * @param sequence number of this document among the record's documents of the type (1-based)
   * @param originalName uploaded file name (for its extension)
   * @return nominated name
   */
  public String nominate(String reference, String documentType, int sequence, String originalName) {
    String ref = clean(reference);
    String type =
        documentType == null || documentType.isBlank() ? GENERIC_TYPE : clean(documentType);
    return ref + "_" + type + "_" + sequence + extensionOf(originalName);
  }

  private static String clean(String value) {
    String upper = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
    String safe = UNSAFE.matcher(upper).replaceAll("-");
    return safe.isEmpty() ? GENERIC_TYPE : safe;
  }

  private static String extensionOf(String name) {
    if (name == null) {
      return "";
    }
    int dot = name.lastIndexOf('.');
    return dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
  }
}
