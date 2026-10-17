package com.iortatechnxt.brokerverse.organization.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link Branch}. */
public interface BranchRepository extends JpaRepository<Branch, Long> {

  /**
   * Lists the branches of a company ordered by code.
   *
   * @param companyId company id
   * @return branches
   */
  List<Branch> findByCompanyIdOrderByCode(Long companyId);

  /**
   * Finds a branch by company and code.
   *
   * @param companyId company id
   * @param code branch code
   * @return branch if present
   */
  Optional<Branch> findByCompanyIdAndCode(Long companyId, String code);

  /**
   * Checks whether a branch code is taken within a company.
   *
   * @param companyId company id
   * @param code branch code
   * @return true when taken
   */
  boolean existsByCompanyIdAndCode(Long companyId, String code);
}
