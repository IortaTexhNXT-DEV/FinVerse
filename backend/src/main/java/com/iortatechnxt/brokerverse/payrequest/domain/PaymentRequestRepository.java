package com.iortatechnxt.brokerverse.payrequest.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Marketing requests (MKT 1.2.0-2.26.0). */
public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {

  /**
   * A request with its lines.
   *
   * @param id id
   * @return request
   */
  @EntityGraph(attributePaths = "lines")
  @Query("select r from PaymentRequest r where r.id = :id")
  Optional<PaymentRequest> findLoaded(@Param("id") Long id);

  /**
   * A request by number.
   *
   * @param requestNo request number
   * @return request
   */
  @EntityGraph(attributePaths = "lines")
  Optional<PaymentRequest> findByRequestNo(String requestNo);

  /**
   * Work list search (MKT 1.3.0, 1.6.0, 1.18.0): stage, kind, request date range and a text on the
   * request number, payee, reference or DV number.
   *
   * @param companyId company
   * @param stage stage or null
   * @param kind kind or null
   * @param from first request date or null
   * @param to last request date or null
   * @param q lower-case like pattern ({@code %} for all)
   * @param pageable page
   * @return requests
   */
  @Query(
      """
      select r from PaymentRequest r
      where r.companyId = :companyId
        and (:stage is null or r.stage = :stage)
        and (:kind is null or r.kind = :kind)
        and (cast(:fromDate as LocalDate) is null or r.requestDate >= :fromDate)
        and (cast(:toDate as LocalDate) is null or r.requestDate <= :toDate)
        and (lower(r.requestNo) like :q
             or lower(r.payee.name) like :q
             or lower(r.payee.code) like :q
             or lower(coalesce(r.content.referenceText, '')) like :q
             or lower(coalesce(r.track.dvNo, '')) like :q)
      """)
  Page<PaymentRequest> search(
      @Param("companyId") Long companyId,
      @Param("stage") RequestStage stage,
      @Param("kind") RequestKind kind,
      @Param("fromDate") LocalDate from,
      @Param("toDate") LocalDate to,
      @Param("q") String q,
      Pageable pageable);

  /**
   * Requests per stage (work list tabs).
   *
   * @param companyId company
   * @return stage and count
   */
  @Query(
      "select r.stage, count(r) from PaymentRequest r where r.companyId = :companyId"
          + " group by r.stage")
  List<Object[]> countByStage(@Param("companyId") Long companyId);

  /**
   * Requests of a period by request date (reports).
   *
   * @param companyId company
   * @param from first date
   * @param to last date
   * @return requests, oldest first
   */
  @EntityGraph(attributePaths = "lines")
  List<PaymentRequest> findByCompanyIdAndRequestDateBetweenOrderByRequestDateAscIdAsc(
      Long companyId, LocalDate from, LocalDate to);

  /**
   * Live check cancellations of a paid request (one at a time, MKT 1.19.0).
   *
   * @param targetRequestNo paid request
   * @param ended stages that no longer block
   * @return count
   */
  @Query(
      "select count(r) from PaymentRequest r where r.target.requestNo = :target"
          + " and r.stage not in :ended")
  long countLiveCancellations(
      @Param("target") String targetRequestNo, @Param("ended") List<RequestStage> ended);

  /**
   * Requests waiting in some stages, oldest first (approval inbox).
   *
   * @param stages stages
   * @return requests
   */
  List<PaymentRequest> findByStageInOrderByIdAsc(Collection<RequestStage> stages);
}
