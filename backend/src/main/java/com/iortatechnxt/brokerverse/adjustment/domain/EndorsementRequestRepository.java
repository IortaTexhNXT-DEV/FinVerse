package com.iortatechnxt.brokerverse.adjustment.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Endorsement requests (ADJID.001-025). */
public interface EndorsementRequestRepository extends JpaRepository<EndorsementRequest, Long> {

  /**
   * A request by number.
   *
   * @param requestNo request number
   * @return request
   */
  Optional<EndorsementRequest> findByRequestNo(String requestNo);

  /**
   * Requests raised on an invoice, newest first.
   *
   * @param invoiceNo invoice number
   * @return requests
   */
  List<EndorsementRequest> findBySubjectInvoiceNoOrderByIdDesc(String invoiceNo);

  /**
   * The request whose posting booked an endorsement or return invoice.
   *
   * @param invoiceNo invoice booked by the posting
   * @return request
   */
  Optional<EndorsementRequest> findFirstByOutcomeNewInvoiceNo(String invoiceNo);

  /**
   * Requests of a company in some stages, oldest first (posting batches, jobs).
   *
   * @param companyId company
   * @param stages stages
   * @return requests
   */
  List<EndorsementRequest> findByCompanyIdAndStageInOrderByIdAsc(
      Long companyId, Collection<RequestStage> stages);

  /**
   * Requests of a posting batch.
   *
   * @param batchNo validation batch number
   * @return requests
   */
  List<EndorsementRequest> findByOutcomeBatchNoOrderByIdAsc(String batchNo);

  /**
   * Number of requests of a company in a stage (work tiles).
   *
   * @param companyId company
   * @param stage stage
   * @return count
   */
  long countByCompanyIdAndStage(Long companyId, RequestStage stage);

  /**
   * Searches requests: stage, and text on request number, invoice, ARN, policy, assured or
   * endorsement reference.
   *
   * @param companyId company
   * @param stage stage, null for all
   * @param text lower-case text with wildcards ({@code %} for all)
   * @param pageable page
   * @return requests
   */
  @Query(
      """
      select r from EndorsementRequest r
      where r.companyId = :companyId
        and (:stage is null or r.stage = :stage)
        and (lower(r.requestNo) like :text
             or lower(r.subject.invoiceNo) like :text
             or lower(r.subject.arn) like :text
             or lower(coalesce(r.subject.policyNo, '')) like :text
             or lower(r.subject.assuredName) like :text
             or lower(coalesce(r.terms.endorsementRef, '')) like :text)
      """)
  Page<EndorsementRequest> search(
      @Param("companyId") Long companyId,
      @Param("stage") RequestStage stage,
      @Param("text") String text,
      Pageable pageable);

  /**
   * Requests created in a period (reports).
   *
   * @param companyId company
   * @param from from (inclusive)
   * @param to to (exclusive)
   * @return requests, oldest first
   */
  @Query(
      """
      select r from EndorsementRequest r
      where r.companyId = :companyId and r.createdAt >= :from and r.createdAt < :to
      order by r.id
      """)
  List<EndorsementRequest> createdBetween(
      @Param("companyId") Long companyId, @Param("from") Instant from, @Param("to") Instant to);

  /**
   * Requests posted in a period (validation list, daily report).
   *
   * @param companyId company
   * @param from from (inclusive)
   * @param to to (exclusive)
   * @return requests, oldest first
   */
  @Query(
      """
      select r from EndorsementRequest r
      where r.companyId = :companyId and r.trail.postedAt >= :from and r.trail.postedAt < :to
      order by r.trail.postedAt, r.id
      """)
  List<EndorsementRequest> postedBetween(
      @Param("companyId") Long companyId, @Param("from") Instant from, @Param("to") Instant to);
}
