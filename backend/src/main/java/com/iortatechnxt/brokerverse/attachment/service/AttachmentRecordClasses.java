package com.iortatechnxt.brokerverse.attachment.service;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * The record class ({@code sto_record_class}) of an attachment by its document type
 * (DOCUMENT_STORAGE_DECISION section 7): issued policy documents, official receipts and claim
 * settlement papers keep their own retention, legal hold and ECM archiving; every other document is
 * a general document.
 */
public final class AttachmentRecordClasses {

  /** Record class of documents without a specific class. */
  public static final String GENERAL = "GENERAL_DOCUMENT";

  private static final String POLICY = "POLICY_DOCUMENT";
  private static final String SETTLEMENT = "CLAIM_SETTLEMENT";

  private static final Map<String, String> BY_TYPE =
      new TreeMap<>(
          Map.of(
              "EPOLICY", POLICY,
              "POLICY_COPY", POLICY,
              "INSURANCE_ADVICE", POLICY,
              "EB_POLICY_FORM", POLICY,
              "OFFICIAL_RECEIPT", "OFFICIAL_RECEIPT",
              "CLAIM_SETTLEMENT", SETTLEMENT,
              "RELEASE_PAPERS", SETTLEMENT,
              "SETTLED_RELEASE_PAPERS", SETTLEMENT,
              "INSURER_RELEASE_PAPERS", SETTLEMENT));

  private AttachmentRecordClasses() {}

  /**
   * The record class of a document type.
   *
   * @param documentType document type, may be null
   * @return record class code
   */
  public static String of(String documentType) {
    return documentType == null ? GENERAL : BY_TYPE.getOrDefault(documentType, GENERAL);
  }

  /**
   * The same mapping as an SQL expression (copy of the files kept before ST1).
   *
   * @param column document type column
   * @return {@code case} expression
   */
  static String sqlCase(String column) {
    return BY_TYPE.entrySet().stream()
        .map(e -> "when '" + e.getKey() + "' then '" + e.getValue() + "'")
        .collect(Collectors.joining(" ", "case " + column + " ", " else '" + GENERAL + "' end"));
  }
}
