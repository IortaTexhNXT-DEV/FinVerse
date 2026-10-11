package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** The source register. */
public interface SbmSourceRepository extends JpaRepository<SbmSource, Long> {

  /**
   * A source by code.
   *
   * @param code code
   * @return source
   */
  Optional<SbmSource> findByCode(String code);

  /**
   * Every source by code.
   *
   * @return sources
   */
  List<SbmSource> findAllByOrderByCodeAsc();
}
