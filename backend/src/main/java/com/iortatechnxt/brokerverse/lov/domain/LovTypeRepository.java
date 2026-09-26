package com.iortatechnxt.brokerverse.lov.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** List types. */
public interface LovTypeRepository extends JpaRepository<LovType, Long> {

  /**
   * Finds a type by code.
   *
   * @param code code
   * @return type
   */
  Optional<LovType> findByCode(String code);

  /**
   * All types by name.
   *
   * @return types
   */
  List<LovType> findAllByOrderByName();
}
