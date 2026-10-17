package com.iortatechnxt.brokerverse.coa.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link GlCategory}. */
public interface GlCategoryRepository extends JpaRepository<GlCategory, Long> {

  /**
   * Finds a category by code.
   *
   * @param code code
   * @return category if present
   */
  Optional<GlCategory> findByCode(String code);

  /**
   * Lists categories ordered by code.
   *
   * @return categories
   */
  List<GlCategory> findAllByOrderByCode();
}
