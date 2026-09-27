package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Lines of the No Touch batches. */
public interface SbmNoTouchLineRepository extends JpaRepository<SbmNoTouchLine, Long> {

  /**
   * Lines of a batch.
   *
   * @param batchId batch
   * @return lines
   */
  List<SbmNoTouchLine> findByBatchIdOrderByIdAsc(Long batchId);

  /**
   * A line of a batch by masterlist number.
   *
   * @param batchId batch
   * @param sbmNo masterlist number
   * @return line
   */
  Optional<SbmNoTouchLine> findByBatchIdAndSbmNo(Long batchId, String sbmNo);
}
