package com.iortatechnxt.brokerverse.dimension.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link DimensionValue}. */
public interface DimensionValueRepository extends JpaRepository<DimensionValue, Long> {

  /**
   * Lists values of a dimension.
   *
   * @param companyId company
   * @param type dimension type
   * @return values ordered by code
   */
  List<DimensionValue> findByCompanyIdAndTypeOrderByCode(Long companyId, DimensionType type);

  /**
   * Finds a value.
   *
   * @param companyId company
   * @param type dimension type
   * @param code code
   * @return value if present
   */
  Optional<DimensionValue> findByCompanyIdAndTypeAndCode(
      Long companyId, DimensionType type, String code);
}
