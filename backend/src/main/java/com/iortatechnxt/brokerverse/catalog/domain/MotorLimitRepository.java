package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Motor BI / PD limit tables. */
public interface MotorLimitRepository extends JpaRepository<MotorLimit, Long> {

  /**
   * Every row.
   *
   * @return rows
   */
  List<MotorLimit> findAllByOrderByCoverageAscLimitAmountAscEffectiveFromDesc();
}
