package com.iortatechnxt.brokerverse.bulk.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Bulk rows. */
public interface BulkRowRepository extends JpaRepository<BulkRowRecord, Long> {

  /**
   * Rows of a job.
   *
   * @param jobId job
   * @return rows by row number
   */
  List<BulkRowRecord> findByJobIdOrderByRowNo(Long jobId);

  /**
   * Rows of a job with a status.
   *
   * @param jobId job
   * @param status status
   * @return rows by row number
   */
  List<BulkRowRecord> findByJobIdAndStatusOrderByRowNo(Long jobId, BulkRowStatus status);

  /**
   * A page of a job's rows.
   *
   * @param jobId job
   * @param pageable page
   * @return rows
   */
  Page<BulkRowRecord> findByJobIdOrderByRowNo(Long jobId, Pageable pageable);

  /**
   * A page of a job's rows with a status.
   *
   * @param jobId job
   * @param status status
   * @param pageable page
   * @return rows
   */
  Page<BulkRowRecord> findByJobIdAndStatusOrderByRowNo(
      Long jobId, BulkRowStatus status, Pageable pageable);

  /**
   * Committed rows per outcome category of a job (BRQID.006 run report).
   *
   * @param jobId job
   * @param status committed status
   * @return rows of [category (null = uncategorised), count]
   */
  @Query(
      "select r.outcome, count(r) from BulkRowRecord r"
          + " where r.jobId = :jobId and r.status = :status group by r.outcome")
  List<Object[]> countOutcomes(@Param("jobId") Long jobId, @Param("status") BulkRowStatus status);
}
