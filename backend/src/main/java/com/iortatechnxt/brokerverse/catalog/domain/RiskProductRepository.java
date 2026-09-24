package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Risk products. */
public interface RiskProductRepository
    extends JpaRepository<RiskProduct, Long>, JpaSpecificationExecutor<RiskProduct> {

  /**
   * A product by risk code.
   *
   * @param code risk code
   * @return product
   */
  Optional<RiskProduct> findByCode(String code);
}
