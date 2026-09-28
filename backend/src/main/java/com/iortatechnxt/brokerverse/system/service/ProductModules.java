package com.iortatechnxt.brokerverse.system.service;

import com.iortatechnxt.brokerverse.common.exception.ModuleNotInUseException;
import com.iortatechnxt.brokerverse.system.domain.ProductModule;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Service;

/**
 * Whether the product modules, and the controllers, reports, jobs and permissions that belong to
 * them, are in use in this deployment. Menus, APIs, permissions, the Report Centre, dashboard
 * widgets and scheduled jobs all follow these switches.
 */
@Service
public class ProductModules {

  private final ModuleSwitchLookup lookup;
  private final ModulePermissionIndex permissionIndex;

  /**
   * Creates the service.
   *
   * @param lookup cached switches
   * @param permissionIndex permission usage per module
   */
  public ProductModules(ModuleSwitchLookup lookup, ModulePermissionIndex permissionIndex) {
    this.lookup = lookup;
    this.permissionIndex = permissionIndex;
  }

  /**
   * Codes of the modules switched off.
   *
   * @return module codes
   */
  public List<String> switchedOff() {
    return lookup.switchedOff();
  }

  /**
   * Whether a module is on.
   *
   * @param module module
   * @return true when switched on
   */
  public boolean isOn(ProductModule module) {
    return !lookup.switchedOff().contains(module.name());
  }

  /**
   * Whether the module of a class is on; platform classes are always on.
   *
   * @param type controller, report or job class
   * @return true unless the class belongs to a switched-off module
   */
  public boolean isClassOn(Class<?> type) {
    return ProductModule.ofClass(type).map(this::isOn).orElse(true);
  }

  /**
   * Refuses the use of a class of a switched-off module.
   *
   * @param type controller, report or job class
   * @throws ModuleNotInUseException when its module is switched off
   */
  public void requireClassOn(Class<?> type) {
    Optional<ProductModule> module = ProductModule.ofClass(type);
    if (module.isPresent() && !isOn(module.get())) {
      throw new ModuleNotInUseException(module.get().displayName());
    }
  }

  /**
   * Whether a permission grants anything: it is not owned by a switched-off module, and not used
   * only by the controllers of switched-off modules.
   *
   * @param permission permission code
   * @return false for a permission of switched-off modules only
   */
  public boolean isPermissionActive(String permission) {
    List<String> off = lookup.switchedOff();
    if (off.isEmpty()) {
      return true;
    }
    Optional<ProductModule> owner = ProductModule.owningPermission(permission);
    if (owner.isPresent()) {
      return !off.contains(owner.get().name());
    }
    Set<String> using = permissionIndex.modulesUsing(permission);
    return using.isEmpty() || !off.containsAll(using);
  }

  /**
   * The permissions that grant nothing while the current modules are switched off.
   *
   * @return permission codes, sorted
   */
  public List<String> inactivePermissions() {
    if (lookup.switchedOff().isEmpty()) {
      return List.of();
    }
    Set<String> codes = new TreeSet<>(permissionIndex.permissions());
    for (ProductModule m : ProductModule.values()) {
      codes.addAll(m.ownPermissions());
    }
    return codes.stream().filter(p -> !isPermissionActive(p)).toList();
  }

  /**
   * The active permissions of a set of granted permissions.
   *
   * @param granted permission codes
   * @return the codes that grant something
   */
  public List<String> activePermissions(Collection<String> granted) {
    return granted.stream().filter(this::isPermissionActive).toList();
  }
}
