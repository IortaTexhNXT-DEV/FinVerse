package com.iortatechnxt.brokerverse.storage.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.storage.BucketClass;
import com.iortatechnxt.brokerverse.common.storage.ObjectRef;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Metadata of a stored object (table {@code stored_file}, DOCUMENT_STORAGE_DECISION section 3):
 * owner, type, name, size, SHA-256, bucket and key, retention, legal hold, malware scan state and
 * ECM archive state. The bytes live in the object store only.
 */
@Entity
@Table(name = "stored_file")
public class StoredFile extends BaseEntity {

  @Column(name = "company_id", updatable = false)
  private Long companyId;

  @Column(name = "owner_entity_type", nullable = false, length = 60, updatable = false)
  private String ownerEntityType;

  @Column(name = "owner_entity_id", nullable = false, length = 60, updatable = false)
  private String ownerEntityId;

  @Column(name = "document_type", length = 40)
  private String documentType;

  @Column(name = "record_class", nullable = false, length = 40, updatable = false)
  private String recordClass;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "content_type", nullable = false, length = 150)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(nullable = false, length = 64, updatable = false)
  private String sha256;

  @Enumerated(EnumType.STRING)
  @Column(name = "bucket_class", nullable = false, length = 20, updatable = false)
  private BucketClass bucketClass;

  @Column(nullable = false, length = 63, updatable = false)
  private String bucket;

  @Column(name = "object_key", nullable = false, length = 512)
  private String objectKey;

  @Column(name = "version_id", length = 1024)
  private String versionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "scan_status", nullable = false, length = 20)
  private ScanStatus scanStatus;

  @Column(name = "scan_result", length = 40)
  private String scanResult;

  @Column(name = "scanned_at")
  private Instant scannedAt;

  @Column(name = "retention_until", nullable = false)
  private LocalDate retentionUntil;

  @Column(name = "legal_hold", nullable = false)
  private boolean legalHold;

  @Column(name = "legal_hold_reason", length = 500)
  private String legalHoldReason;

  @Column(name = "legal_hold_placed_by", length = 50)
  private String legalHoldPlacedBy;

  @Column(name = "legal_hold_approved_by", length = 50)
  private String legalHoldApprovedBy;

  @Column(name = "legal_hold_at")
  private Instant legalHoldAt;

  @Column(name = "final_at")
  private Instant finalAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "ecm_status", nullable = false, length = 20)
  private EcmStatus ecmStatus = EcmStatus.NOT_REQUIRED;

  @Column(name = "ecm_reference", length = 200)
  private String ecmReference;

  @Column(name = "ecm_requested_at")
  private Instant ecmRequestedAt;

  @Column(name = "ecm_archived_at")
  private Instant ecmArchivedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @Column(name = "deleted_by", length = 50)
  private String deletedBy;

  @Column(name = "purged_at")
  private Instant purgedAt;

  protected StoredFile() {}

  /**
   * Records a new file.
   *
   * @param file facts of the file
   */
  public StoredFile(NewStoredFile file) {
    this.companyId = file.owner().companyId();
    this.ownerEntityType = file.owner().entityType();
    this.ownerEntityId = file.owner().entityId();
    this.documentType = file.documentType();
    this.recordClass = file.recordClass().getCode();
    this.fileName = file.fileName();
    this.contentType = file.contentType();
    this.sizeBytes = file.sizeBytes();
    this.sha256 = file.sha256();
    this.bucketClass = file.ref().bucket();
    this.bucket = file.bucket();
    this.objectKey = file.ref().key();
    this.versionId = file.versionId();
    this.scanStatus = file.scanStatus();
    this.retentionUntil = file.retentionUntil();
    this.ecmStatus =
        file.recordClass().isArchiveToEcm() ? EcmStatus.AWAITING_FINAL : EcmStatus.NOT_REQUIRED;
  }

  /**
   * Where the object is.
   *
   * @return object reference
   */
  public ObjectRef objectRef() {
    return new ObjectRef(bucketClass, objectKey);
  }

  /**
   * The owning record.
   *
   * @return owner
   */
  public FileOwner owner() {
    return new FileOwner(companyId, ownerEntityType, ownerEntityId);
  }

  /**
   * Whether the file is live (not deleted and its object not purged).
   *
   * @return true when live
   */
  public boolean isLive() {
    return deletedAt == null && purgedAt == null;
  }

  /**
   * Confirms a presigned upload: the object is present and waits for its scan.
   *
   * @param version object version
   */
  public void uploaded(String version) {
    if (scanStatus != ScanStatus.AWAITING_UPLOAD) {
      throw new BusinessRuleException("FILE_ALREADY_UPLOADED", "The file was already uploaded");
    }
    this.versionId = version;
    this.scanStatus = ScanStatus.PENDING;
  }

  /**
   * Records a clean scan result.
   *
   * @param result scan result value
   * @param at time
   */
  public void scannedClean(String result, Instant at) {
    this.scanStatus = ScanStatus.CLEAN;
    this.scanResult = result;
    this.scannedAt = at;
  }

  /**
   * Records a quarantine: the object moved under the quarantine prefix.
   *
   * @param result scan result value
   * @param quarantined new object reference
   * @param version version of the moved object
   * @param at time
   */
  public void quarantined(String result, ObjectRef quarantined, String version, Instant at) {
    this.scanStatus = ScanStatus.QUARANTINED;
    this.scanResult = result;
    this.scannedAt = at;
    this.objectKey = quarantined.key();
    this.versionId = version;
  }

  /**
   * Places a legal hold.
   *
   * @param reason reason
   * @param placedBy requester
   * @param approvedBy approver
   * @param at time
   */
  public void placeHold(String reason, String placedBy, String approvedBy, Instant at) {
    this.legalHold = true;
    this.legalHoldReason = reason;
    this.legalHoldPlacedBy = placedBy;
    this.legalHoldApprovedBy = approvedBy;
    this.legalHoldAt = at;
  }

  /** Releases the legal hold (the history stays in the requests and the audit trail). */
  public void releaseHold() {
    this.legalHold = false;
    this.legalHoldReason = null;
    this.legalHoldPlacedBy = null;
    this.legalHoldApprovedBy = null;
    this.legalHoldAt = null;
  }

  /**
   * Soft delete; the retention job removes the object later.
   *
   * @param user user
   * @param at time
   */
  public void markDeleted(String user, Instant at) {
    if (legalHold) {
      throw new BusinessRuleException(
          "FILE_UNDER_LEGAL_HOLD", "The file is under legal hold and cannot be deleted");
    }
    this.deletedAt = at;
    this.deletedBy = user;
  }

  /**
   * Records the removal of the object.
   *
   * @param at time
   */
  public void markPurged(Instant at) {
    if (deletedAt == null) {
      this.deletedAt = at;
      this.deletedBy = "SYSTEM";
    }
    this.purgedAt = at;
  }

  /**
   * The owning record reached its final state (signed, issued, filed): a file of a class archived
   * to ECM becomes due for the archive publisher.
   *
   * @param at time
   * @return true when the file is now due for ECM
   */
  public boolean markFinal(Instant at) {
    if (finalAt == null) {
      this.finalAt = at;
    }
    if (ecmStatus == EcmStatus.AWAITING_FINAL) {
      this.ecmStatus = EcmStatus.PENDING;
      return true;
    }
    return false;
  }

  /**
   * The archive request was published.
   *
   * @param at time
   */
  public void ecmRequested(Instant at) {
    this.ecmStatus = EcmStatus.REQUESTED;
    this.ecmRequestedAt = at;
  }

  /**
   * The ECM acknowledged the record.
   *
   * @param reference ECM reference
   * @param at time
   */
  public void ecmArchived(String reference, Instant at) {
    if (ecmStatus == EcmStatus.NOT_REQUIRED || ecmStatus == EcmStatus.AWAITING_FINAL) {
      throw new BusinessRuleException(
          "FILE_NOT_FOR_ECM", "The file was not sent to ECM, so it has no ECM reference");
    }
    this.ecmStatus = EcmStatus.ARCHIVED;
    this.ecmReference = reference;
    this.ecmArchivedAt = at;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getOwnerEntityType() {
    return ownerEntityType;
  }

  public String getOwnerEntityId() {
    return ownerEntityId;
  }

  public String getDocumentType() {
    return documentType;
  }

  public String getRecordClass() {
    return recordClass;
  }

  public String getFileName() {
    return fileName;
  }

  public String getContentType() {
    return contentType;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public String getSha256() {
    return sha256;
  }

  public BucketClass getBucketClass() {
    return bucketClass;
  }

  public String getBucket() {
    return bucket;
  }

  public String getObjectKey() {
    return objectKey;
  }

  public String getVersionId() {
    return versionId;
  }

  public ScanStatus getScanStatus() {
    return scanStatus;
  }

  public String getScanResult() {
    return scanResult;
  }

  public Instant getScannedAt() {
    return scannedAt;
  }

  public LocalDate getRetentionUntil() {
    return retentionUntil;
  }

  public boolean isLegalHold() {
    return legalHold;
  }

  public String getLegalHoldReason() {
    return legalHoldReason;
  }

  public String getLegalHoldPlacedBy() {
    return legalHoldPlacedBy;
  }

  public String getLegalHoldApprovedBy() {
    return legalHoldApprovedBy;
  }

  public Instant getLegalHoldAt() {
    return legalHoldAt;
  }

  public Instant getFinalAt() {
    return finalAt;
  }

  public EcmStatus getEcmStatus() {
    return ecmStatus;
  }

  public String getEcmReference() {
    return ecmReference;
  }

  public Instant getEcmRequestedAt() {
    return ecmRequestedAt;
  }

  public Instant getEcmArchivedAt() {
    return ecmArchivedAt;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public String getDeletedBy() {
    return deletedBy;
  }

  public Instant getPurgedAt() {
    return purgedAt;
  }
}
