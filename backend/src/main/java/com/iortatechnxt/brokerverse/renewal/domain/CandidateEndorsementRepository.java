package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Endorsements linked to renewals. */
public interface CandidateEndorsementRepository extends JpaRepository<CandidateEndorsement, Long> {

  /**
   * Endorsements of a candidate.
   *
   * @param candidateId candidate
   * @return endorsements
   */
  List<CandidateEndorsement> findByCandidateIdOrderByIdAsc(Long candidateId);

  /**
   * One endorsement of a candidate.
   *
   * @param candidateId candidate
   * @param reference endorsement or request number
   * @return endorsement
   */
  Optional<CandidateEndorsement> findByCandidateIdAndReference(Long candidateId, String reference);
}
