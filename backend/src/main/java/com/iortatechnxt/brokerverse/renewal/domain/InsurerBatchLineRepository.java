package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Lines of insurer batches. */
public interface InsurerBatchLineRepository extends JpaRepository<InsurerBatchLine, Long> {

  /**
   * Lines of a batch.
   *
   * @param batchId batch
   * @return lines
   */
  List<InsurerBatchLine> findByBatchIdOrderByIdAsc(Long batchId);

  /**
   * Lines of a candidate, newest first.
   *
   * @param candidateId candidate
   * @return lines
   */
  List<InsurerBatchLine> findByCandidateIdOrderByIdDesc(Long candidateId);
}
