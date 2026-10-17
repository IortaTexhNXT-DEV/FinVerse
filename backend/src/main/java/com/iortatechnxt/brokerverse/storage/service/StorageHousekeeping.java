package com.iortatechnxt.brokerverse.storage.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.storage.FileStore;
import com.iortatechnxt.brokerverse.common.storage.FileStoreException;
import com.iortatechnxt.brokerverse.common.storage.ObjectMetadata;
import com.iortatechnxt.brokerverse.common.storage.ObjectRef;
import com.iortatechnxt.brokerverse.common.storage.StorageProperties;
import com.iortatechnxt.brokerverse.events.service.IntegrationEvent;
import com.iortatechnxt.brokerverse.events.service.IntegrationEventPublisher;
import com.iortatechnxt.brokerverse.storage.domain.EcmStatus;
import com.iortatechnxt.brokerverse.storage.domain.ScanStatus;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import com.iortatechnxt.brokerverse.storage.domain.StoredFileRepository;
import com.iortatechnxt.brokerverse.storage.service.StorageTopics.EcmArchiveRequested;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The units of work of the storage jobs, one transaction per file or object: retention delete
 * (never under legal hold), removal of soft-deleted objects, abandoned uploads, orphan objects and
 * the ECM archive publisher.
 */
@Service
@Transactional
public class StorageHousekeeping {

  /** Most files or objects handled per run of a job; the rest follow in the next run. */
  static final int BATCH = 5000;

  private static final Logger LOG = LoggerFactory.getLogger(StorageHousekeeping.class);
  private static final String ENTITY = StoredFileService.ENTITY;
  private static final String OBJECT_ENTITY = "StoredObject";
  private static final int MAX_AUDIT_ID = 60;

  private final StoredFileRepository files;
  private final FileStore store;
  private final IntegrationEventPublisher publisher;
  private final StorageProperties properties;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param files metadata
   * @param store object store
   * @param publisher integration outbox
   * @param properties storage settings
   * @param audit audit trail
   * @param clock clock
   */
  public StorageHousekeeping(
      StoredFileRepository files,
      FileStore store,
      IntegrationEventPublisher publisher,
      StorageProperties properties,
      AuditTrailService audit,
      Clock clock) {
    this.files = files;
    this.store = store;
    this.publisher = publisher;
    this.properties = properties;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Files whose retention ended, not under legal hold.
   *
   * @param today business date
   * @return ids
   */
  @Transactional(readOnly = true)
  public List<Long> retentionDue(LocalDate today) {
    return files.retentionDue(today, PageRequest.of(0, BATCH));
  }

  /**
   * Soft-deleted files past the grace period, not under legal hold.
   *
   * @return ids
   */
  @Transactional(readOnly = true)
  public List<Long> deletedDue() {
    return files.deletedDue(
        clock.instant().minus(properties.deletedGrace()), PageRequest.of(0, BATCH));
  }

  /**
   * Announced inbound uploads older than the orphan age that were never confirmed.
   *
   * @return ids
   */
  @Transactional(readOnly = true)
  public List<Long> abandonedUploads() {
    return files.announcedBefore(
        ScanStatus.AWAITING_UPLOAD,
        clock.instant().minus(properties.orphanAge()),
        PageRequest.of(0, BATCH));
  }

  /**
   * Files past retention that stay because of a legal hold.
   *
   * @param today business date
   * @return count
   */
  @Transactional(readOnly = true)
  public long heldPastRetention(LocalDate today) {
    return files.heldPastRetention(today);
  }

  /**
   * Removes the object of a file and marks the row purged; skipped under legal hold.
   *
   * @param id stored file id
   * @param why reason for the audit entry
   * @return true when the object was removed
   */
  public boolean purge(Long id, String why) {
    StoredFile file = find(id);
    if (file.isLegalHold() || file.getPurgedAt() != null) {
      return false;
    }
    try {
      store.delete(file.objectRef());
    } catch (FileStoreException e) {
      LOG.warn("Object of stored file {} not removed: {}", id, e.getMessage());
      return false;
    }
    file.markPurged(clock.instant());
    audit.record(
        ENTITY,
        id,
        AuditAction.DEACTIVATE,
        "Object removed (" + why + "): " + FileScanService.describe(file));
    return true;
  }

  /**
   * Closes an announced upload that was never confirmed (the retention job removes any object).
   *
   * @param id stored file id
   */
  public void abandon(Long id) {
    StoredFile file = find(id);
    file.markDeleted(CurrentUser.SYSTEM, clock.instant());
    audit.record(
        ENTITY,
        id,
        AuditAction.DEACTIVATE,
        "Upload never confirmed: " + FileScanService.describe(file));
  }

  /**
   * The keys of a bucket that have a metadata row.
   *
   * @param bucket bucket name
   * @param keys candidate keys
   * @return keys with a row
   */
  @Transactional(readOnly = true)
  public Set<String> knownKeys(String bucket, Collection<String> keys) {
    return new HashSet<>(files.knownKeys(bucket, keys));
  }

  /**
   * Deletes an object that has no metadata row (skipped when it carries a legal hold).
   *
   * @param ref object
   * @return true when deleted
   */
  public boolean deleteOrphan(ObjectRef ref) {
    boolean held = store.metadata(ref).map(ObjectMetadata::legalHold).orElse(false);
    if (held) {
      return false;
    }
    store.delete(ref);
    String name = ref.key().substring(ref.key().lastIndexOf('/') + 1);
    audit.record(
        OBJECT_ENTITY,
        name.length() > MAX_AUDIT_ID ? name.substring(0, MAX_AUDIT_ID) : name,
        AuditAction.DEACTIVATE,
        "Orphan object deleted (no metadata row): " + ref.bucket() + " " + ref.key());
    return true;
  }

  /**
   * Files due for the ECM archive publisher.
   *
   * @return ids
   */
  @Transactional(readOnly = true)
  public List<Long> ecmDue() {
    return files.findByEcmStatusOrderByIdAsc(EcmStatus.PENDING, PageRequest.of(0, BATCH)).stream()
        .map(StoredFile::getId)
        .toList();
  }

  /**
   * Publishes the ECM archive request of a final record through the integration outbox (the user's
   * transaction never waits for the ECM).
   *
   * @param id stored file id
   * @return true when published
   */
  public boolean publishEcm(Long id) {
    StoredFile file = find(id);
    if (file.getEcmStatus() != EcmStatus.PENDING || file.getScanStatus() != ScanStatus.CLEAN) {
      return false;
    }
    publisher.publish(
        new IntegrationEvent(
            StorageTopics.ECM_ARCHIVE_REQUESTED,
            StorageTopics.TYPE_ECM_ARCHIVE_REQUESTED,
            "stored-file:" + id,
            file.getCompanyId(),
            new EcmArchiveRequested(
                id,
                file.getRecordClass(),
                file.getDocumentType(),
                file.getOwnerEntityType(),
                file.getOwnerEntityId(),
                file.getContentType(),
                file.getSizeBytes(),
                file.getSha256(),
                file.getBucket(),
                file.getObjectKey(),
                file.getVersionId(),
                file.getFinalAt())));
    Instant now = clock.instant();
    file.ecmRequested(now);
    audit.record(
        ENTITY, id, AuditAction.EXPORT, "ECM archive requested: " + FileScanService.describe(file));
    return true;
  }

  private StoredFile find(Long id) {
    return files.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }
}
