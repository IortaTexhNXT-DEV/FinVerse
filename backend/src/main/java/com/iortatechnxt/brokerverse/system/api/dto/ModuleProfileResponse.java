package com.iortatechnxt.brokerverse.system.api.dto;

import com.iortatechnxt.brokerverse.system.domain.ModuleProfile;
import com.iortatechnxt.brokerverse.system.domain.ProductModule;
import java.util.Arrays;
import java.util.List;

/**
 * A module profile.
 *
 * @param code profile code
 * @param name profile name
 * @param description what the profile is for
 * @param modulesOff names of the modules the profile switches off
 */
public record ModuleProfileResponse(
    String code, String name, String description, List<String> modulesOff) {

  /** Defensive copy. */
  public ModuleProfileResponse {
    modulesOff = List.copyOf(modulesOff);
  }

  /**
   * Maps a profile.
   *
   * @param p profile
   * @return response
   */
  public static ModuleProfileResponse from(ModuleProfile p) {
    return new ModuleProfileResponse(
        p.getCode(),
        p.getName(),
        p.getDescription(),
        Arrays.stream(ProductModule.values())
            .filter(m -> !p.hasOn(m.name()))
            .map(ProductModule::displayName)
            .toList());
  }
}
