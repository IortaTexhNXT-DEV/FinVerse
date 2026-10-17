package com.iortatechnxt.brokerverse.migration.intake.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Staged rows. */
public interface StageRowRepository extends JpaRepository<StageRow, Long> {

  /**
   * Rows of extracts in a status, in file order.
   *
   * @param extractIds extracts
   * @param statuses statuses
   * @param pageable page
   * @return rows
   */
  Page<StageRow> findByExtractIdInAndStatusInOrderByIdAsc(
      Collection<Long> extractIds, Collection<RowStatus> statuses, Pageable pageable);

  /**
   * Rows of extracts, in file order.
   *
   * @param extractIds extracts
   * @return rows
   */
  List<StageRow> findByExtractIdInOrderByIdAsc(Collection<Long> extractIds);

  /**
   * Rows of a batch in a status.
   *
   * @param batchId batch
   * @param statuses statuses
   * @return rows
   */
  List<StageRow> findByBatchIdAndStatusInOrderByIdAsc(Long batchId, Collection<RowStatus> statuses);

  /**
   * Rows of a batch, paged.
   *
   * @param batchId batch
   * @param pageable page
   * @return rows
   */
  Page<StageRow> findByBatchIdOrderByIdAsc(Long batchId, Pageable pageable);

  /**
   * Rows of a batch in a status, paged.
   *
   * @param batchId batch
   * @param status status
   * @param pageable page
   * @return rows
   */
  Page<StageRow> findByBatchIdAndStatusOrderByIdAsc(
      Long batchId, RowStatus status, Pageable pageable);

  /**
   * Rows of an extract with a legacy key (joining sub-layouts).
   *
   * @param extractId extract
   * @param legacyKeys keys
   * @return rows
   */
  List<StageRow> findByExtractIdAndLegacyKeyIn(Long extractId, Collection<String> legacyKeys);

  /**
   * Number of rows of a batch per status.
   *
   * @param batchId batch
   * @return status and count pairs
   */
  @Query("select r.status, count(r) from StageRow r where r.batchId = ?1 group by r.status")
  List<Object[]> countByStatus(Long batchId);

  /**
   * Number of rows of an extract.
   *
   * @param extractId extract
   * @return count
   */
  long countByExtractId(Long extractId);
}
