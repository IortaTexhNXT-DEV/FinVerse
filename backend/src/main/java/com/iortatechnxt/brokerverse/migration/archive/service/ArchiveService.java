package com.iortatechnxt.brokerverse.migration.archive.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.migration.archive.domain.ArchiveRecord;
import com.iortatechnxt.brokerverse.migration.archive.domain.ArchiveRecordRepository;
import com.iortatechnxt.brokerverse.migration.archive.service.port.LegacyDocumentSource;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the legacy archive (DATA_MIGRATION_DESIGN section 16): the archive records of the closed
 * transactions and history (objects C04, P02, G02 and H01) and their documents (object H02),
 * attached to the record as type {@code LEGACY_DOCUMENT} after their size and SHA-256 are checked
 * against the document index. A rolled-back batch hides its records and removes their documents.
 */
@Service
@Transactional
public class ArchiveService {

  /** Document type of a legacy document. */
  public static final String DOCUMENT_TYPE = "LEGACY_DOCUMENT";

  private final ArchiveRecordRepository records;
  private final AttachmentService attachments;
  private final LegacyDocumentSource documents;

  /**
   * Creates the service.
   *
   * @param records archive records
   * @param attachments attachments
   * @param documents where the legacy documents arrive
   */
  public ArchiveService(
      ArchiveRecordRepository records,
      AttachmentService attachments,
      LegacyDocumentSource documents) {
    this.records = records;
    this.attachments = attachments;
    this.documents = documents;
  }

  /**
   * Stores an archive record; a record of the same key left by a rolled-back batch is replaced.
   *
   * @param companyId company
   * @param keys keys
   * @param facts facts
   * @param load batch, row hash, user and time
   * @return the record
   */
  public ArchiveRecord record(
      Long companyId, ArchiveRecord.Keys keys, ArchiveRecord.Facts facts, ArchiveRecord.Load load) {
    Optional<ArchiveRecord> existing =
        records.findByCompanyIdAndSourceSystemAndRecordTypeAndLegacyKey(
            companyId, keys.sourceSystem(), keys.recordType(), keys.legacyKey());
    if (existing.isPresent()) {
      if (!existing.get().isRolledBack()) {
        throw new BusinessRuleException(
            "MIG_ARCHIVE_EXISTS",
            "Archive record " + keys.recordType() + " " + keys.legacyKey() + " is already loaded");
      }
      records.delete(existing.get());
      records.flush();
    }
    return records.save(new ArchiveRecord(companyId, keys, facts, load));
  }

  /**
   * Attaches a legacy document to its archive record.
   *
   * @param companyId company
   * @param document the document index row
   * @return the attachment
   */
  public Attachment attach(Long companyId, IndexedDocument document) {
    ArchiveRecord record =
        records
            .findByCompanyIdAndSourceSystemAndRecordTypeAndLegacyKey(
                companyId, document.sourceSystem(), document.recordType(), document.legacyKey())
            .filter(r -> !r.isRolledBack())
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "MIG_ARCHIVE_RECORD_MISSING",
                        "Archive record "
                            + document.recordType()
                            + " "
                            + document.legacyKey()
                            + " is not loaded"));
    byte[] content =
        documents
            .read(companyId, document.sourceSystem(), document.fileName())
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "MIG_DOCUMENT_MISSING",
                        "File " + document.fileName() + " is not in the transfer folder"));
    if (content.length != document.sizeBytes()) {
      throw new BusinessRuleException(
          "MIG_DOCUMENT_SIZE",
          "File "
              + document.fileName()
              + " has "
              + content.length
              + " bytes; the index says "
              + document.sizeBytes());
    }
    String sha = Sha256.hex(content);
    boolean same =
        MessageDigest.isEqual(
            sha.getBytes(StandardCharsets.US_ASCII),
            document.sha256().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    if (!same) {
      throw new BusinessRuleException(
          "MIG_DOCUMENT_CHECKSUM",
          "The SHA-256 of file " + document.fileName() + " does not match the index");
    }
    Attachment saved =
        attachments.upload(
            new AttachmentTarget(MigrationCodes.ENTITY_ARCHIVE, String.valueOf(record.getId())),
            document.fileName(),
            content,
            document.description(),
            DOCUMENT_TYPE);
    record.documentAdded();
    return saved;
  }

  /**
   * Hides an archive record of a rolled-back batch.
   *
   * @param id record
   */
  public void rollBackRecord(Long id) {
    ArchiveRecord record =
        records
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(MigrationCodes.ENTITY_ARCHIVE, id));
    record.rollBack();
  }

  /**
   * Removes a legacy document of a rolled-back batch.
   *
   * @param attachmentId attachment
   */
  public void removeDocument(Long attachmentId) {
    Attachment attachment = attachments.get(attachmentId);
    attachments.delete(attachmentId);
    records
        .findById(Long.valueOf(attachment.getEntityId()))
        .ifPresent(ArchiveRecord::documentRemoved);
  }

  /**
   * A row of the document index.
   *
   * @param sourceSystem legacy system
   * @param recordType record type of the archive record
   * @param legacyKey key of the archive record
   * @param fileName file name in the transfer folder
   * @param sizeBytes declared size
   * @param sha256 declared SHA-256
   * @param description document type, number and date
   */
  public record IndexedDocument(
      String sourceSystem,
      String recordType,
      String legacyKey,
      String fileName,
      long sizeBytes,
      String sha256,
      String description) {}
}
