package com.iortatechnxt.brokerverse.system.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link ProductModuleSwitch}. */
public interface ProductModuleSwitchRepository extends JpaRepository<ProductModuleSwitch, Long> {

  /**
   * Finds the switch of a module.
   *
   * @param code module code
   * @return switch if present
   */
  Optional<ProductModuleSwitch> findByCode(String code);

  /**
   * All switches in display order.
   *
   * @return switches
   */
  List<ProductModuleSwitch> findAllByOrderBySortOrderAsc();
}
