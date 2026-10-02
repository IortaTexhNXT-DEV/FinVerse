package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Limit rules. */
public interface SbmLimitRuleRepository extends JpaRepository<SbmLimitRule, Long> {

  /**
   * Limit rules of a company.
   *
   * @param companyId company
   * @return rules
   */
  List<SbmLimitRule> findByCompanyIdOrderByInsurerCodeAscIdAsc(Long companyId);

  /**
   * Rules of an insurer in a status.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param status ACTIVE
   * @return rules
   */
  List<SbmLimitRule> findByCompanyIdAndInsurerCodeAndRecordStatus(
      Long companyId, String insurerCode, RecordStatus status);
}
