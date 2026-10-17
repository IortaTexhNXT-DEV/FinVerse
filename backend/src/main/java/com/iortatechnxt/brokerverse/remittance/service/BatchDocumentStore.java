package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.remittance.domain.BatchDocument;
import com.iortatechnxt.brokerverse.remittance.domain.BatchDocument.StoredFile;
import com.iortatechnxt.brokerverse.remittance.domain.BatchDocumentRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.DocumentKind;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The documents stored on a remittance batch at submission (RMTID.011): rendered by {@link
 * BatchDocuments} and kept in the file store (owner type {@value #OWNER_TYPE}, the document row;
 * record class {@code GENERAL_DOCUMENT}; build step ST1). Before submission a document is rendered
 * on request.
 */
@Component
public class BatchDocumentStore {

  /** Owner entity type of the stored batch documents. */
  public static final String OWNER_TYPE = "RemittanceBatchDocument";

  /** Record class of the batch documents. */
  public static final String RECORD_CLASS = "GENERAL_DOCUMENT";

  private final BatchDocuments renderer;
  private final BatchDocumentRepository documents;
  private final StoredFileService storedFiles;

  /**
   * Creates the store.
   *
   * @param renderer document rendering
   * @param documents stored documents
   * @param storedFiles file store
   */
  public BatchDocumentStore(
      BatchDocuments renderer, BatchDocumentRepository documents, StoredFileService storedFiles) {
    this.renderer = renderer;
    this.documents = documents;
    this.storedFiles = storedFiles;
  }

  /**
   * A document of a batch: the stored one after submission, else rendered now.
   *
   * @param batch batch with lines
   * @param kind kind
   * @return file
   */
  public StoredFile document(RemittanceBatch batch, DocumentKind kind) {
    return documents
        .findByBatchIdAndKind(batch.getId(), kind)
        .map(
            d ->
                new StoredFile(
                    d.getFileName(), d.getContentType(), content(d), d.getTemplateVersion()))
        .orElseGet(() -> renderer.render(batch, kind));
  }

  /**
   * A document of a batch for the download endpoint: a presigned link to the stored document after
   * submission, else the document rendered now (or the bytes of a document stored before ST1).
   *
   * @param batch batch with lines
   * @param kind kind
   * @return download
   */
  public FileDownload download(RemittanceBatch batch, DocumentKind kind) {
    Optional<BatchDocument> stored = documents.findByBatchIdAndKind(batch.getId(), kind);
    if (stored.isPresent() && stored.get().getStoredFileId() != null) {
      return FileDownload.stored(stored.get().getStoredFileId());
    }
    StoredFile file = document(batch, kind);
    return FileDownload.inline(file.fileName(), file.contentType(), file.content());
  }

  private byte[] content(BatchDocument document) {
    return document.getStoredFileId() == null
        ? document.getContent()
        : storedFiles.read(document.getStoredFileId());
  }

  /**
   * Renders and stores the three documents on the batch (submission, RMTID.011).
   *
   * @param batch batch with lines
   */
  public void store(RemittanceBatch batch) {
    for (DocumentKind kind : DocumentKind.values()) {
      StoredFile file = renderer.render(batch, kind);
      Optional<BatchDocument> existing = documents.findByBatchIdAndKind(batch.getId(), kind);
      BatchDocument document;
      if (existing.isPresent()) {
        document = existing.get();
        Long previous = document.getStoredFileId();
        document.replace(file);
        if (previous != null) {
          storedFiles.delete(previous);
        }
      } else {
        document = documents.save(new BatchDocument(batch.getId(), kind, file));
      }
      document.storedIn(storeFile(batch, document, file));
    }
  }

  private Long storeFile(RemittanceBatch batch, BatchDocument document, StoredFile file) {
    return storedFiles
        .storeChecked(
            new StoreRequest(
                new FileOwner(batch.getCompanyId(), OWNER_TYPE, String.valueOf(document.getId())),
                document.getKind().name(),
                RECORD_CLASS,
                file.fileName(),
                file.content(),
                null),
            file.contentType(),
            FileOrigin.GENERATED)
        .getId();
  }
}
