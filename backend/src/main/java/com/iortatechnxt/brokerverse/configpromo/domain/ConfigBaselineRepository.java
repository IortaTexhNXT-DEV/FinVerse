package com.iortatechnxt.brokerverse.configpromo.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Configuration baselines. */
public interface ConfigBaselineRepository extends JpaRepository<ConfigBaseline, Long> {

  /**
   * Every baseline, newest first.
   *
   * @return baselines
   */
  List<ConfigBaseline> findAllByOrderByIdDesc();

  /**
   * Whether a name is taken.
   *
   * @param name name
   * @return true when taken
   */
  boolean existsByNameIgnoreCase(String name);
}
