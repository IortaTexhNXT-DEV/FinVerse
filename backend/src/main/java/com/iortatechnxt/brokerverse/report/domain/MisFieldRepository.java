package com.iortatechnxt.brokerverse.report.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** MIS fields of the catalogue (V2912). */
public interface MisFieldRepository extends JpaRepository<MisField, Long> {

  /**
   * Every field, by entity and code.
   *
   * @return fields
   */
  List<MisField> findAllByOrderByEntityAscCodeAsc();

  /**
   * The field of a code.
   *
   * @param code code
   * @return field
   */
  Optional<MisField> findByCode(String code);
}
