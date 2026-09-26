package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CollectorRequest.Status;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Collector disposition requests (BRCLXN.030-033). */
public interface CollectorRequestRepository extends JpaRepository<CollectorRequest, Long> {

  /**
   * The request of a source (idempotency of the port).
   *
   * @param source requesting module
   * @param sourceRef its reference
   * @return request
   */
  Optional<CollectorRequest> findBySourceAndSourceRef(String source, String sourceRef);

  /**
   * Requests of an item, newest first.
   *
   * @param unappliedId item
   * @return requests
   */
  List<CollectorRequest> findByUnappliedIdOrderByIdDesc(Long unappliedId);

  /**
   * The request a disposition was assigned from.
   *
   * @param dispositionId disposition
   * @return request
   */
  Optional<CollectorRequest> findByDispositionId(Long dispositionId);

  /**
   * Requests of a company in some statuses, filtered by text, newest first.
   *
   * @param companyId company
   * @param statuses statuses
   * @param text request, invoice or requester fragment (lower case), null for all
   * @param pageable page
   * @return requests
   */
  @Query(
      "select r from CollectorRequest r where r.companyId = :companyId and r.status in :statuses"
          + " and (:text is null or lower(r.requestNo) like :text"
          + " or lower(coalesce(r.invoiceNo, '')) like :text or lower(r.requestedBy) like :text)"
          + " order by r.id desc")
  Page<CollectorRequest> search(
      @Param("companyId") Long companyId,
      @Param("statuses") Collection<Status> statuses,
      @Param("text") String text,
      Pageable pageable);

  /**
   * Count per status.
   *
   * @param companyId company
   * @param status status
   * @return count
   */
  long countByCompanyIdAndStatus(Long companyId, Status status);
}
