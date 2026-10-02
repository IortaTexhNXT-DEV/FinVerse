package com.iortatechnxt.brokerverse.system.api;

import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import com.iortatechnxt.brokerverse.system.api.dto.ModuleDecisionRequest;
import com.iortatechnxt.brokerverse.system.api.dto.ModuleProfileResponse;
import com.iortatechnxt.brokerverse.system.api.dto.ModuleSwitchRequest;
import com.iortatechnxt.brokerverse.system.api.dto.ModuleSwitchResponse;
import com.iortatechnxt.brokerverse.system.service.ProductModuleAdministration;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Product module switches of the deployment (Administration > Product Modules): the system
 * administrator requests a switch or a module profile, another user approves it.
 */
@RestController
@RequestMapping("/api/v1/admin/modules")
public class ProductModuleController {

  private static final String VIEW =
      "hasAnyAuthority('MODULE_SWITCH_MANAGE','MODULE_SWITCH_APPROVE','SYSTEM_MONITOR')";
  private static final String MANAGE = "hasAuthority('MODULE_SWITCH_MANAGE')";
  private static final String DECIDE =
      "hasAnyAuthority('MODULE_SWITCH_APPROVE','MODULE_SWITCH_MANAGE')";

  private final ProductModuleAdministration modules;

  /**
   * Creates the controller.
   *
   * @param modules module administration
   */
  public ProductModuleController(ProductModuleAdministration modules) {
    this.modules = modules;
  }

  /**
   * Lists the module switches.
   *
   * @return switches in display order
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public List<ModuleSwitchResponse> list() {
    return modules.list().stream().map(ModuleSwitchResponse::from).toList();
  }

  /**
   * Lists the module profiles.
   *
   * @return profiles
   */
  @GetMapping("/profiles")
  @PreAuthorize(VIEW)
  public List<ModuleProfileResponse> profiles() {
    return modules.profiles().stream().map(ModuleProfileResponse::from).toList();
  }

  /**
   * Requests switching a module on or off.
   *
   * @param code module code
   * @param request requested state and reason
   * @return the switch
   */
  @PostMapping("/{code}/change")
  @PreAuthorize(MANAGE)
  public ModuleSwitchResponse request(
      @PathVariable String code, @Valid @RequestBody ModuleSwitchRequest request) {
    return ModuleSwitchResponse.from(modules.request(code, request.enabled(), request.reason()));
  }

  /**
   * Requests the changes of a module profile.
   *
   * @param code profile code
   * @param request reason
   * @return the switches with a change requested
   */
  @PostMapping("/profiles/{code}/apply")
  @PreAuthorize(MANAGE)
  public List<ModuleSwitchResponse> applyProfile(
      @PathVariable String code, @Valid @RequestBody ReasonRequest request) {
    return modules.applyProfile(code, request.reason()).stream()
        .map(ModuleSwitchResponse::from)
        .toList();
  }

  /**
   * Approves the change of a module.
   *
   * @param code module code
   * @return the switch
   */
  @PostMapping("/{code}/approve")
  @PreAuthorize("hasAuthority('MODULE_SWITCH_APPROVE')")
  public ModuleSwitchResponse approve(@PathVariable String code) {
    return ModuleSwitchResponse.from(modules.approve(code));
  }

  /**
   * Rejects (or, by the requester, withdraws) the change of a module.
   *
   * @param code module code
   * @param request reason
   * @return the switch
   */
  @PostMapping("/{code}/reject")
  @PreAuthorize(DECIDE)
  public ModuleSwitchResponse reject(
      @PathVariable String code, @Valid @RequestBody ModuleDecisionRequest request) {
    return ModuleSwitchResponse.from(modules.reject(code, request.reason()));
  }
}
