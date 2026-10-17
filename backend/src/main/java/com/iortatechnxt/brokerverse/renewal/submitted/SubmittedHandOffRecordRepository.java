package com.iortatechnxt.brokerverse.renewal.submitted;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Hand-offs of submitted policies. */
public interface SubmittedHandOffRecordRepository
    extends JpaRepository<SubmittedHandOffRecord, Long> {

  /**
   * The hand-off of a renewal candidate.
   *
   * @param candidateId candidate
   * @return hand-off
   */
  Optional<SubmittedHandOffRecord> findByCandidateId(Long candidateId);
}
