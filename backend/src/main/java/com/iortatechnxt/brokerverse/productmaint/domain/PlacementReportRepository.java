package com.iortatechnxt.brokerverse.productmaint.domain;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Generated Consolidated Placement Update Reports. */
public interface PlacementReportRepository extends JpaRepository<PlacementReport, Long> {

  Page<PlacementReport> findByCompanyIdOrderByReportDateDescIdDesc(Long companyId, Pageable page);

  boolean existsByCompanyIdAndReportDateAndTriggerType(
      Long companyId, LocalDate reportDate, String triggerType);
}
