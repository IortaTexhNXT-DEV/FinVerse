package com.iortatechnxt.brokerverse.underwriting.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link Quotation}. */
public interface QuotationRepository extends JpaRepository<Quotation, Long> {

  /**
   * Loads a quotation with product, parties and iterations.
   *
   * @param id id
   * @return quotation
   */
  @EntityGraph(attributePaths = {"product", "customer", "intermediary", "iterations"})
  Optional<Quotation> findWithDetailsById(Long id);

  /**
   * Lists quotations issued in a date range, optionally in one status.
   *
   * @param companyId company
   * @param statuses statuses to include
   * @param from issue date from
   * @param to issue date to
   * @return quotations, newest first
   */
  @EntityGraph(attributePaths = {"product", "customer", "intermediary", "iterations"})
  @Query(
      """
      select q from Quotation q
      where q.companyId = :companyId and q.status in :statuses
        and q.issueDate between :from and :to
      order by q.issueDate desc, q.id desc
      """)
  List<Quotation> search(
      @Param("companyId") Long companyId,
      @Param("statuses") Collection<QuotationStatus> statuses,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);

  /**
   * Open quotations (candidates for expiry).
   *
   * @param companyId company
   * @param statuses open statuses
   * @return quotations
   */
  List<Quotation> findByCompanyIdAndStatusIn(Long companyId, Collection<QuotationStatus> statuses);

  /**
   * Quotations in one status across companies (approval inbox).
   *
   * @param status status
   * @return quotations with their product and iterations, oldest first
   */
  @EntityGraph(attributePaths = {"product", "iterations"})
  List<Quotation> findByStatusOrderById(QuotationStatus status);
}
