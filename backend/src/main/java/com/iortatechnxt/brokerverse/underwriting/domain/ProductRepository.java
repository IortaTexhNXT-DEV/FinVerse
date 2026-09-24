package com.iortatechnxt.brokerverse.underwriting.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link Product}. */
public interface ProductRepository extends JpaRepository<Product, Long> {

  /**
   * Lists a company's products.
   *
   * @param companyId company
   * @return products ordered by code
   */
  List<Product> findByCompanyIdOrderByCode(Long companyId);

  /**
   * Finds a product by code.
   *
   * @param companyId company
   * @param code code
   * @return product if present
   */
  Optional<Product> findByCompanyIdAndCode(Long companyId, String code);

  /**
   * Checks whether a code is taken.
   *
   * @param companyId company
   * @param code code
   * @return true when it exists
   */
  boolean existsByCompanyIdAndCode(Long companyId, String code);
}
