package com.iortatechnxt.brokerverse.renewal.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Insurer batches. */
public interface InsurerBatchRepository extends JpaRepository<InsurerBatch, Long> {

  /**
   * A batch by number.
   *
   * @param companyId company
   * @param batchNo batch number
   * @return batch
   */
  Optional<InsurerBatch> findByCompanyIdAndBatchNo(Long companyId, String batchNo);

  /**
   * Batches of a company, newest first.
   *
   * @param companyId company
   * @return batches
   */
  List<InsurerBatch> findByCompanyIdOrderByIdDesc(Long companyId);

  /**
   * Batches in a status whose reply is due before a date (overdue alert).
   *
   * @param statuses statuses
   * @param date date
   * @return batches
   */
  List<InsurerBatch> findByStatusInAndReplyDueBefore(
      Collection<InsurerBatchStatus> statuses, LocalDate date);
}
