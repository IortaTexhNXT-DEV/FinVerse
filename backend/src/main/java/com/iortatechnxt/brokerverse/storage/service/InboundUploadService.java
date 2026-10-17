package com.iortatechnxt.brokerverse.storage.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.storage.BucketClass;
import com.iortatechnxt.brokerverse.common.storage.FileStore;
import com.iortatechnxt.brokerverse.common.storage.ObjectKeys;
import com.iortatechnxt.brokerverse.common.storage.ObjectMetadata;
import com.iortatechnxt.brokerverse.common.storage.ObjectRef;
import com.iortatechnxt.brokerverse.common.storage.PresignedLink;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.domain.NewStoredFile;
import com.iortatechnxt.brokerverse.storage.domain.RecordClass;
import com.iortatechnxt.brokerverse.storage.domain.ScanStatus;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import com.iortatechnxt.brokerverse.storage.domain.StoredFileRepository;
import java.time.Clock;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Large bulk files (bank and insurer files, watchlist feeds, bulk uploads) bypass the application:
 * the client receives a presigned PUT into the {@code incoming/} prefix of the inbound bucket,
 * uploads directly, then confirms. The object is accepted only after its malware scan is clean
 * (DOCUMENT_STORAGE_DECISION section 3 "Uploads" and decision 2).
 */
@Service
@Transactional
public class InboundUploadService {

  /** Largest single PUT (S3 limit, 5 GB). */
  static final long MAX_INBOUND_BYTES = 5L * 1024 * 1024 * 1024;

  private static final String ENTITY = StoredFileService.ENTITY;
  private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

  private final StoredFileRepository files;
  private final StoredFileService storedFiles;
  private final FileLinkService links;
  private final FileScanService scans;
  private final RecordClassService recordClasses;
  private final UploadRules rules;
  private final FileStore store;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param files metadata
   * @param storedFiles stored files
   * @param links link validity
   * @param scans scan results
   * @param recordClasses record classes
   * @param rules upload rules
   * @param store object store
   * @param audit audit trail
   * @param clock clock
   */
  public InboundUploadService(
      StoredFileRepository files,
      StoredFileService storedFiles,
      FileLinkService links,
      FileScanService scans,
      RecordClassService recordClasses,
      UploadRules rules,
      FileStore store,
      AuditTrailService audit,
      Clock clock) {
    this.files = files;
    this.storedFiles = storedFiles;
    this.links = links;
    this.scans = scans;
    this.recordClasses = recordClasses;
    this.rules = rules;
    this.store = store;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Records an announced inbound file and returns the upload link. The caller has checked the
   * owner's permission.
   *
   * @param request the announced file
   * @return file row ({@code AWAITING_UPLOAD}) and the presigned PUT
   */
  public StartedUpload start(InboundRequest request) {
    FileOwner owner = request.owner();
    if (owner == null || !owner.isValid()) {
      throw new BusinessRuleException("INVALID_FILE_OWNER", "Invalid owner entity type or id");
    }
    RecordClass recordClass = recordClasses.requireActive(request.recordClass());
    if (recordClass.getBucketClass() != BucketClass.INBOUND) {
      throw new BusinessRuleException(
          "RECORD_CLASS_NOT_INBOUND", "Direct uploads are for inbound record classes only");
    }
    rules.requireSize(request.sizeBytes(), MAX_INBOUND_BYTES);
    String fileName = UploadRules.sanitize(request.fileName());
    String contentType = rules.contentTypeOf(fileName);
    String sha256 = request.sha256() == null ? "" : request.sha256().toLowerCase(Locale.ROOT);
    if (!SHA256.matcher(sha256).matches()) {
      throw new BusinessRuleException(
          "FILE_CHECKSUM_REQUIRED", "Declare the SHA-256 checksum of the file (64 hex digits)");
    }
    ObjectRef ref =
        new ObjectRef(
                BucketClass.INBOUND,
                ObjectKeys.newKey(
                    storedFiles.companyCode(owner),
                    owner.entityType(),
                    clock.instant(),
                    UUID.randomUUID()))
            .withPrefix(ObjectKeys.INCOMING_PREFIX);
    StoredFile file =
        files.save(
            new StoredFile(
                new NewStoredFile(
                    owner,
                    request.documentType(),
                    recordClass,
                    fileName,
                    contentType,
                    request.sizeBytes(),
                    sha256,
                    storedFiles.bucketName(ref),
                    ref,
                    null,
                    ScanStatus.AWAITING_UPLOAD,
                    recordClasses.retentionUntil(recordClass, storedFiles.today()))));
    PresignedLink link = store.presignedPut(ref, links.ttl(), contentType, sha256);
    audit.record(
        ENTITY,
        file.getId(),
        AuditAction.CREATE,
        "Upload link issued: "
            + FileScanService.describe(file)
            + ", "
            + request.sizeBytes()
            + " bytes");
    return new StartedUpload(file, link);
  }

  /**
   * Confirms the upload: the object must be present with the announced size. The file then waits
   * for its scan result.
   *
   * @param id stored file id
   * @return metadata
   */
  public StoredFile complete(Long id) {
    StoredFile file = storedFiles.get(id);
    ObjectMetadata metadata =
        store
            .metadata(file.objectRef())
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "FILE_NOT_UPLOADED", "The file has not been uploaded yet"));
    if (metadata.size() != file.getSizeBytes()) {
      throw new BusinessRuleException(
          "FILE_SIZE_MISMATCH", "The uploaded file does not have the announced size");
    }
    file.uploaded(metadata.versionId());
    audit.record(
        ENTITY, id, AuditAction.UPDATE, "Upload completed: " + FileScanService.describe(file));
    scans.evaluate(file);
    return file;
  }

  /**
   * An announced inbound file.
   *
   * @param owner owning record
   * @param documentType document type (optional)
   * @param recordClass record class (bucket class INBOUND)
   * @param fileName file name
   * @param sizeBytes size
   * @param sha256 SHA-256 the upload must match (hex)
   */
  public record InboundRequest(
      FileOwner owner,
      String documentType,
      String recordClass,
      String fileName,
      long sizeBytes,
      String sha256) {}

  /**
   * A started upload.
   *
   * @param file file row
   * @param link presigned PUT
   */
  public record StartedUpload(StoredFile file, PresignedLink link) {}
}
