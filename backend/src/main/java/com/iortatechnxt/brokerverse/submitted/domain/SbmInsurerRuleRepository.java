package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Insurer assignment rules. */
public interface SbmInsurerRuleRepository extends JpaRepository<SbmInsurerRule, Long> {

  /**
   * Rules of a company.
   *
   * @param companyId company
   * @return rules
   */
  List<SbmInsurerRule> findByCompanyIdOrderBySegmentAscPriorityDesc(Long companyId);

  /**
   * Rules of a segment in a status, highest priority first.
   *
   * @param companyId company
   * @param segment segment
   * @param status ACTIVE
   * @return rules
   */
  List<SbmInsurerRule> findByCompanyIdAndSegmentAndRecordStatusOrderByPriorityDesc(
      Long companyId, String segment, RecordStatus status);
}
