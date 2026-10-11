package com.iortatechnxt.brokerverse.system.api.dto;

import com.iortatechnxt.brokerverse.system.domain.ProductModule;
import com.iortatechnxt.brokerverse.system.domain.ProductModuleSwitch;
import java.time.Instant;
import java.util.List;

/**
 * A product module switch on the Product Modules screen.
 *
 * @param code module code
 * @param name module name
 * @param enabled whether the module is on
 * @param needs names of the modules this one needs
 * @param pendingEnabled requested state waiting for approval (null when none)
 * @param pendingReason reason of the request
 * @param pendingBy requester (user name)
 * @param pendingAt request time
 * @param changedReason reason of the last change
 * @param changedBy requester of the last change
 * @param changedAt time of the last change
 * @param approvedBy approver of the last change
 */
public record ModuleSwitchResponse(
    String code,
    String name,
    boolean enabled,
    List<String> needs,
    Boolean pendingEnabled,
    String pendingReason,
    String pendingBy,
    Instant pendingAt,
    String changedReason,
    String changedBy,
    Instant changedAt,
    String approvedBy) {

  /** Defensive copy. */
  public ModuleSwitchResponse {
    needs = List.copyOf(needs);
  }

  /**
   * Maps a switch.
   *
   * @param s switch
   * @return response
   */
  public static ModuleSwitchResponse from(ProductModuleSwitch s) {
    ProductModule module = ProductModule.byCode(s.getCode()).orElse(null);
    return new ModuleSwitchResponse(
        s.getCode(),
        module == null ? s.getCode() : module.displayName(),
        s.isEnabled(),
        module == null
            ? List.of()
            : module.dependsOn().stream()
                .map(c -> ProductModule.byCode(c).map(ProductModule::displayName).orElse(c))
                .toList(),
        s.getPendingEnabled(),
        s.getPendingReason(),
        s.getPendingBy(),
        s.getPendingAt(),
        s.getChangedReason(),
        s.getChangedBy(),
        s.getChangedAt(),
        s.getApprovedBy());
  }
}
