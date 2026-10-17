package com.iortatechnxt.brokerverse.collections.unapplied.domain;

import com.iortatechnxt.brokerverse.collections.unapplied.domain.ApplicationRequest.Status;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Requests sent to Cashiering on collector dispositions (BRCLXN.030/032, 041). */
public interface ApplicationRequestRepository extends JpaRepository<ApplicationRequest, Long> {

  /**
   * The request of a source reference (Cashiering's answer).
   *
   * @param sourceRef reference sent to Cashiering
   * @return request
   */
  Optional<ApplicationRequest> findBySourceRef(String sourceRef);

  /**
   * Requests of an item, newest first.
   *
   * @param companyId company
   * @param unappliedRef item reference
   * @return requests
   */
  List<ApplicationRequest> findByCompanyIdAndUnappliedRefOrderByIdDesc(
      Long companyId, String unappliedRef);

  /**
   * Requests in some statuses, filtered by text, newest first (status view).
   *
   * @param companyId company
   * @param statuses statuses
   * @param text item, invoice, Cashiering reference or requester fragment (lower case), null for
   *     all
   * @param pageable page
   * @return requests
   */
  @Query(
      "select r from ApplicationRequest r where r.companyId = :companyId and r.status in :statuses"
          + " and (:text is null or lower(r.unappliedRef) like :text"
          + " or lower(coalesce(r.invoiceNo, '')) like :text"
          + " or lower(coalesce(r.cashieringRef, '')) like :text"
          + " or lower(r.requestedBy) like :text) order by r.id desc")
  Page<ApplicationRequest> search(
      @Param("companyId") Long companyId,
      @Param("statuses") Collection<Status> statuses,
      @Param("text") String text,
      Pageable pageable);

  /**
   * Requests of an action not yet listed in a file, made before a time (the daily file).
   *
   * @param companyId company
   * @param action APPLY_TO_INVOICE
   * @param before end of the period (exclusive)
   * @return requests, oldest first
   */
  List<ApplicationRequest>
      findByCompanyIdAndActionAndFileRunNoIsNullAndRequestedAtBeforeOrderByIdAsc(
          Long companyId, String action, Instant before);

  /**
   * Requests a user sent that are still open.
   *
   * @param companyId company
   * @param requestedBy user
   * @param statuses statuses
   * @return count
   */
  long countByCompanyIdAndRequestedByIgnoreCaseAndStatusIn(
      Long companyId, String requestedBy, Collection<Status> statuses);
}
