package com.iortatechnxt.brokerverse.finreport.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Account schedule definitions (FRBS 3.2.0). */
public interface ScheduleDefinitionRepository extends JpaRepository<ScheduleDefinition, Long> {

  /**
   * A definition by code.
   *
   * @param code code
   * @return definition
   */
  Optional<ScheduleDefinition> findByCode(String code);

  /**
   * Whether a code is taken.
   *
   * @param code code
   * @return true when taken
   */
  boolean existsByCode(String code);

  /**
   * Every definition in code order.
   *
   * @return definitions
   */
  List<ScheduleDefinition> findAllByOrderByCodeAsc();
}
