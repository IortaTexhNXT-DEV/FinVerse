package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Incentive criteria (PMADD07/08). */
public interface IncentiveCriteriaRepository extends JpaRepository<IncentiveCriteria, Long> {

  /**
   * The criteria of a company, by code and effective date.
   *
   * @param companyId company
   * @return criteria
   */
  List<IncentiveCriteria> findByCompanyIdOrderByCodeAscEffectiveFromDesc(Long companyId);

  /**
   * Every row of one criterion code (history).
   *
   * @param companyId company
   * @param code code
   * @return rows, newest first
   */
  List<IncentiveCriteria> findByCompanyIdAndCodeOrderByEffectiveFromDesc(
      Long companyId, String code);

  /**
   * The rows of a company in a record status.
   *
   * @param companyId company
   * @param status record status
   * @return rows
   */
  List<IncentiveCriteria> findByCompanyIdAndRecordStatus(Long companyId, RecordStatus status);

  /**
   * The rows in a record status, every company.
   *
   * @param status record status
   * @return rows
   */
  List<IncentiveCriteria> findByRecordStatus(RecordStatus status);
}
