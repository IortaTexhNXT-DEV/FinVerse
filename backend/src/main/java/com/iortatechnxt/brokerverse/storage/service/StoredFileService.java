package com.iortatechnxt.brokerverse.storage.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.storage.FileStore;
import com.iortatechnxt.brokerverse.common.storage.FileStoreException;
import com.iortatechnxt.brokerverse.common.storage.ObjectKeys;
import com.iortatechnxt.brokerverse.common.storage.ObjectRef;
import com.iortatechnxt.brokerverse.common.storage.StorageProperties;
import com.iortatechnxt.brokerverse.common.storage.StoredObject;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.organization.service.OrganizationDirectory;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.domain.NewStoredFile;
import com.iortatechnxt.brokerverse.storage.domain.RecordClass;
import com.iortatechnxt.brokerverse.storage.domain.ScanStatus;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import com.iortatechnxt.brokerverse.storage.domain.StoredFileRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores and reads files through the {@link FileStore} port with their metadata in {@code
 * stored_file} (DOCUMENT_STORAGE_DECISION section 3). The entry point of every module that keeps
 * files: new code never adds a {@code bytea} column (developer guide, "File storage").
 *
 * <p>The object is written first, then the row is saved: if the transaction fails the object is an
 * orphan, which {@code FILE_ORPHAN_RECONCILIATION} deletes after 24 hours. Deletes are soft; the
 * {@code FILE_RETENTION} job removes the object. Downloads go by presigned link ({@link
 * FileLinkService}); {@link #read(Long)} is for the flows that must stream (password-protected
 * e-mail attachments, ZIP bundles) and re-checks the SHA-256.
 */
@Service
@Transactional
public class StoredFileService {

  /** Entity type of the audit entries. */
  public static final String ENTITY = "StoredFile";

  private static final String SHARED_COMPANY = "shared";

