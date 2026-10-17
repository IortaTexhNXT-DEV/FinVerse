package com.iortatechnxt.brokerverse.screening.str.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Suspicious transaction reports (SNSRP-705, 706). */
public interface StrRepository extends JpaRepository<SuspiciousTransactionReport, Long> {

  /**
   * The STR of a case.
   *
   * @param caseId case
   * @return the STR
   */
  Optional<SuspiciousTransactionReport> findByCaseId(Long caseId);

  /**
   * Whether an AMLC reference is used (FR-SS-072 R1).
   *
   * @param reference reference
   * @return true when used
   */
  boolean existsByAmlcReferenceIgnoreCase(String reference);

  /**
   * The STR register of a company.
   *
   * @param companyId company
   * @param statuses statuses
   * @param pageable page
   * @return STRs, newest first
   */
  @Query(
      "select s from SuspiciousTransactionReport s where s.companyId = :companyId"
          + " and s.status in :statuses order by s.id desc")
  Page<SuspiciousTransactionReport> register(
      @Param("companyId") Long companyId,
      @Param("statuses") Collection<StrStatus> statuses,
      Pageable pageable);

  /**
   * STRs of a company in statuses whose committee decision falls in a period (FR-SS-071 R1).
   *
   * @param companyId company
   * @param statuses APPROVED (and EXTRACTED for a re-extraction)
   * @param from period start (inclusive)
   * @param to period end (exclusive)
   * @return STRs by number
   */
  @Query(
      "select s from SuspiciousTransactionReport s where s.companyId = :companyId"
          + " and s.status in :statuses and s.committeeDecidedAt >= :from"
          + " and s.committeeDecidedAt < :to order by s.strNo")
  List<SuspiciousTransactionReport> decidedIn(
      @Param("companyId") Long companyId,
      @Param("statuses") Collection<StrStatus> statuses,
      @Param("from") Instant from,
      @Param("to") Instant to);
}
