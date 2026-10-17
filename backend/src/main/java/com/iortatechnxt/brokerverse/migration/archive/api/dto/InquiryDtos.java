package com.iortatechnxt.brokerverse.migration.archive.api.dto;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.migration.archive.domain.AccessLog;
import com.iortatechnxt.brokerverse.migration.archive.domain.ArchiveRecord;
import com.iortatechnxt.brokerverse.migration.archive.service.LegacyInquiryService.RecordView;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Payloads of the Legacy Inquiry and of the access log. */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of the payloads
public final class InquiryDtos {

  private InquiryDtos() {}

  /**
   * An archive record in the result list.
   *
   * @param id id
   * @param sourceSystem legacy system
   * @param recordType record type
   * @param legacyKey legacy key
   * @param clientKey legacy client number
   * @param clientName client name
   * @param policyNo policy or cover number
   * @param invoiceNo invoice number
   * @param receiptNo receipt number
   * @param claimNo claim number
   * @param documentDate document date
   * @param currency currency
   * @param amount amount
   * @param status legacy status
   * @param documentCount documents
   */
  public record RecordSummary(
      Long id,
      String sourceSystem,
      String recordType,
      String legacyKey,
      String clientKey,
      String clientName,
      String policyNo,
      String invoiceNo,
      String receiptNo,
      String claimNo,
      LocalDate documentDate,
      String currency,
      BigDecimal amount,
      String status,
      int documentCount) {

    /**
     * Maps a record.
     *
     * @param r record
     * @return summary
     */
    public static RecordSummary from(ArchiveRecord r) {
      return new RecordSummary(
          r.getId(),
          r.getSourceSystem(),
          r.getRecordType(),
          r.getLegacyKey(),
          r.getClientKey(),
          r.getClientName(),
          r.getPolicyNo(),
          r.getInvoiceNo(),
          r.getReceiptNo(),
          r.getClaimNo(),
          r.getDocumentDate(),
          r.getCurrency(),
          r.getAmount(),
          r.getStatus(),
          r.getDocumentCount());
    }
  }

  /**
   * An archive record with its legacy columns and documents.
   *
   * @param record summary
   * @param periodFrom period from
   * @param periodTo period to
   * @param details labelled legacy columns
   * @param loadedAt loaded at
   * @param documents legacy documents
   */
  public record RecordDetail(
      RecordSummary record,
      LocalDate periodFrom,
      LocalDate periodTo,
      Map<String, String> details,
      Instant loadedAt,
      List<DocumentResponse> documents) {

    /** Defensive copies. */
    public RecordDetail {
      details = Map.copyOf(details);
      documents = List.copyOf(documents);
    }

    /**
     * Maps a view.
     *
     * @param v view
     * @return detail
     */
    public static RecordDetail from(RecordView v) {
      ArchiveRecord r = v.record();
      return new RecordDetail(
          RecordSummary.from(r),
          r.getPeriodFrom(),
          r.getPeriodTo(),
          r.getSummary(),
          r.getCreatedAt(),
          v.documents().stream().map(DocumentResponse::from).toList());
    }
  }

  /**
   * A legacy document.
   *
   * @param id attachment id
   * @param fileName file name
   * @param description document type, number and date
   * @param sizeBytes size
   * @param sha256 checksum
   */
  public record DocumentResponse(
      Long id, String fileName, String description, long sizeBytes, String sha256) {

    /**
     * Maps an attachment.
     *
     * @param a attachment
     * @return document
     */
    public static DocumentResponse from(Attachment a) {
      return new DocumentResponse(
          a.getId(), a.getFileName(), a.getDescription(), a.getSizeBytes(), a.getSha256());
    }
  }

  /**
   * An access log entry.
   *
   * @param id id
   * @param username user
   * @param accessedAt time
   * @param sourceAddress source address
   * @param action action
   * @param criteria criteria
   * @param recordKeys record keys
   * @param resultCount result count
   * @param reasonCode reason
   * @param reasonText reason details
   */
  public record AccessLogResponse(
      Long id,
      String username,
      Instant accessedAt,
      String sourceAddress,
      String action,
      String criteria,
      String recordKeys,
      int resultCount,
      String reasonCode,
      String reasonText) {

    /**
     * Maps an entry.
     *
     * @param a entry
     * @return response
     */
    public static AccessLogResponse from(AccessLog a) {
      return new AccessLogResponse(
          a.getId(),
          a.getUsername(),
          a.getAccessedAt(),
          a.getSourceAddress(),
          a.getAction(),
          a.getCriteria(),
          a.getRecordKeys(),
          a.getResultCount(),
          a.getReasonCode(),
          a.getReasonText());
    }
  }

  /**
   * A legacy document staged for the document index.
   *
   * @param id stored file
   * @param fileName file name
   * @param sizeBytes size
   * @param sha256 checksum
   * @param stagedAt staged at
   */
  public record StagedDocument(
      Long id, String fileName, long sizeBytes, String sha256, Instant stagedAt) {

    /**
     * Maps a stored file.
     *
     * @param f file
     * @return staged document
     */
    public static StagedDocument from(StoredFile f) {
      return new StagedDocument(
          f.getId(), f.getFileName(), f.getSizeBytes(), f.getSha256(), f.getCreatedAt());
    }
  }
}
