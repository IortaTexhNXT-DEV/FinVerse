package com.iortatechnxt.brokerverse.attachment.service;

import java.time.LocalDate;

/**
 * The facts a nominated document name is built from; each {@link NamingPattern} uses some of them.
 *
 * @param reference business reference of the record (DEFAULT)
 * @param formType form type, e.g. KYC_REVIEW (SCREENING)
 * @param clientName client name (SCREENING)
 * @param dateReceived date the document was received (SCREENING)
 * @param documentType document type code, null for a generic document
 * @param sequence number of this document among the record's documents of the type (1-based)
 * @param originalName uploaded file name (for its extension)
 */
public record NamingFacts(
    String reference,
    String formType,
    String clientName,
    LocalDate dateReceived,
    String documentType,
    int sequence,
    String originalName) {

  /**
   * Facts of the default pattern.
   *
   * @param reference business reference
   * @param documentType document type code
   * @param sequence number of the document of the type (1-based)
   * @param originalName uploaded file name
   * @return facts
   */
  public static NamingFacts of(
      String reference, String documentType, int sequence, String originalName) {
    return new NamingFacts(reference, null, null, null, documentType, sequence, originalName);
  }
}
