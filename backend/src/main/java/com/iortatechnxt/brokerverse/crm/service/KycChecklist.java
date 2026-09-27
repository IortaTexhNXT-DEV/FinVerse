package com.iortatechnxt.brokerverse.crm.service;

import java.time.Instant;
import java.util.List;

/**
 * KYC checklist of a client (BRNB.090): the mandatory document types of its client type (lists of
 * values KYC_DOCS_INDIVIDUAL / KYC_DOCS_CORPORATE) and the documents uploaded.
 *
 * @param items one line per mandatory or uploaded document type
 * @param complete whether every mandatory document is present
 */
public record KycChecklist(List<Item> items, boolean complete) {

  /** Defensive copy. */
  public KycChecklist {
    items = List.copyOf(items);
  }

  /**
   * Labels of the missing mandatory documents.
   *
   * @return labels
   */
  public List<String> missing() {
    return items.stream()
        .filter(i -> i.required() && i.documents().isEmpty())
        .map(Item::label)
        .toList();
  }

  /**
   * One document type of the checklist.
   *
   * @param documentType document type code
   * @param label document type label
   * @param required whether it is mandatory for the client type
   * @param documents uploaded documents of the type
   */
  public record Item(
      String documentType, String label, boolean required, List<Document> documents) {

    /** Defensive copy. */
    public Item {
      documents = List.copyOf(documents);
    }
  }

  /**
   * An uploaded KYC document.
   *
   * @param attachmentId attachment (download link)
   * @param fileName file name
   * @param uploadedBy user
   * @param uploadedAt time
   */
  public record Document(
      Long attachmentId, String fileName, String uploadedBy, Instant uploadedAt) {}
}
