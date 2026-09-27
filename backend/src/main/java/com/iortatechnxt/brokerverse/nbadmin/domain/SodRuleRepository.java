package com.iortatechnxt.brokerverse.nbadmin.domain;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Separation-of-duties rules (V1065). */
public interface SodRuleRepository extends JpaRepository<SodRule, Long> {

  /**
   * Rules in a status.
   *
   * @param status record status
   * @return rules
   */
  List<SodRule> findByRecordStatus(RecordStatus status);

  /**
   * Every rule, by number.
   *
   * @return rules
   */
  List<SodRule> findAllByOrderByRuleCodeAsc();
}
