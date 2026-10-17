package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** No Touch billing batches. */
public interface SbmNoTouchBatchRepository extends JpaRepository<SbmNoTouchBatch, Long> {

  /**
   * The batch of an insurer and month.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param period month
   * @return batch
   */
  Optional<SbmNoTouchBatch> findByCompanyIdAndInsurerCodeAndPeriod(
      Long companyId, String insurerCode, String period);

  /**
   * A batch by number.
   *
   * @param batchNo number
   * @return batch
   */
  Optional<SbmNoTouchBatch> findByBatchNo(String batchNo);

  /**
   * Batches of a company, newest first.
   *
   * @param companyId company
   * @return batches
   */
  List<SbmNoTouchBatch> findByCompanyIdOrderByIdDesc(Long companyId);
}
