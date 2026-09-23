package com.iortatechnxt.finverse.fixedasset.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link AssetCategory}. */
public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {

  /**
   * Lists the categories of a company.
   *
   * @param companyId company
   * @return categories ordered by code
   */
  List<AssetCategory> findByCompanyIdOrderByCode(Long companyId);

  /**
   * Finds a category by code.
   *
   * @param companyId company
   * @param code code
   * @return category if present
   */
  Optional<AssetCategory> findByCompanyIdAndCode(Long companyId, String code);

  /**
   * Checks whether a code is taken.
   *
   * @param companyId company
   * @param code code
   * @return true when taken
   */
  boolean existsByCompanyIdAndCode(Long companyId, String code);
}
