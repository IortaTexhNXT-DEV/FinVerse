package com.iortatechnxt.brokerverse.issuance.service;

/**
 * Port that reads the policy data from a received e-policy (BRNB.074/104). The default {@link
 * PdfTextPolicyDataExtractor} reads the text layer of the PDF with the configurable patterns of the
 * insurer (table {@code iss_extraction_pattern}). OCR of scanned policies is parked (Q24): an OCR
 * implementation replaces the bean without changing the review screen, because every extraction is
 * reviewed by a user before the account is updated.
 */
public interface PolicyDataExtractor {

  /**
   * Reads an e-policy.
   *
   * @param pdf PDF bytes
   * @param insurerCode insurer of the account (selects its patterns), may be null
   * @return the values found; missing values are null and explained in the note
   */
  ExtractedPolicy extract(byte[] pdf, String insurerCode);
}
