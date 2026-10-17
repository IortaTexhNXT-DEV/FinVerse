package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Letter rules. */
public interface SbmLetterRuleRepository extends JpaRepository<SbmLetterRule, Long> {

  /**
   * Rules of a company.
   *
   * @param companyId company
   * @return rules
   */
  List<SbmLetterRule> findByCompanyIdOrderByLetterTypeAscIdAsc(Long companyId);

  /**
   * Rules in a status.
   *
   * @param companyId company
   * @param status ACTIVE
   * @return rules
   */
  List<SbmLetterRule> findByCompanyIdAndRecordStatus(Long companyId, RecordStatus status);
}
