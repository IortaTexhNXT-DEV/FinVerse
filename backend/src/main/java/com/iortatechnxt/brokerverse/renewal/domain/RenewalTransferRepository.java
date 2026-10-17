package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Transfers between Marketing units. */
public interface RenewalTransferRepository extends JpaRepository<RenewalTransfer, Long> {

  /**
   * The open request of a candidate.
   *
   * @param candidateId candidate
   * @param status REQUESTED
   * @return request
   */
  Optional<RenewalTransfer> findFirstByCandidateIdAndStatus(
      Long candidateId, TransferStatus status);

  /**
   * Requests to units.
   *
   * @param units receiving units
   * @return requests, newest first
   */
  List<RenewalTransfer> findByToUnitInOrderByIdDesc(Collection<String> units);

  /**
   * Requests from units.
   *
   * @param units sending units
   * @return requests, newest first
   */
  List<RenewalTransfer> findByFromUnitInOrderByIdDesc(Collection<String> units);

  /**
   * Requests of a candidate, newest first.
   *
   * @param candidateId candidate
   * @return requests
   */
  List<RenewalTransfer> findByCandidateIdOrderByIdDesc(Long candidateId);

  /**
   * Requests made by a user.
   *
   * @param createdBy requester
   * @return requests, newest first
   */
  List<RenewalTransfer> findByCreatedByOrderByIdDesc(String createdBy);
}
