package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Threshold rules. */
public interface EbThresholdRuleRepository extends JpaRepository<EbThresholdRule, Long> {

  /**
   * Rules of a company.
   *
   * @param companyId company
   * @return rules
   */
  List<EbThresholdRule> findByCompanyIdOrderByIdAsc(Long companyId);

  /**
   * Rules in a record status (approval inbox).
   *
   * @param status status
   * @return rules
   */
  List<EbThresholdRule> findByRecordStatus(RecordStatus status);
}
