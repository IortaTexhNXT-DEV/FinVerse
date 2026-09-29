package com.iortatechnxt.brokerverse.system.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to switch a module on or off.
 *
 * @param enabled requested state
 * @param reason why the module is switched (stored with the change)
 */
public record ModuleSwitchRequest(boolean enabled, @NotBlank @Size(max = 400) String reason) {}
