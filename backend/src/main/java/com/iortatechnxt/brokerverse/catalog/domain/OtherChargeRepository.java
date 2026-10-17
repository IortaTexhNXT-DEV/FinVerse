package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Other charges billed with the premium. */
public interface OtherChargeRepository extends JpaRepository<OtherCharge, Long> {

  /**
   * Every row, by charge, scope and newest first.
   *
   * @return rows
   */
  List<OtherCharge> findAllByOrderByChargeCodeAscLineCodeAscProductCodeAscEffectiveFromDesc();
}
