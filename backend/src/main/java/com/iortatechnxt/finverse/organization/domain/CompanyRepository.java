package com.iortatechnxt.finverse.organization.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link Company}. */
public interface CompanyRepository extends JpaRepository<Company, Long> {

  /**
   * Finds a company by code.
   *
   * @param code company code
   * @return company if present
   */
  Optional<Company> findByCode(String code);

  /**
   * Checks whether a company code is taken.
   *
   * @param code company code
   * @return true when taken
   */
  boolean existsByCode(String code);
}
