package com.iortatechnxt.brokerverse.tax.domain;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Received BIR 2307 certificates (DIS 2.11). */
public interface ReceivedCertificateRepository extends JpaRepository<ReceivedCertificate, Long> {

  /**
   * Searches certificates.
   *
   * @param companyId company
   * @param status status, null for all
   * @param q certificate, agent code or agent name contains (lower case pattern), null for all
   * @param pageable page
   * @return certificates
   */
  @Query(
      "select c from ReceivedCertificate c where c.companyId = :companyId"
          + " and (:status is null or c.status = :status)"
          + " and (:q is null or lower(c.certificateNo) like :q or lower(c.agentCode) like :q"
          + " or lower(c.agentName) like :q)")
  Page<ReceivedCertificate> search(
      @Param("companyId") Long companyId,
      @Param("status") String status,
      @Param("q") String q,
      Pageable pageable);

  /**
   * Whether a live certificate with the same number exists for an agent.
   *
   * @param companyId company
   * @param agentCode agent
   * @param certificateNo number
   * @param status status excluded
   * @return true when one exists
   */
  boolean existsByCompanyIdAndAgentCodeAndCertificateNoAndStatusNot(
      Long companyId, String agentCode, String certificateNo, String status);

  /**
   * The live certificates whose period ends in a range (SAWT, 1702).
   *
   * @param companyId company
   * @param from first day
   * @param to last day
   * @param status status
   * @return certificates by agent
   */
  List<ReceivedCertificate> findByCompanyIdAndPeriodToBetweenAndStatusOrderByAgentCodeAsc(
      Long companyId, LocalDate from, LocalDate to, String status);

  /**
   * The certificates recorded from a source record (a DV, a commission submission).
   *
   * @param sourceModule module
   * @param sourceRef reference
   * @return certificates
   */
  List<ReceivedCertificate> findBySourceModuleAndSourceRef(String sourceModule, String sourceRef);
}
