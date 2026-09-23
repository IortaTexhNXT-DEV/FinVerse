package com.iortatechnxt.finverse.alert.domain;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence and search for {@link Alert}. */
public interface AlertRepository extends JpaRepository<Alert, Long> {

  /**
   * Checks whether a live alert exists for a condition.
   *
   * @param dedupKey condition key
   * @param status status to exclude (RESOLVED)
   * @return true when an open or acknowledged alert exists
   */
  boolean existsByDedupKeyAndStatusNot(String dedupKey, AlertStatus status);

  /**
   * Searches alerts. Null filters are ignored.
   *
   * @param status status filter
   * @param severity severity filter
   * @param code exception code filter
   * @param companyId company filter
   * @param from inclusive lower bound of the raise time
   * @param to exclusive upper bound of the raise time
   * @param pageable paging
   * @return alerts, newest first
   */
  @Query(
      """
      select a from Alert a
      where (:status is null or a.status = :status)
        and (:severity is null or a.severity = :severity)
        and (:code is null or a.exceptionCode = :code)
        and (:companyId is null or a.companyId = :companyId)
        and a.raisedAt >= :from and a.raisedAt < :to
      order by a.raisedAt desc, a.id desc
      """)
  Page<Alert> search(
      @Param("status") AlertStatus status,
      @Param("severity") AlertSeverity severity,
      @Param("code") String code,
      @Param("companyId") Long companyId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      Pageable pageable);

  /**
   * Live alerts per severity.
   *
   * @param resolved status to exclude (RESOLVED)
   * @return counts
   */
  @Query(
      "select a.severity as severity, count(a) as total from Alert a"
          + " where a.status <> :resolved group by a.severity")
  List<SeverityCount> countLiveBySeverity(@Param("resolved") AlertStatus resolved);

  /** Projection of {@link #countLiveBySeverity}. */
  interface SeverityCount {

    /**
     * Severity.
     *
     * @return severity
     */
    AlertSeverity getSeverity();

    /**
     * Number of live alerts.
     *
     * @return count
     */
    long getTotal();
  }
}
