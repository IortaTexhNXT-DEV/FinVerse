package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Short-period table. */
public interface ShortPeriodRateRepository extends JpaRepository<ShortPeriodRate, Long> {

  /**
   * Every row.
   *
   * @return rows
   */
  List<ShortPeriodRate> findAllByOrderByMonthsCoveredAscEffectiveFromDesc();
}
