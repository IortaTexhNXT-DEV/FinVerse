package com.iortatechnxt.brokerverse.migration.mapping.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Code map sets. */
public interface CodeMapSetRepository extends JpaRepository<CodeMapSet, Long> {

  /**
   * A set by code.
   *
   * @param code set code
   * @return set
   */
  Optional<CodeMapSet> findByCode(String code);

  /**
   * Every set by code.
   *
   * @return sets
   */
  List<CodeMapSet> findAllByOrderByCodeAsc();
}
