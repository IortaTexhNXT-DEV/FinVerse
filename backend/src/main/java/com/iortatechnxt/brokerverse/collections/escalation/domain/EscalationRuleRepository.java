package com.iortatechnxt.brokerverse.collections.escalation.domain;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Escalation rules (BRCLXN.049). */
public interface EscalationRuleRepository extends JpaRepository<EscalationRule, Long> {

  /**
   * Rules of a company by code.
   *
   * @param companyId company
   * @return rules
   */
  List<EscalationRule> findByCompanyIdOrderByCode(Long companyId);

  /**
   * A rule by code.
   *
   * @param companyId company
   * @param code code
   * @return rule
   */
  Optional<EscalationRule> findByCompanyIdAndCode(Long companyId, String code);

  /**
   * Rules in a record status, every company (job).
   *
   * @param status ACTIVE
   * @return rules
   */
  List<EscalationRule> findByRecordStatusOrderByCompanyIdAscCodeAsc(RecordStatus status);
}
