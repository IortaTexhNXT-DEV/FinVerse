package com.iortatechnxt.brokerverse.placement.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Payment reports. */
public interface PaymentReportRepository extends JpaRepository<PaymentReport, Long> {

  /**
   * Reports of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return reports
   */
  Page<PaymentReport> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);

  /**
   * Lines of confirmed reports matched as paid to the given accounts (payment confirmation source).
   *
   * @param arns accounts
   * @param confirmed confirmed status
   * @param matched matched status
   * @return lines
   */
  @Query(
      "select l from PaymentReportLine l join fetch l.report r where l.arn in :arns"
          + " and r.status = :confirmed and l.matchStatus = :matched order by l.id")
  List<PaymentReportLine> confirmedLines(
      @Param("arns") Collection<String> arns,
      @Param("confirmed") PaymentReportStatus confirmed,
      @Param("matched") MatchStatus matched);
}
