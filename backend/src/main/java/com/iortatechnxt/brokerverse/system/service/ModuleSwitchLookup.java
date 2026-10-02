package com.iortatechnxt.brokerverse.system.service;

import com.iortatechnxt.brokerverse.system.domain.ProductModuleSwitch;
import com.iortatechnxt.brokerverse.system.domain.ProductModuleSwitchRepository;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cached read of the product module switches ({@link ModuleCaches#SWITCHED_OFF}). */
@Service
public class ModuleSwitchLookup {

  private final ProductModuleSwitchRepository switches;

  /**
   * Creates the lookup.
   *
   * @param switches switch repository
   */
  public ModuleSwitchLookup(ProductModuleSwitchRepository switches) {
    this.switches = switches;
  }

  /**
   * Codes of the modules switched off.
   *
   * @return module codes, sorted
   */
  @Cacheable(cacheNames = ModuleCaches.SWITCHED_OFF, key = "'all'")
  @Transactional(readOnly = true)
  public List<String> switchedOff() {
    return switches.findAllByOrderBySortOrderAsc().stream()
        .filter(s -> !s.isEnabled())
        .map(ProductModuleSwitch::getCode)
        .sorted()
        .toList();
  }
}
