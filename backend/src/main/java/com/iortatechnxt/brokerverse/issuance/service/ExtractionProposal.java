package com.iortatechnxt.brokerverse.issuance.service;

import java.util.Map;

/**
 * The fields proposed from a document (BRIDSP-02). Nothing is written anywhere before a user
 * confirms the proposal.
 *
 * @param fields proposed values by field name ({@code ExtractionField} names)
 * @param readable false when no text could be read (scanned document without OCR)
 * @param note what is missing or why nothing was read, may be null
 */
public record ExtractionProposal(
    Map<String, ExtractedValue> fields, boolean readable, String note) {

  /** Defensive copy. */
  public ExtractionProposal {
    fields = fields == null ? Map.of() : Map.copyOf(fields);
  }

  /**
   * A document without readable text.
   *
   * @return empty proposal
   */
  public static ExtractionProposal notReadable() {
    return new ExtractionProposal(
        Map.of(), false, "The document has no readable text; enter the fields by hand");
  }
}
