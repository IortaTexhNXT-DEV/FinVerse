package com.iortatechnxt.brokerverse.lov.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** List values. */
public interface LovValueRepository extends JpaRepository<LovValue, Long> {

  /**
   * Values of a list in display order.
   *
   * @param typeCode list
   * @return values
   */
  List<LovValue> findByTypeCodeOrderBySortOrderAscLabelAsc(String typeCode);

  /**
   * One value.
   *
   * @param typeCode list
   * @param code code
   * @return value
   */
  Optional<LovValue> findByTypeCodeAndCode(String typeCode, String code);
}
