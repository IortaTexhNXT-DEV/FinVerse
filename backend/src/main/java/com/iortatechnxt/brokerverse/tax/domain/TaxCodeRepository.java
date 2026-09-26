package com.iortatechnxt.brokerverse.tax.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Tax codes. */
public interface TaxCodeRepository extends JpaRepository<TaxCode, Long> {

  /**
   * Tax codes of a company.
   *
   * @param companyId company
   * @return codes ordered by type and code
   */
  List<TaxCode> findByCompanyIdOrderByTaxTypeAscCodeAsc(Long companyId);

  /**
   * Finds a code.
   *
   * @param companyId company
   * @param code code
   * @return code
   */
  Optional<TaxCode> findByCompanyIdAndCode(Long companyId, String code);

  /**
   * Whether a code exists.
   *
   * @param companyId company
   * @param code code
   * @return true when present
   */
  boolean existsByCompanyIdAndCode(Long companyId, String code);
}
