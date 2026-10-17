package com.iortatechnxt.brokerverse.system.api;

import com.iortatechnxt.brokerverse.system.api.dto.ModulesInUseResponse;
import com.iortatechnxt.brokerverse.system.service.ProductModules;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The product modules in use, read by the web client of every signed-in user to build menus. */
@RestController
@RequestMapping("/api/v1/system/modules")
public class ModulesInUseController {

  private final ProductModules modules;

  /**
   * Creates the controller.
   *
   * @param modules module switches
   */
  public ModulesInUseController(ProductModules modules) {
    this.modules = modules;
  }

  /**
   * The modules switched off and the permissions that grant nothing because of them.
   *
   * @return modules in use
   */
  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ModulesInUseResponse inUse() {
    return new ModulesInUseResponse(modules.switchedOff(), modules.inactivePermissions());
  }
}
