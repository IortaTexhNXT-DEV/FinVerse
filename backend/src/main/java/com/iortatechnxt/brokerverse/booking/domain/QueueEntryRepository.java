package com.iortatechnxt.brokerverse.booking.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Booking queue. */
public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

  /**
   * The open queue entry of an account.
   *
   * @param arn account
   * @param status QUEUED
   * @return entry
   */
  Optional<QueueEntry> findByArnAndStatus(String arn, QueueStatus status);

  /**
   * Every entry of an account in a status (e.g. its earlier failures).
   *
   * @param arn account
   * @param status status
   * @return entries
   */
  List<QueueEntry> findAllByArnAndStatus(String arn, QueueStatus status);

  /**
   * Entries of a company in some statuses, oldest first.
   *
   * @param companyId company
   * @param statuses statuses
   * @return entries
   */
  List<QueueEntry> findByCompanyIdAndStatusInOrderByIdAsc(
      Long companyId, Collection<QueueStatus> statuses);

  /**
   * Entries of a company in a status, newest first (paged).
   *
   * @param companyId company
   * @param status status
   * @param pageable page
   * @return page
   */
  Page<QueueEntry> findByCompanyIdAndStatusOrderByIdDesc(
      Long companyId, QueueStatus status, Pageable pageable);

  /**
   * Open entries of every company (the scheduled batch).
   *
   * @param status QUEUED
   * @return entries, oldest first
   */
  List<QueueEntry> findByStatusOrderByIdAsc(QueueStatus status);

  /**
   * Count of entries in a status.
   *
   * @param companyId company
   * @param status status
   * @return count
   */
  long countByCompanyIdAndStatus(Long companyId, QueueStatus status);
}
