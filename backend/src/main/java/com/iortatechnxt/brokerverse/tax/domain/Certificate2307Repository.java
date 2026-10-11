package com.iortatechnxt.brokerverse.tax.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** BIR Form 2307 certificates. */
public interface Certificate2307Repository extends JpaRepository<Certificate2307, Long> {

  /**
   * Loads a certificate with its lines and batch.
   *
   * @param id id
   * @return certificate
   */
  @EntityGraph(
      type = EntityGraph.EntityGraphType.LOAD,
      attributePaths = {"lines", "batch"})
  Optional<Certificate2307> findWithLinesById(Long id);

  /**
   * Certificates of a quarter.
   *
   * @param companyId company
   * @param periodStart quarter start
   * @return certificates ordered by payee
   */
  @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = "batch")
  List<Certificate2307> findByCompanyIdAndPeriodStartOrderByPartyCode(
      Long companyId, LocalDate periodStart);

  /**
   * Certificates of a batch with their lines.
   *
   * @param batchId batch
   * @return certificates ordered by number
   */
  @EntityGraph(
      type = EntityGraph.EntityGraphType.LOAD,
      attributePaths = {"lines", "batch"})
  List<Certificate2307> findByBatchIdOrderByCertificateNo(Long batchId);

  /**
   * Certificates whose quarter starts in a date range.
   *
   * @param companyId company
   * @param from quarter start from
   * @param to quarter start to
   * @return certificates ordered by quarter and number
   */
  @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = "batch")
  List<Certificate2307> findByCompanyIdAndPeriodStartBetweenOrderByPeriodStartAscCertificateNoAsc(
      Long companyId, LocalDate from, LocalDate to);

  /**
   * Whether a payee already holds an issued certificate for a quarter.
   *
   * @param companyId company
   * @param partyCode payee
   * @param periodStart quarter start
   * @param status status
   * @return true when present
   */
  boolean existsByCompanyIdAndPartyCodeAndPeriodStartAndStatus(
      Long companyId, String partyCode, LocalDate periodStart, CertificateStatus status);
}
