package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** LAMD reports. */
public interface LamdReportRepository extends JpaRepository<LamdReport, Long> {

  /**
   * The report of an upload job.
   *
   * @param jobNo job
   * @return report
   */
  Optional<LamdReport> findByJobNo(String jobNo);

  /**
   * Reports of a company, newest first.
   *
   * @param companyId company
   * @return reports
   */
  List<LamdReport> findByCompanyIdOrderByIdDesc(Long companyId);
}
