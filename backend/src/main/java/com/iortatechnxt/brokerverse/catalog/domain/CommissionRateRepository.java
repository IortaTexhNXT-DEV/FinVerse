package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Commission rates. */
public interface CommissionRateRepository extends JpaRepository<CommissionRate, Long> {

  /**
   * Rates of an insurer.
   *
   * @param companyId company
   * @param insurerCode insurer party code
   * @return rates
   */
  List<CommissionRate> findByCompanyIdAndInsurerCodeOrderByProductCodeAscEffectiveFromDesc(
      Long companyId, String insurerCode);
}
