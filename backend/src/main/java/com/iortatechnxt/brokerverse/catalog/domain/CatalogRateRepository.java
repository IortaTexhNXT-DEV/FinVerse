package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Tax and rating rates. */
public interface CatalogRateRepository extends JpaRepository<CatalogRate, Long> {

  /**
   * Rows of one tax or factor.
   *
   * @param rateCode tax or factor
   * @return rows
   */
  List<CatalogRate> findByRateCode(RateCode rateCode);

  /**
   * Every row.
   *
   * @return rows
   */
  List<CatalogRate> findAllByOrderByRateCodeAscLineCodeAscEffectiveFromDesc();
}
