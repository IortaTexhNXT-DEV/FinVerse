package com.iortatechnxt.brokerverse.closing.domain;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link PeriodCloseSchedule}. */
public interface PeriodCloseScheduleRepository extends JpaRepository<PeriodCloseSchedule, Long> {

  /**
   * Schedules of a company, newest first.
   *
   * @param companyId company
   * @return schedules
   */
  List<PeriodCloseSchedule> findByCompanyIdOrderByScheduledAtDescIdDesc(Long companyId);

  /**
   * Whether a period already has an active schedule.
   *
   * @param periodId period
   * @param status status
   * @return true when present
   */
  boolean existsByPeriodIdAndStatus(Long periodId, String status);

  /**
   * Schedules in a status due by a time, oldest first.
   *
   * @param status status
   * @param time time
   * @return schedules
   */
  List<PeriodCloseSchedule> findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
      String status, Instant time);
}
