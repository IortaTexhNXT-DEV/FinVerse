package com.iortatechnxt.brokerverse.migration.archive.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The search criteria of the Legacy Inquiry.
 *
 * @param client client name (part) or legacy client number
 * @param policyNo policy or cover number
 * @param invoiceNo invoice number
 * @param receiptNo AR or OR number
 * @param claimNo claim number
 * @param recordType record type
 * @param sourceSystem legacy system
 * @param from document date from
 * @param to document date to
 */
public record ArchiveCriteria(
    String client,
    String policyNo,
    String invoiceNo,
    String receiptNo,
    String claimNo,
    String recordType,
    String sourceSystem,
    LocalDate from,
    LocalDate to) {

  /**
   * Whether no criterion is given.
   *
   * @return true when empty
   */
  public boolean empty() {
    return describe().isEmpty();
  }

  /**
   * The criteria as written to the access log.
   *
   * @return label=value pairs
   */
  public String describe() {
    List<String> parts = new ArrayList<>();
    add(parts, "client", client);
    add(parts, "policy", policyNo);
    add(parts, "invoice", invoiceNo);
    add(parts, "receipt", receiptNo);
    add(parts, "claim", claimNo);
    add(parts, "type", recordType);
    add(parts, "system", sourceSystem);
    add(parts, "from", from == null ? null : from.toString());
    add(parts, "to", to == null ? null : to.toString());
    return String.join("; ", parts);
  }

  private static void add(List<String> parts, String label, String value) {
    if (value != null && !value.isBlank()) {
      parts.add(label + "=" + value.strip());
    }
  }
}
