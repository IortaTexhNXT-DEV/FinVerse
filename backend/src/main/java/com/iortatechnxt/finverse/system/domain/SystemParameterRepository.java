package com.iortatechnxt.finverse.system.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link SystemParameter}. */
public interface SystemParameterRepository extends JpaRepository<SystemParameter, Long> {

  /**
   * Finds a parameter by key.
   *
   * @param key parameter key
   * @return parameter if present
   */
  Optional<SystemParameter> findByKey(String key);

  /**
   * Lists all parameters grouped by category.
   *
   * @return parameters
   */
  List<SystemParameter> findAllByOrderByCategoryAscKeyAsc();
}
