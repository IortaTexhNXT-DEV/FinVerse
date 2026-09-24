package com.iortatechnxt.brokerverse.crm.api.dto;

import com.iortatechnxt.brokerverse.crm.service.KycChecklist;
import java.time.Instant;
import java.util.List;

/**
 * KYC checklist of a client (BRNB.090).
 *
 * @param complete whether every mandatory document is present
 * @param missing labels of missing mandatory documents
 * @param items document types
 */
public record KycChecklistResponse(boolean complete, List<String> missing, List<Item> items) {

  /**
   * Maps a checklist.
   *
   * @param c checklist
   * @return response
   */
  public static KycChecklistResponse from(KycChecklist c) {
    return new KycChecklistResponse(
        c.complete(),
        c.missing(),
        c.items().stream()
            .map(
                i ->
                    new Item(
                        i.documentType(),
                        i.label(),
                        i.required(),
                        i.documents().stream()
                            .map(
                                d ->
                                    new Document(
                                        d.attachmentId(),
                                        d.fileName(),
                                        d.uploadedBy(),
                                        d.uploadedAt()))
                            .toList()))
            .toList());
  }

  /**
   * A document type.
   *
   * @param documentType code
   * @param label label
   * @param required mandatory for the client type
   * @param documents uploaded documents
   */
  public record Item(
      String documentType, String label, boolean required, List<Document> documents) {}

  /**
   * An uploaded document.
   *
   * @param attachmentId attachment
   * @param fileName file name
   * @param uploadedBy user
   * @param uploadedAt time
   */
  public record Document(
      Long attachmentId, String fileName, String uploadedBy, Instant uploadedAt) {}
}
