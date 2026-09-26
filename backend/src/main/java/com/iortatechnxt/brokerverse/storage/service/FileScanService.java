package com.iortatechnxt.brokerverse.storage.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.storage.FileStore;
import com.iortatechnxt.brokerverse.common.storage.ObjectKeys;
import com.iortatechnxt.brokerverse.common.storage.ObjectMetadata;
import com.iortatechnxt.brokerverse.common.storage.ObjectRef;
import com.iortatechnxt.brokerverse.common.storage.StorageProperties;
import com.iortatechnxt.brokerverse.common.storage.StoredObject;
import com.iortatechnxt.brokerverse.storage.domain.RecordClass;
import com.iortatechnxt.brokerverse.storage.domain.ScanStatus;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import com.iortatechnxt.brokerverse.storage.domain.StoredFileRepository;
import com.iortatechnxt.brokerverse.storage.service.FileQuarantineListener.QuarantinedFile;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the malware scan result of stored files (DOCUMENT_STORAGE_DECISION, decision 2): the
 * scanning service (Amazon GuardDuty Malware Protection for S3) tags each object with {@code
 * GuardDutyMalwareScanStatus}. Only {@value #NO_THREATS_FOUND} is accepted; any other result moves
 * the object under the quarantine prefix and tells the {@link FileQuarantineListener}s (uploader
 * and security role). A file whose tag is still missing stays {@code PENDING} and cannot be
 * downloaded. A clean file of a record class under legal hold receives its hold here.
 */
@Service
@Transactional
public class FileScanService {

  /** The only scan result that is accepted. */
  public static final String NO_THREATS_FOUND = "NO_THREATS_FOUND";

  private static final String ENTITY = StoredFileService.ENTITY;

  private final StoredFileRepository files;
  private final FileStore store;
  private final RecordClassService recordClasses;
  private final List<FileQuarantineListener> listeners;
  private final StorageProperties properties;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param files metadata
   * @param store object store
   * @param recordClasses record classes
   * @param listeners quarantine listeners
   * @param properties storage settings
   * @param audit audit trail
   * @param clock clock
   */
  public FileScanService(
      StoredFileRepository files,
      FileStore store,
      RecordClassService recordClasses,
      List<FileQuarantineListener> listeners,
      StorageProperties properties,
      AuditTrailService audit,
      Clock clock) {
    this.files = files;
    this.store = store;
    this.recordClasses = recordClasses;
    this.listeners = List.copyOf(listeners);
    this.properties = properties;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Ids of files whose scan result is still to be read, oldest first.
   *
   * @param batch maximum number
   * @return ids
   */
  @Transactional(readOnly = true)
  public List<Long> pendingIds(int batch) {
    return files
        .findByScanStatusOrderByCreatedAtAsc(ScanStatus.PENDING, PageRequest.of(0, batch))
        .stream()
        .map(StoredFile::getId)
        .toList();
  }

  /**
   * Reads the scan result of one file in its own transaction (jobs, and link requests that are
   * refused afterwards but must keep a quarantine).
   *
   * @param id stored file id
   * @return status after the check
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ScanStatus refresh(Long id) {
    return evaluate(
        files.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id)));
  }

  /**
   * Reads the scan result of a pending file and applies it.
   *
   * @param file stored file
   * @return status after the check
   */
  public ScanStatus evaluate(StoredFile file) {
    if (file.getScanStatus() != ScanStatus.PENDING) {
      return file.getScanStatus();
    }
    Optional<String> result =
        store.metadata(file.objectRef()).flatMap(m -> scanResult(m, properties.scanTag()));
    if (result.isEmpty()) {
      return ScanStatus.PENDING;
    }
    if (NO_THREATS_FOUND.equals(result.get())) {
      file.scannedClean(result.get(), clock.instant());
      audit.record(
          ENTITY, file.getId(), AuditAction.UPDATE, "Malware scan clean: " + describe(file));
      applyClassHold(file);
      return ScanStatus.CLEAN;
    }
    quarantine(file, result.get());
    return ScanStatus.QUARANTINED;
  }

  private static Optional<String> scanResult(ObjectMetadata metadata, String tag) {
    return metadata.tag(tag).filter(v -> !v.isBlank());
  }

  private void quarantine(StoredFile file, String result) {
    ObjectRef from = file.objectRef();
    ObjectRef to = from.withPrefix(ObjectKeys.QUARANTINE_PREFIX);
    StoredObject moved = store.copy(from, to);
    store.delete(from);
    file.quarantined(result, to, moved.versionId(), clock.instant());
    audit.record(
        ENTITY,
        file.getId(),
        AuditAction.REJECT,
        "Quarantined after malware scan " + result + ": " + describe(file));
    QuarantinedFile facts =
        new QuarantinedFile(
            file.getId(),
            file.getCompanyId(),
            file.getOwnerEntityType(),
            file.getOwnerEntityId(),
            file.getFileName(),
            file.getCreatedBy(),
            result);
    listeners.forEach(l -> l.quarantined(facts));
  }

  private void applyClassHold(StoredFile file) {
    RecordClass recordClass = recordClasses.get(file.getRecordClass());
    if (!recordClass.isLegalHold() || file.isLegalHold()) {
      return;
    }
    store.legalHold(file.objectRef(), true);
    file.placeHold(
        "Record class " + recordClass.getCode() + " is under legal hold",
        CurrentUser.SYSTEM,
        null,
        clock.instant());
    audit.record(
        ENTITY,
        file.getId(),
        AuditAction.UPDATE,
        "Legal hold placed (record class " + recordClass.getCode() + "): " + describe(file));
  }

  static String describe(StoredFile file) {
    return file.getFileName()
        + " of "
        + file.getOwnerEntityType()
        + " "
        + file.getOwnerEntityId()
        + " (SHA-256 "
        + file.getSha256()
        + ")";
  }
}
