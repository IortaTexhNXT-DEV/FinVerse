package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** The clause library (PMADD02). */
public interface ClauseRepository extends JpaRepository<Clause, Long> {

  /**
   * A clause by code.
   *
   * @param code code
   * @return clause
   */
  Optional<Clause> findByCode(String code);

  /**
   * Every clause by code.
   *
   * @return clauses
   */
  List<Clause> findAllByOrderByCodeAsc();
}
