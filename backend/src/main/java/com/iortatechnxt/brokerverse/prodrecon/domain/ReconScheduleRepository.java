package com.iortatechnxt.brokerverse.prodrecon.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Extraction schedules (PRCID.001). */
public interface ReconScheduleRepository extends JpaRepository<ReconSchedule, Long> {

  /**
   * Schedules of a company by insurer.
   *
   * @param companyId company
   * @return schedules
   */
  List<ReconSchedule> findByCompanyIdOrderByInsurerCodeAsc(Long companyId);

  /**
   * The schedule of an insurer.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @return schedule
   */
  Optional<ReconSchedule> findByCompanyIdAndInsurerCode(Long companyId, String insurerCode);

  /**
   * Active schedules due on or before a date.
   *
   * @param date business date
   * @return schedules
   */
  List<ReconSchedule> findByActiveTrueAndNextRunDateLessThanEqualOrderByIdAsc(LocalDate date);
}
