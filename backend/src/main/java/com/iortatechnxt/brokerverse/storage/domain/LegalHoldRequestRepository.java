package com.iortatechnxt.brokerverse.storage.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Legal hold requests. */
public interface LegalHoldRequestRepository extends JpaRepository<LegalHoldRequest, Long> {

  /**
   * Requests in a status, oldest first.
   *
   * @param status status
   * @return requests
   */
  List<LegalHoldRequest> findByStatusOrderByRequestedAtAsc(HoldRequestStatus status);

  /**
   * The requests of a file, newest first.
   *
   * @param storedFileId file
   * @return requests
   */
  List<LegalHoldRequest> findByStoredFileIdOrderByIdDesc(Long storedFileId);

  /**
   * Whether a file has a request in a status.
   *
   * @param storedFileId file
   * @param status status
   * @return true when present
   */
  boolean existsByStoredFileIdAndStatus(Long storedFileId, HoldRequestStatus status);
}
