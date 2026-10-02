package com.iortatechnxt.brokerverse.storage.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Stored file metadata. */
public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

  /**
   * The live files of a record, oldest first.
   *
   * @param entityType owner entity type
   * @param entityId owner key
   * @return files
   */
  @Query(
      "select f from StoredFile f where f.ownerEntityType = :type and f.ownerEntityId = :id"
          + " and f.deletedAt is null and f.purgedAt is null order by f.createdAt, f.id")
  List<StoredFile> liveOf(@Param("type") String entityType, @Param("id") String entityId);

  /**
   * Files whose scan result is still to be read, oldest first.
   *
   * @param status scan status ({@code PENDING})
   * @param page batch
   * @return files
   */
  List<StoredFile> findByScanStatusOrderByCreatedAtAsc(ScanStatus status, Pageable page);

  /**
   * Quarantined files, newest first.
   *
   * @param status {@code QUARANTINED}
   * @param page page
   * @return files
   */
  Page<StoredFile> findByScanStatusOrderByScannedAtDesc(ScanStatus status, Pageable page);

  /**
   * Files due for the ECM archive publisher.
   *
   * @param status {@code PENDING}
   * @param page batch
   * @return files
   */
  List<StoredFile> findByEcmStatusOrderByIdAsc(EcmStatus status, Pageable page);

  /**
   * Files whose retention ended and whose object is still present, not under legal hold.
   *
   * @param today business date
   * @param page batch
   * @return ids
   */
  @Query(
      "select f.id from StoredFile f where f.purgedAt is null and f.legalHold = false"
          + " and f.retentionUntil < :today order by f.id")
  List<Long> retentionDue(@Param("today") LocalDate today, Pageable page);

  /**
   * Soft-deleted files past the grace period whose object is still present, not under legal hold.
   *
   * @param before deletion time limit
   * @param page batch
   * @return ids
   */
  @Query(
      "select f.id from StoredFile f where f.purgedAt is null and f.legalHold = false"
          + " and f.deletedAt < :before order by f.id")
  List<Long> deletedDue(@Param("before") Instant before, Pageable page);

  /**
   * Announced inbound uploads never confirmed.
   *
   * @param status {@code AWAITING_UPLOAD}
   * @param before announcement time limit
   * @param page batch
   * @return ids
   */
  @Query(
      "select f.id from StoredFile f where f.scanStatus = :status and f.createdAt < :before"
          + " and f.deletedAt is null order by f.id")
  List<Long> announcedBefore(
      @Param("status") ScanStatus status, @Param("before") Instant before, Pageable page);

  /**
   * Files that would be due for removal but are under legal hold.
   *
   * @param today business date
   * @return count
   */
  @Query(
      "select count(f) from StoredFile f where f.purgedAt is null and f.legalHold = true"
          + " and f.retentionUntil < :today")
  long heldPastRetention(@Param("today") LocalDate today);

  /**
   * The keys of a bucket that have a metadata row (orphan reconciliation).
   *
   * @param bucket bucket name
   * @param keys candidate keys
   * @return keys with a row
   */
  @Query("select f.objectKey from StoredFile f where f.bucket = :bucket and f.objectKey in :keys")
  List<String> knownKeys(@Param("bucket") String bucket, @Param("keys") Collection<String> keys);
}
