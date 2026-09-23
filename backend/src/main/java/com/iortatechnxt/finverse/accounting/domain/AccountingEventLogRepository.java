package com.iortatechnxt.finverse.accounting.domain;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence and search for {@link AccountingEventLog}. */
public interface AccountingEventLogRepository extends JpaRepository<AccountingEventLog, Long> {

  /**
   * Searches the event register.
   *
   * @param companyId company
   * @param status status or null
   * @param eventType event type or null
   * @param from value date from
   * @param to value date to
   * @param pageable paging
   * @return events
   */
  @Query(
      """
      select e from AccountingEventLog e
      where e.companyId = :companyId
        and (:status is null or e.status = :status)
        and (:eventType is null or e.eventType = :eventType)
        and e.valueDate between :from and :to
      """)
  Page<AccountingEventLog> search(
      @Param("companyId") Long companyId,
      @Param("status") EventStatus status,
      @Param("eventType") String eventType,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to,
      Pageable pageable);
}