  private final StoredFileRepository files;
  private final FileStore store;
  private final RecordClassService recordClasses;
  private final FileScanService scans;
  private final UploadRules rules;
  private final OrganizationDirectory organization;
  private final StorageProperties properties;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param files metadata
   * @param store object store
   * @param recordClasses record classes
   * @param scans scan results
   * @param rules upload rules
   * @param organization company codes
   * @param properties storage settings
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public StoredFileService(
      StoredFileRepository files,
      FileStore store,
      RecordClassService recordClasses,
      FileScanService scans,
      UploadRules rules,
      OrganizationDirectory organization,
      StorageProperties properties,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.files = files;
    this.store = store;
    this.recordClasses = recordClasses;
    this.scans = scans;
    this.rules = rules;
    this.organization = organization;
    this.properties = properties;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Stores a file: size, type and signature checks, SHA-256 (compared with the checksum the caller
   * declared, if any), object write with SSE-KMS, metadata row, scan result, audit entry. The
   * caller (the owning module, or the API after the owner's permission check) is responsible for
   * the permission.
   *
   * @param request the file
   * @return metadata
   */
  public StoredFile store(StoreRequest request) {
    FileOwner owner = UploadRules.requireOwner(request.owner());
    RecordClass recordClass = recordClasses.requireActive(request.recordClass());
    byte[] content = request.content() == null ? new byte[0] : request.content();
    rules.requireSize(content.length, rules.maxUploadBytes());
    String fileName = UploadRules.sanitize(request.fileName());
    String contentType = rules.requireType(fileName, content);
    return persist(
        new Checked(owner, request, recordClass, fileName, contentType),
        content,
        FileOrigin.UPLOADED);
  }

  /**
   * Stores a file whose content the owning module produced itself or has already validated with its
   * own rules (generated documents and report outputs, attachments checked by the attachment
   * module, and the copy of the files kept in the database before ST1): no allow-list of types and
   * no upload size limit. The SHA-256 is computed (and compared with a declared one), the object
   * written with SSE-KMS, then the row saved and audited. A {@link FileOrigin#GENERATED} file is
   * recorded clean; an {@link FileOrigin#UPLOADED} file waits for its malware scan.
   *
   * @param request the file
   * @param contentType content type the module determined
   * @param origin generated by the application or supplied from outside
   * @return metadata
   */
  public StoredFile storeChecked(StoreRequest request, String contentType, FileOrigin origin) {
    FileOwner owner = UploadRules.requireOwner(request.owner());
    RecordClass recordClass = recordClasses.requireActive(request.recordClass());
    byte[] content = request.content() == null ? new byte[0] : request.content();
    String fileName = UploadRules.sanitize(request.fileName());
    String type =
        contentType == null || contentType.isBlank()
            ? "application/octet-stream"
            : contentType.strip();
    return persist(new Checked(owner, request, recordClass, fileName, type), content, origin);
  }

  private StoredFile persist(Checked facts, byte[] content, FileOrigin origin) {
    String sha256 = Sha256.hex(content);
    UploadRules.requireDeclaredChecksum(facts.request().sha256(), sha256);
    RecordClass recordClass = facts.recordClass();
    FileOwner owner = facts.owner();
    Instant now = clock.instant();
    ObjectRef ref =
        new ObjectRef(
            recordClass.getBucketClass(),
            ObjectKeys.newKey(companyCode(owner), owner.entityType(), now, UUID.randomUUID()));
    StoredObject written = write(ref, content, facts.contentType(), sha256);
    StoredFile file =
        files.save(
            new StoredFile(
                new NewStoredFile(
                    owner,
                    UploadRules.blankToNull(facts.request().documentType()),
                    recordClass,
                    facts.fileName(),
                    facts.contentType(),
                    content.length,
                    sha256,
                    bucketName(ref),
                    ref,
                    written.versionId(),
                    ScanStatus.PENDING,
                    recordClasses.retentionUntil(recordClass, today()))));
    audit.record(
        ENTITY,
        file.getId(),
        AuditAction.CREATE,
        "Stored "
            + FileScanService.describe(file)
            + ", "
            + content.length
            + " bytes, class "
            + recordClass.getCode()
            + (origin == FileOrigin.GENERATED ? ", generated by the application" : ""));
    if (origin == FileOrigin.GENERATED) {
      scans.generated(file);
    } else {
      scans.evaluate(file);
    }
    return file;
  }

  /**
   * A live file.
   *
   * @param id id
   * @return metadata
   */
  @Transactional(readOnly = true)
  public StoredFile get(Long id) {
    return files
        .findById(id)
        .filter(StoredFile::isLive)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * The live files of a record.
   *
   * @param owner owning record
   * @return files, oldest first
   */
  @Transactional(readOnly = true)
  public List<StoredFile> filesOf(FileOwner owner) {
    return files.liveOf(owner.entityType(), owner.entityId());
  }

  /**
   * Quarantined files, newest first.
   *
   * @param page page
   * @return files
   */
  @Transactional(readOnly = true)
  public Page<StoredFile> quarantined(Pageable page) {
    return files.findByScanStatusOrderByScannedAtDesc(ScanStatus.QUARANTINED, page);
  }

  /**
   * Reads the bytes of a clean file and re-checks the SHA-256 (streaming flows only; downloads use
   * a presigned link). The caller has checked the permission; the read is audited.
   *
   * @param id id
   * @return bytes
   */
  public byte[] read(Long id) {
    StoredFile file = get(id);
    requireDownloadable(file.getScanStatus());
    byte[] content;
    try {
      content = store.get(file.objectRef());
    } catch (FileStoreException e) {
      throw new BusinessRuleException("FILE_CONTENT_MISSING", "The stored file cannot be read", e);
    }
    if (!Sha256.hex(content).equals(file.getSha256())) {
      audit.recordIndependently(
          currentUser.username(),
          ENTITY,
          id,
          AuditAction.REJECT,
          "Checksum verification failed: " + FileScanService.describe(file));
      throw new BusinessRuleException(
          "FILE_INTEGRITY_FAILURE", "The stored file failed its checksum verification");
    }
    audit.record(ENTITY, id, AuditAction.EXPORT, "Read " + FileScanService.describe(file));
    return content;
  }

  /**
   * Soft-deletes a file (refused under legal hold); the retention job removes the object after the
   * grace period.
   *
   * @param id id
   */
  public void delete(Long id) {
    StoredFile file = get(id);
    file.markDeleted(currentUser.username(), clock.instant());
    audit.record(ENTITY, id, AuditAction.DEACTIVATE, "Deleted " + FileScanService.describe(file));
  }

  /**
   * The owning record reached its final state (signed, issued, filed). Files of a class archived to
   * ECM become due for the {@code FILE_ECM_ARCHIVE} job.
   *
   * @param id id
   * @return metadata
   */
  public StoredFile markFinal(Long id) {
    StoredFile file = get(id);
    if (file.markFinal(clock.instant())) {
      audit.record(
          ENTITY, id, AuditAction.UPDATE, "Final; due for ECM: " + FileScanService.describe(file));
    }
    return file;
  }

  /**
   * Records the reference the ECM returned for an archived record (ECM adapter, question DSQ02).
   *
   * @param id id
   * @param reference ECM reference
   * @return metadata
   */
  public StoredFile recordEcmReference(Long id, String reference) {
    if (reference == null || reference.isBlank()) {
      throw new BusinessRuleException("ECM_REFERENCE_REQUIRED", "Enter the ECM reference");
    }
    StoredFile file =
        files.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    file.ecmArchived(reference.strip(), clock.instant());
    audit.record(ENTITY, id, AuditAction.UPDATE, "Archived in ECM as " + reference.strip());
    return file;
  }

  /**
   * Refuses a file that may not be downloaded: scan pending or quarantined.
   *
   * @param status scan status of the file
   */
  static void requireDownloadable(ScanStatus status) {
    if (status == ScanStatus.QUARANTINED) {
      throw new BusinessRuleException(
          "FILE_QUARANTINED", "The file was quarantined by the malware scan");
    }
    if (status != ScanStatus.CLEAN) {
      throw new BusinessRuleException(
          "FILE_SCAN_PENDING", "The malware scan of the file has not completed yet");
    }
  }

  /**
   * The company segment of a key: the company code (not personal data).
   *
   * @param owner owner
   * @return code
   */
  @Transactional(readOnly = true)
  public String companyCode(FileOwner owner) {
    return owner.companyId() == null
        ? SHARED_COMPANY
        : organization.company(owner.companyId()).code().toLowerCase(Locale.ROOT);
  }

  /**
   * The bucket name of a reference (the class name for the local store).
   *
   * @param ref reference
   * @return bucket
   */
  public String bucketName(ObjectRef ref) {
    String name = properties.buckets().of(ref.bucket());
    return name == null ? ref.bucket().name().toLowerCase(Locale.ROOT) : name;
  }

  /**
   * The business date of the storage (retention dates).
   *
   * @return date
   */
  public LocalDate today() {
    return BusinessClock.today(clock);
  }

  private StoredObject write(ObjectRef ref, byte[] content, String contentType, String sha256) {
    try {
      return store.put(ref, content, contentType, sha256);
    } catch (FileStoreException e) {
      throw new BusinessRuleException("FILE_STORE_FAILED", "The file could not be stored", e);
    }
  }

  /** Owner, class, name and type of a file whose checks passed. */
  private record Checked(
      FileOwner owner,
      StoreRequest request,
      RecordClass recordClass,
      String fileName,
      String contentType) {}

  /**
   * A file to store.
   *
   * @param owner owning record
   * @param documentType document type of the owning module (optional)
   * @param recordClass record class code
   * @param fileName file name as uploaded
   * @param content bytes
   * @param sha256 SHA-256 the sender declared (optional, hex)
   */
  public record StoreRequest(
      FileOwner owner,
      String documentType,
      String recordClass,
      String fileName,
      byte[] content,
      String sha256) {}
}
