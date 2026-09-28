package com.iortatechnxt.brokerverse.system.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.security.UserDisplayNames;
import com.iortatechnxt.brokerverse.system.domain.ModuleProfile;
import com.iortatechnxt.brokerverse.system.domain.ModuleProfileRepository;
import com.iortatechnxt.brokerverse.system.domain.ProductModule;
import com.iortatechnxt.brokerverse.system.domain.ProductModuleSwitch;
import com.iortatechnxt.brokerverse.system.domain.ProductModuleSwitchRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintenance of the product module switches by the system administrator (maker) with the approval
 * of another user (checker): a single switch, or every switch of a module profile. A module that
 * another module needs cannot be switched off while that module is on, and a module cannot be
 * switched on while a module it needs is off.
 */
@Service
@Transactional
public class ProductModuleAdministration {

  private static final String ENTITY = "ProductModule";

  private final ProductModuleSwitchRepository switches;
  private final ModuleProfileRepository profiles;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final UserDisplayNames users;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param switches switches
   * @param profiles module profiles
   * @param audit audit trail
   * @param currentUser current user
   * @param users display names in the audit texts
   * @param clock clock
   */
  public ProductModuleAdministration(
      ProductModuleSwitchRepository switches,
      ModuleProfileRepository profiles,
      AuditTrailService audit,
      CurrentUser currentUser,
      UserDisplayNames users,
      Clock clock) {
    this.switches = switches;
    this.profiles = profiles;
    this.audit = audit;
    this.currentUser = currentUser;
    this.users = users;
    this.clock = clock;
  }

  /**
   * All switches in display order.
   *
   * @return switches
   */
  @Transactional(readOnly = true)
  public List<ProductModuleSwitch> list() {
    return switches.findAllByOrderBySortOrderAsc();
  }

  /**
   * All module profiles.
   *
   * @return profiles by name
   */
  @Transactional(readOnly = true)
  public List<ModuleProfile> profiles() {
    return profiles.findAllByOrderByNameAsc();
  }

  /**
   * Requests switching a module on or off; the change waits for approval.
   *
   * @param code module code
   * @param on requested state
   * @param reason why
   * @return the switch
   */
  public ProductModuleSwitch request(String code, boolean on, String reason) {
    ProductModuleSwitch s = require(code);
    checkDependencies(module(code), on, stateMap());
    s.requestChange(on, reason, currentUser.username(), clock.instant());
    audit.record(
        ENTITY,
        code,
        AuditAction.SUBMIT,
        "Requested to switch "
            + module(code).displayName()
            + (on ? " on" : " off")
            + ", waiting for approval. Reason: "
            + s.getPendingReason());
    return s;
  }

  /**
   * Requests every change needed to reach a module profile, with one reason.
   *
   * @param profileCode profile code
   * @param reason why the profile is applied
   * @return the switches with a change requested
   */
  public List<ProductModuleSwitch> applyProfile(String profileCode, String reason) {
    ModuleProfile profile =
        profiles
            .findByCode(profileCode)
            .orElseThrow(() -> new ResourceNotFoundException("Module profile", profileCode));
    Map<String, Boolean> target = stateMap();
    target.replaceAll((code, on) -> profile.hasOn(code));
    List<ProductModuleSwitch> changed = new ArrayList<>();
    for (ProductModuleSwitch s : list()) {
      boolean wanted = profile.hasOn(s.getCode());
      if (wanted != s.isEnabled() && s.getPendingEnabled() == null) {
        checkDependencies(module(s.getCode()), wanted, target);
        s.requestChange(
            wanted, profile.getName() + ": " + reason, currentUser.username(), clock.instant());
        changed.add(s);
      }
    }
    if (changed.isEmpty()) {
      throw new BusinessRuleException(
          "PROFILE_ALREADY_APPLIED", "Every module is already switched as the profile sets it");
    }
    audit.record(
        ENTITY,
        profileCode,
        AuditAction.SUBMIT,
        "Requested the module profile "
            + profile.getName()
            + " ("
            + changed.size()
            + " module(s)), waiting for approval");
    return changed;
  }

  /**
   * Approves the change of a module (not by its requester) and applies it.
   *
   * @param code module code
   * @return the switch
   */
  public ProductModuleSwitch approve(String code) {
    ProductModuleSwitch s = require(code);
    if (s.getPendingEnabled() != null) {
      checkDependencies(module(code), s.getPendingEnabled(), stateMap());
    }
    String requestedBy = s.getPendingBy();
    boolean on = s.approveChange(currentUser.username(), clock.instant());
    switches.saveAndFlush(s);
    audit.record(
        ENTITY,
        code,
        AuditAction.AUTHORIZE,
        "Switched "
            + module(code).displayName()
            + (on ? " on" : " off")
            + " as requested by "
            + users.displayName(requestedBy));
    return s;
  }

  /**
   * Rejects the change of a module with a reason; the requester may withdraw it without one.
   *
   * @param code module code
   * @param reason why
   * @return the switch
   */
  public ProductModuleSwitch reject(String code, String reason) {
    ProductModuleSwitch s = require(code);
    String requestedBy = s.getPendingBy();
    boolean withdrawn = currentUser.username().equalsIgnoreCase(requestedBy);
    if (!withdrawn && (reason == null || reason.isBlank())) {
      throw new BusinessRuleException("REASON_REQUIRED", "Give the reason for the rejection");
    }
    s.rejectChange();
    audit.record(
        ENTITY,
        code,
        AuditAction.REJECT,
        (withdrawn ? "Withdrew" : "Rejected")
            + " the change of "
            + module(code).displayName()
            + " requested by "
            + users.displayName(requestedBy)
            + (reason == null || reason.isBlank() ? "" : ". Reason: " + reason.strip()));
    return s;
  }

  private void checkDependencies(ProductModule module, boolean on, Map<String, Boolean> state) {
    if (on) {
      for (String needed : module.dependsOn()) {
        if (!state.getOrDefault(needed, Boolean.TRUE)) {
          throw new BusinessRuleException(
              "MODULE_NEEDS_MODULE",
              module.displayName()
                  + " needs "
                  + module(needed).displayName()
                  + "; switch that module on first");
        }
      }
      return;
    }
    for (ProductModule other : ProductModule.values()) {
      if (other.dependsOn().contains(module.name())
          && state.getOrDefault(other.name(), Boolean.TRUE)) {
        throw new BusinessRuleException(
            "MODULE_NEEDED",
            other.displayName()
                + " needs "
                + module.displayName()
                + "; switch "
                + other.displayName()
                + " off first");
      }
    }
  }

  private Map<String, Boolean> stateMap() {
    return list().stream()
        .collect(
            Collectors.toMap(
                ProductModuleSwitch::getCode,
                ProductModuleSwitch::isEnabled,
                (a, b) -> a,
                HashMap::new));
  }

  private ProductModuleSwitch require(String code) {
    return switches
        .findByCode(code)
        .orElseThrow(() -> new ResourceNotFoundException("Product module", code));
  }

  private static ProductModule module(String code) {
    return ProductModule.byCode(code)
        .orElseThrow(() -> new ResourceNotFoundException("Product module", code));
  }

  /**
   * Display names of the modules by code (for the screens).
   *
   * @return name per code
   */
  public static Map<String, String> names() {
    return Arrays.stream(ProductModule.values())
        .collect(Collectors.toMap(Enum::name, ProductModule::displayName, (a, b) -> a));
  }
}
