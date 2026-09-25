package com.iortatechnxt.brokerverse.frbs.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Service-fee rules (FRBS 2.10.0). */
public interface ServiceFeeRuleRepository extends JpaRepository<ServiceFeeRule, Long> {

  /**
   * Every rule, by segment and start date.
   *
   * @return rules
   */
  List<ServiceFeeRule> findAllByOrderBySegmentAscEffectiveFromAsc();
}
