package com.iortatechnxt.finverse.reinsurance.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Cessions (one per premium transaction). */
public interface CessionRepository extends JpaRepository<Cession, Long> {

  /**
   * Cession of a transaction.
   *
   * @param companyId company
   * @param policyId policy
   * @param endorsementNo endorsement number (0 = original issue)
   * @return cession
   */
  @EntityGraph(attributePaths = "lines")
  Optional<Cession> findByCompanyIdAndPolicyIdAndEndorsementNo(
      Long companyId, Long policyId, int endorsementNo);

  /**
   * Cession with its lines.
   *
   * @param id id
   * @return cession
   */
  @EntityGraph(attributePaths = "lines")
  Optional<Cession> findWithLinesById(Long id);

  /**
   * Cessions of a policy in endorsement order, with lines.
   *
   * @param policyId policy
   * @return cessions
   */
  @EntityGraph(attributePaths = "lines")
  List<Cession> findByPolicyIdOrderByEndorsementNo(Long policyId);

  /**
   * Cessions of several policies, with lines (bulk read for the underwriting registers).
   *
   * @param policyIds policies
   * @return cessions
   */
  @EntityGraph(attributePaths = "lines")
  List<Cession> findByPolicyIdIn(Collection<Long> policyIds);

  /**
   * Cessions with an RI accounting date in a period, with lines.
   *
   * @param companyId company
   * @param from first date
   * @param to last date
   * @return cessions ordered by date and number
   */
  @EntityGraph(attributePaths = "lines")
  List<Cession> findByCompanyIdAndRiDateBetweenOrderByRiDateAscCessionNoAsc(
      Long companyId, LocalDate from, LocalDate to);

  /**
   * Keys of the transactions of a company already ceded.
   *
   * @param companyId company
   * @return keys
   */
  @Query(
      "select new com.iortatechnxt.finverse.reinsurance.domain.CessionKey(c.policyId,"
          + " c.endorsementNo) from Cession c where c.companyId = :companyId")
  List<CessionKey> cededKeys(@Param("companyId") Long companyId);

  /**
   * Full (risk by risk) allocations of a policy, most recent cover first.
   *
   * @param policyId policy
   * @return cessions with lines
   */
  @EntityGraph(attributePaths = "lines")
  @Query(
      "select c from Cession c where c.policyId = :policyId"
          + " and c.basis = com.iortatechnxt.finverse.reinsurance.domain.CessionBasis.FULL"
          + " order by c.effectiveDate desc, c.endorsementNo desc")
  List<Cession> fullAllocations(@Param("policyId") Long policyId);
}
