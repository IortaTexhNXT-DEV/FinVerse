package com.iortatechnxt.brokerverse.system.api.dto;

import java.util.List;

/**
 * The product modules switched off in this deployment, for the menus of the web client.
 *
 * @param switchedOff codes of the modules switched off
 * @param inactivePermissions permissions that grant nothing while those modules are off
 */
public record ModulesInUseResponse(List<String> switchedOff, List<String> inactivePermissions) {

  /** Defensive copies. */
  public ModulesInUseResponse {
    switchedOff = List.copyOf(switchedOff);
    inactivePermissions = List.copyOf(inactivePermissions);
  }
}
