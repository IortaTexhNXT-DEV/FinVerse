package com.iortatechnxt.brokerverse.migration.load.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Migration batches. */
public interface MigBatchRepository extends JpaRepository<MigBatch, Long> {

  /**
   * A batch by number.
   *
   * @param batchNo number
   * @return batch
   */
  Optional<MigBatch> findByBatchNo(String batchNo);

  /**
   * Batches of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return batches
   */
  Page<MigBatch> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);

  /**
   * Batches of an object, newest first.
   *
   * @param companyId company
   * @param objectCode object
   * @return batches
   */
  List<MigBatch> findByCompanyIdAndObjectCodeOrderByIdDesc(Long companyId, String objectCode);

  /**
   * Batches in statuses.
   *
   * @param statuses statuses
   * @return batches
   */
  List<MigBatch> findByStatusInOrderByIdAsc(Collection<BatchStatus> statuses);

  /**
   * Batches of a company in statuses.
   *
   * @param companyId company
   * @param statuses statuses
   * @return batches
   */
  List<MigBatch> findByCompanyIdAndStatusIn(Long companyId, Collection<BatchStatus> statuses);

  /**
   * Batches of a parent (reruns).
   *
   * @param parentBatchId parent
   * @return children
   */
  List<MigBatch> findByParentBatchIdOrderByIdAsc(Long parentBatchId);

  /**
   * Whether an object has a batch in one of the statuses.
   *
   * @param companyId company
   * @param objectCode object
   * @param statuses statuses
   * @return true when one exists
   */
  boolean existsByCompanyIdAndObjectCodeAndStatusIn(
      Long companyId, String objectCode, Collection<BatchStatus> statuses);
}
